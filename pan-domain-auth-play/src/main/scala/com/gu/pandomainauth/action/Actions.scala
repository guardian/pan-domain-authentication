package com.gu.pandomainauth.action

import com.gu.pandomainauth.PanDomainAuth
import com.gu.pandomainauth.model.{AuthenticatedUser, User}
import com.gu.pandomainauth.service.{Google2FAGroupChecker, GoogleAuthException, GoogleAuth, CookieUtils}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Application, Logger}
import play.api.Play.current
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global


class UserRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

trait AuthActions extends PanDomainAuth {

  /**
   * Returns true if the authed user is valid in the implementing system (meets your multifactor requirements, you recognise the email etc.).
   * 
   * If your implementing application needs to audit logins / register new users etc then this ia also the place to do it.
   * 
   * @param authedUser
   * @return true if the user is valid in your app
   */
  def validateUser(authedUser: AuthenticatedUser): Boolean

  /**
   * The auth callback url. This is where google will send the user after authentication. This action on this url should
   * invoke processGoogleCallback
   * 
   * @return
   */
  def authCallbackUrl: String

  val GoogleAuth = new GoogleAuth(settings.googleAuthSettings, system, authCallbackUrl)

  val multifactorChecker = settings.google2FAGroupSettings.map(new Google2FAGroupChecker(_))
  /**
   * A Play session key that stores the target URL that was being accessed when redirected for authentication
   */
  val LOGIN_ORIGIN_KEY = "loginOriginUrl"
  val ANTI_FORGERY_KEY = "antiForgeryToken"


  /**
   * starts the authentication process for a user. By default this just sends the user off to google for auth
   * but if you want to show welcome page with a button on it then override.
   */
  def sendForAuth[A](implicit request: RequestHeader, email: Option[String] = None) = {
    val antiForgeryToken = GoogleAuth.generateAntiForgeryToken()
    GoogleAuth.redirectToGoogle(antiForgeryToken, email).map { res =>
      val originUrl = request.uri
      res.withSession { request.session + (ANTI_FORGERY_KEY -> antiForgeryToken) + (LOGIN_ORIGIN_KEY -> originUrl) }
    }
  }

  def checkMultifactor(authedUser: AuthenticatedUser) = multifactorChecker.map(_.checkMultifactor(authedUser)).getOrElse(false)

  /**
   * invoked when the user is not logged in a can't be authed - this may be when the user is not valid in yur system
   * or when they have exoplicitly logged out.
   *
   * Override this to add a logged out screen and display maeesages for your app. The default implementation is
   * to ust return a 403 response
   *
   * @param message
   * @param request
   * @return
   */
  def showUnauthedMessage(message: String)(implicit request: RequestHeader): Result = {
    Logger.info(message)
    Forbidden
  }

  /**
   * Generates the message shown to the user when user validation fails. override this to add a custom error message
   *
   * @param claimedAuth
   * @return
   */
  def invalidUserMessage(claimedAuth: AuthenticatedUser) = s"user ${claimedAuth.user.email} not valid for $system"


  def processGoogleCallback()(implicit request: RequestHeader) = {
    val token = request.session.get(ANTI_FORGERY_KEY).getOrElse( throw new GoogleAuthException("missing anti forgery token"))
    val originalUrl = request.session.get(LOGIN_ORIGIN_KEY).getOrElse( throw new GoogleAuthException("missing original url"))

    val existingCookie = request.cookies.get(settings.cookieName) // will e populated if this was a re-auth for expired login

    GoogleAuth.validatedUserIdentity(token).map { claimedAuth =>
      val authedUserData = existingCookie match {
        case Some(c) => {
          val existingAuth = CookieUtils.parseCookieData(c.value, settings.secret)
          Logger.debug("user re-authed, merging auth data")

          claimedAuth.copy(
            authenticatingSystem = system,
            authenticatedIn = existingAuth.authenticatedIn + system,
            multiFactor = existingAuth.multiFactor
          )
        }
        case None => {
          Logger.debug("fresh user login")
          claimedAuth.copy(multiFactor = checkMultifactor(claimedAuth))
        }
      }

      if (validateUser(authedUserData)) {
        val updatedCookie = generateCookie(authedUserData)
        Redirect(originalUrl).withCookies(updatedCookie).withSession(session = request.session - ANTI_FORGERY_KEY - LOGIN_ORIGIN_KEY)
      } else {
        showUnauthedMessage(invalidUserMessage(claimedAuth))
      }
    }
  }

  def processLogout(implicit request: RequestHeader) = {
    flushCookie(showUnauthedMessage("logged out"))
  }


  def readAuthenticatedUser(request: RequestHeader): Option[AuthenticatedUser] = readCookie(request) map { cookie =>
    CookieUtils.parseCookieData(cookie.value, settings.secret)
  }


  def readCookie(request: RequestHeader): Option[Cookie] = request.cookies.get(settings.cookieName)

