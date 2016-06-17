package com.gu.pandomainauth.action

import com.gu.pandomainauth.{PanDomain, PanDomainAuth, PublicSettings}
import com.gu.pandomainauth.model._
import com.gu.pandomainauth.service._
import play.api.Logger
import play.api.Play.current
import play.api.mvc.Results._
import play.api.mvc._
import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class UserRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

trait AuthActions extends PanDomainAuth {

  /**
   * Returns true if the authed user is valid in the implementing system (meets your multifactor requirements, you recognise the email etc.).
   *
   * If your implementing application needs to audit logins / register new users etc then this ia also the place to do it (although in this case
   * you should strongly consider setting cacheValidation to true).
   *
   * @param authedUser
   * @return true if the user is valid in your app
   */
  def validateUser(authedUser: AuthenticatedUser): Boolean


  /**
   * By default the validity of the user is checked every request. If your validateUser implementation is expensive or has side effects you
   * can override this to true and validity will only be checked the first time the user visits your app after their login is established.
   *
   * Note the the cache is invalidated after the user's session is re-established with google.
   *
   * @return true if you want to only check the validity of the user once for the lifetime of the user's auth session
   */
  def cacheValidation: Boolean = false


  /**
   * Adding an expiry extension to `APIAuthAction`s allows for a delay between an applications authentication and their
   * respective API XHR calls expiring.
   *
   * By default this is 0 and thus disabled.
   *
   * This is particularly useful for SPAs where users have third party cookies disabled.
   *
   * @return the amount of delay between App and API expiry in milliseconds
   */
  def apiGracePeriod: Long = 0 // ms


  /**
   * The auth callback url. This is where google will send the user after authentication. This action on this url should
   * invoke processGoogleCallback
   *
   * @return
   */
  def authCallbackUrl: String

  val GoogleAuth = new GoogleAuth(settings.googleAuthSettings, system, authCallbackUrl)

  val multifactorChecker = settings.google2FAGroupSettings.map(new Google2FAGroupChecker(_, bucket))
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

    val existingCookie = readCookie(request) // will be populated if this was a re-auth for expired login

