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

  def validateUser(authedUser: AuthenticatedUser): Boolean
  def redirectUrl: String

  val GoogleAuth = new GoogleAuth(settings.googleAuthSettings, system, redirectUrl)

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
  def sendForAuth[A](implicit request: RequestHeader) = {
    val antiForgeryToken = GoogleAuth.generateAntiForgeryToken()
    GoogleAuth.redirectToGoogle(antiForgeryToken).map { res =>
      val originUrl = request.uri
      res.withSession { request.session + (ANTI_FORGERY_KEY -> antiForgeryToken) + (LOGIN_ORIGIN_KEY -> originUrl) }
    }

  }

  def checkMultifactor(authedUser: AuthenticatedUser) = multifactorChecker.map(_.checkMultifactor(authedUser)).getOrElse(false)

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
        val updatedCookie = Cookie(
          name = settings.cookieName,
          value = CookieUtils.generateCookieData(authedUserData, settings.secret),
          domain = Some(domain),
          secure = true,
          httpOnly = true
        )

        Redirect(originalUrl).withCookies(updatedCookie).withSession(session = request.session - ANTI_FORGERY_KEY - LOGIN_ORIGIN_KEY)
      } else {
        Logger.info(s"user ${claimedAuth.user.email} not authed for $system. 403'ing")
        Forbidden
      }
    }
  }

  /**
   * Action that ensures the user is logged in and validated.
   */
  object AuthAction extends ActionBuilder[UserRequest] {

    override def invokeBlock[A](request: Request[A], block: (UserRequest[A]) => Future[Result]): Future[Result] = {

      val cookie = request.cookies.get(settings.cookieName)

      cookie.map{c =>
        try {
          val authedUser = CookieUtils.parseCookieData(c.value, settings.secret)

          if(authedUser.authenticatedIn(system)) {
            block(new UserRequest(authedUser.user, request))
          } else if(validateUser(authedUser)) {

            val updatedAuth = authedUser.copy(authenticatedIn = authedUser.authenticatedIn + system)
            val updatedCookie = Cookie(
              name = settings.cookieName,
              value = CookieUtils.generateCookieData(updatedAuth, settings.secret),
              domain = Some(domain),
              secure = true,
              httpOnly = true
            )

            Logger.debug(s"user ${authedUser.user.email} from other system valid: adding validity in $system.")
            block(new UserRequest(authedUser.user, request)).map(_.withCookies(updatedCookie))
          } else {
            Logger.info(s"user ${authedUser.user.email} not authed for $system. 403'ing")
            Future(Forbidden)
          }
        } catch {
          case e: Exception => {
            Logger.warn("error checking user's auth, re-authing", e)
            sendForAuth(request).map(_.discardingCookies( // remove the invalid cookie data
              DiscardingCookie(name = settings.cookieName, domain = Some(domain), secure = true))
            )
          }
        }

      }.getOrElse{
        Logger.debug(s"user not authed against $domain, authing")
        sendForAuth(request)
      }
    }
  }
}