  def generateCookie(authedUser: AuthenticatedUser): Cookie = Cookie(
    name     = settings.cookieName,
    value    = CookieUtils.generateCookieData(authedUser, settings.secret),
    domain   = Some(domain),
    secure   = true,
    httpOnly = true
  )

  def includeSystemInCookie(authedUser: AuthenticatedUser)(result: Result): Result = {
    val updatedAuth = authedUser.copy(authenticatedIn = authedUser.authenticatedIn + system)
    val updatedCookie = generateCookie(updatedAuth)
    result.withCookies(updatedCookie)
  }

  def flushCookie(result: Result): Result = {
    val clearCookie = DiscardingCookie(
      name = settings.cookieName,
      domain = Some(domain),
      secure = true
    )
    result.discardingCookies(clearCookie)
  }


  // Represents the status of the attempted authentication
  sealed trait AuthenticationStatus
  case class Expired(authedUser: AuthenticatedUser) extends AuthenticationStatus
  case class Authenticated(authedUser: AuthenticatedUser) extends AuthenticationStatus
  case class NotAuthorized(authedUser: AuthenticatedUser) extends AuthenticationStatus
  case class InvalidCookie(exception: Exception) extends AuthenticationStatus
  case object NotAuthenticated extends AuthenticationStatus

  /**
   * Extract the authentication status from the request.
   */
  def extractAuth(request: RequestHeader): AuthenticationStatus = try {
    readAuthenticatedUser(request) map { authedUser =>
      if (authedUser.isExpired) {
        Expired(authedUser)
      } else if (validateUser(authedUser)) {
        Authenticated(authedUser)
      } else {
        NotAuthorized(authedUser)
      }
    } getOrElse {
      NotAuthenticated
    }
  } catch {
    case e: Exception => InvalidCookie(e)
  }


  /**
   * Action that ensures the user is logged in and validated.
   *
   * This action is for page load type requests where it is possible to send the user for auth
   * and for them to interact with the auth provider. For API / XHR type requests use the APIAuthAction
   *
   * if the user is not authed or the auth has expired they are sent for authentication
   */
  object AuthAction extends ActionBuilder[UserRequest] {

    override def invokeBlock[A](request: Request[A], block: (UserRequest[A]) => Future[Result]): Future[Result] = {
      extractAuth(request) match {
        case NotAuthenticated =>
          Logger.debug(s"user not authed against $domain, authing")
          sendForAuth(request)

        case InvalidCookie(e) =>
          Logger.warn("error checking user's auth, clear cookie and re-auth", e)
          // remove the invalid cookie data
          sendForAuth(request).map(flushCookie)

        case Expired(authedUser) =>
          Logger.debug(s"user ${authedUser.user.email} login expired, sending to re-auth")
          sendForAuth(request, Some(authedUser.user.email))

        case NotAuthorized(authedUser) =>
          Logger.debug(s"user not authorized, show error")
          Future(showUnauthedMessage(invalidUserMessage(authedUser))(request))

        case Authenticated(authedUser) =>
          val response = block(new UserRequest(authedUser.user, request))
          if (authedUser.authenticatedIn(system)) {
            response
          } else {
            Logger.debug(s"user ${authedUser.user.email} from other system valid: adding validity in $system.")
            response.map(includeSystemInCookie(authedUser))
          }
      }
    }
  }

  /**
   * Action that ensures the user is logged in and validated.
   *
   * This action is for API / XHR type requests where the user can't be sent to the auth provider for auth. In the
   * cases where the auth is not valid repsonce codes are sent to the requesting app and the javascript that initiated
   * the request should handle these appropriately
   *
   * If the user is not authed then a 401 response is sent, if the auth has expired then a 419 response is sent, if
   * the user is authed but not allowed to perform the action a 403 is sent
   */
  object APIAuthAction extends ActionBuilder[UserRequest] {

    override def invokeBlock[A](request: Request[A], block: (UserRequest[A]) => Future[Result]): Future[Result] = {
      extractAuth(request) match {
        case NotAuthenticated =>
          Logger.debug(s"user not authed against $domain, return 401")
          Future(Unauthorized)

        case InvalidCookie(e) =>
          Logger.warn("error checking user's auth, clear cookie and return 401", e)
          // remove the invalid cookie data
          Future(Unauthorized).map(flushCookie)

        case Expired(authedUser) =>
          Logger.debug(s"user ${authedUser.user.email} login expired, return 419")
          Future(new Status(419))

        case NotAuthorized(authedUser) =>
          Logger.debug(s"user not authorized, return 403")
          Logger.debug(invalidUserMessage(authedUser))
          Future(Forbidden)

        case Authenticated(authedUser) =>
          val response = block(new UserRequest(authedUser.user, request))
          if (authedUser.authenticatedIn(system)) {
            response
          } else {
            Logger.debug(s"user ${authedUser.user.email} from other system valid: adding validity in $system.")
            response.map(includeSystemInCookie(authedUser))
          }
      }
    }
  }
}