    // use both parsers until we complete the transition to only set the assymetric cookie.
    GoogleAuth.validatedUserIdentity(token).map { claimedAuth =>
      val authedUserData = existingCookie match {
        case Some(c) => {
          val existingAuth = CookieUtils.parseCookieData(c.value, settings.publicKey)
          Logger.debug("user re-authed, merging auth data")

          claimedAuth.copy(
            authenticatingSystem = system,
            authenticatedIn = existingAuth.authenticatedIn ++ Set(system),
            multiFactor = checkMultifactor(claimedAuth)
          )
        }
        case None => {
          Logger.debug("fresh user login")
          claimedAuth.copy(multiFactor = checkMultifactor(claimedAuth))
        }
      }

      if (validateUser(authedUserData)) {
        val updatedCookies = generateCookies(authedUserData)
        Redirect(originalUrl).withCookies(updatedCookies:_*).withSession(session = request.session - ANTI_FORGERY_KEY - LOGIN_ORIGIN_KEY)
      } else {
        showUnauthedMessage(invalidUserMessage(claimedAuth))
      }
    }
  }

  def processLogout(implicit request: RequestHeader) = {
    flushCookie(showUnauthedMessage("logged out"))
  }


  def readAuthenticatedUser(request: RequestHeader): Option[AuthenticatedUser] = readCookie(request) map { cookie =>
    CookieUtils.parseCookieData(cookie.value, settings.publicKey)
  }


  def readCookie(request: RequestHeader): Option[Cookie] = request.cookies.get(PublicSettings.assymCookieName)

  def generateCookies(authedUser: AuthenticatedUser): List[Cookie] = List(
    Cookie(
      name     = PublicSettings.cookieName,
      value    = LegacyCookie.generateCookieData(authedUser, settings.secret),
      domain   = Some(domain),
      secure   = true,
      httpOnly = true
    ),
    Cookie(
      name     = PublicSettings.assymCookieName,
      value    = CookieUtils.generateCookieData(authedUser, settings.privateKey),
      domain   = Some(domain),
      secure   = true,
      httpOnly = true
    )
  )

  def includeSystemInCookie(authedUser: AuthenticatedUser)(result: Result): Result = {
    val updatedAuth = authedUser.copy(authenticatedIn = authedUser.authenticatedIn + system)
    val updatedCookies = generateCookies(updatedAuth)
    result.withCookies(updatedCookies:_*)
  }

  def flushCookie(result: Result): Result = {
    val clearCookie = DiscardingCookie(
      name = PublicSettings.cookieName,
      domain = Some(domain),
      secure = true
    )
    val clearAssymCookie = DiscardingCookie(
      name = PublicSettings.assymCookieName,
      domain = Some(domain),
      secure = true
    )
    result.discardingCookies(clearCookie, clearAssymCookie)
  }


  /**
   * Extract the authentication status from the request.
   */
  def extractAuth(request: RequestHeader): AuthenticationStatus = {
    readCookie(request).map { cookie =>
      PanDomain.authStatus(cookie.value, settings.publicKey) match {
        case Expired(authedUser) if authedUser.isInGracePeriod(apiGracePeriod) =>
          GracePeriod(authedUser)
        case authStatus @ Authenticated(authedUser) =>
          if (cacheValidation && authedUser.authenticatedIn(system)) authStatus
          else if (validateUser(authedUser)) authStatus
          else NotAuthorized(authedUser)
        case authStatus => authStatus
      }
    } getOrElse NotAuthenticated
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

        case GracePeriod(authedUser) =>
          Logger.debug(s"user ${authedUser.user.email} login expired, in grace period, sending to re-auth")
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
   * cases where the auth is not valid response codes are sent to the requesting app and the javascript that initiated
   * the request should handle these appropriately
   *
   * If the user is not authed then a 401 response is sent, if the auth has expired then a 419 response is sent, if
   * the user is authed but not allowed to perform the action a 403 is sent
   *
   * If the user is authed or has an expiry extension, a 200 is sent
   *
   */
  object APIAuthAction extends AbstractApiAuthAction with PlainErrorResponses

  trait PlainErrorResponses {
    val notAuthenticatedResult = Unauthorized
    val invalidCookieResult    = Unauthorized
    val expiredResult          = new Status(419)
    val notAuthorizedResult    = Forbidden
  }

  /**
   * Abstraction for API auth actions allowing to mix in custom results for each of the different error scenarios.
   */
  trait AbstractApiAuthAction extends ActionBuilder[UserRequest] {

    val notAuthenticatedResult: Result
    val invalidCookieResult: Result
    val expiredResult: Result
    val notAuthorizedResult: Result

    override def invokeBlock[A](request: Request[A], block: (UserRequest[A]) => Future[Result]): Future[Result] = {
      extractAuth(request) match {
        case NotAuthenticated =>
          Logger.debug(s"user not authed against $domain, return 401")
          Future(notAuthenticatedResult)

        case InvalidCookie(e) =>
          Logger.warn("error checking user's auth, clear cookie and return 401", e)
          // remove the invalid cookie data
          Future(invalidCookieResult).map(flushCookie)

        case Expired(authedUser) =>
          Logger.debug(s"user ${authedUser.user.email} login expired, return 419")
          Future(expiredResult)

        case GracePeriod(authedUser) =>
          Logger.debug(s"user ${authedUser.user.email} login expired but is in grace period.")
          val response = block(new UserRequest(authedUser.user, request))
          responseWithSystemCookie(response, authedUser)

        case NotAuthorized(authedUser) =>
          Logger.debug(s"user not authorized, return 403")
          Logger.debug(invalidUserMessage(authedUser))
          Future(notAuthorizedResult)

        case Authenticated(authedUser) =>
          val response = block(new UserRequest(authedUser.user, request))
          responseWithSystemCookie(response, authedUser)
      }
    }

    def responseWithSystemCookie(response: Future[Result], authedUser: AuthenticatedUser): Future[Result] =
      if (authedUser.authenticatedIn(system)) {
        response
      } else {
        Logger.debug(s"user ${authedUser.user.email} from other system valid: adding validity in $system.")
        response.map(includeSystemInCookie(authedUser))
      }
  }
}
