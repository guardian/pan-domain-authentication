package com.gu.pandomainauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.CookieChanges.NameAndDomain
import com.gu.pandomainauth.internal.planning.*
import com.gu.pandomainauth.internal.planning.ApiEndpoint.{CredentialRefreshing, DisallowApiAccess, NoAuthentication}
import com.gu.pandomainauth.model.*
import com.gu.pandomainauth.oauth.OAuthUrl

import java.math.BigInteger
import java.net.URI
import java.security.SecureRandom

case class CookieChanges(
  setSessionCookies: Map[NameAndDomain, String] = Map.empty,
  wipeCookies: Set[NameAndDomain] = Set.empty
)

object CookieChanges {
  val NoChanges = CookieChanges()

  /**
   * @param domain if populated, the domain the cookie should be dropped on (eg gutools.co.uk). If empty, leave the
   *               cookie domain unspecified when creating the cookie - browsers interpret this as meaning that
   *               the cookie domain should be the exact domain of the site storing the cookie (eg composer.gutools.co.uk)
   *               ...this is appropriate for temporary OAuth-callback-related cookies.
   */
  case class NameAndDomain(name: String, domain: Option[String])
}





object PageRequestHandlingStrategy {
  /**
   * A cookie key that stores the target URL that was being accessed when redirected for authentication
   */
  val LOGIN_ORIGIN_KEY: CookieChanges.NameAndDomain = CookieChanges.NameAndDomain("panda-loginOriginUrl", None)
  /*
   * Cookie key containing an anti-forgery token; helps to validate that the oauth callback arrived in response to the correct oauth request
   * Should only the main auth cookie be on the shared domain, while temp OAuth cookies be on the app-specific domain, to avoid clashes?
   */
  val ANTI_FORGERY_KEY: CookieChanges.NameAndDomain = CookieChanges.NameAndDomain("panda-antiForgeryToken", None)
  /*
   * Cookie that will make panda behave as if the cookie has expired.
   * NOTE: This cookie is for debugging only! It should _not_ be set by any application code to expire the cookie!! Use the `processLogout` action instead!!
   */
  val FORCE_EXPIRY_KEY: CookieChanges.NameAndDomain = CookieChanges.NameAndDomain("panda-forceExpiry", None)

  val TemporaryCookiesUsedForOAuth: Set[CookieChanges.NameAndDomain] = Set(LOGIN_ORIGIN_KEY, ANTI_FORGERY_KEY, FORCE_EXPIRY_KEY)
}

class PageRequestHandlingStrategy[F[_]: Monad](oAuthUrl: OAuthUrl)
  extends AuthStatusHandler[PageEndpoint.RespType, PageEndpoint.RespMod] {

  val F: Monad[F] = Monad[F]
  
  private val random = new SecureRandom()

  private def redirectForAuth(loginHintEmail: Option[String] = None, wipeAuthCookie: Boolean = false): Plan[PageEndpoint.RespType, PageEndpoint.RespMod] = {
    val antiForgeryToken: String = new BigInteger(130, random).toString(32)
    Plan(
      Redirect(oAuthUrl.uriOfOAuthProvider(antiForgeryToken, loginHintEmail)),
      Some(PageEndpoint.PrepareForOAuth(antiForgeryToken, wipeAuthCookie))
    )
  }

  override def planForAuthStatus(authPersistenceStatus: AuthPersistenceStatus): Plan[PageEndpoint.RespType, PageEndpoint.RespMod] =
    authPersistenceStatus.effectiveAuthStatus match {
      case NotAuthenticated => redirectForAuth()
      case InvalidCookie(_) => redirectForAuth(wipeAuthCookie = true)
      case stale: StaleUserAuthentication => redirectForAuth(loginHintEmail = Some(stale.authedUser.user.email))
      case com.gu.pandomainauth.model.NotAuthorized(authedUser) => Plan(com.gu.pandomainauth.internal.planning.NotAuthorized(authedUser))
      case Authenticated(authedUser) => Plan(AllowAccess(authedUser.user),
        Option.when(authPersistenceStatus.hasUnpersistedSystemAuthorisations)(PersistAuth(authedUser))
      )
    }
}

//object OAuthCallbackHandlingStrategy extends AuthStatusHandler[OAuthCallbackEndpoint.RespType, OAuthCallbackEndpoint.RespMod] {
//
//  def planForAuthStatus(authPersistenceStatus: AuthPersistenceStatus): Plan[OAuthCallbackEndpoint.RespType, OAuthCallbackEndpoint.RespMod] = 
//    authPersistenceStatus.effectiveAuthStatus match {
//      case NotAuthenticated | InvalidCookie(_) | Expired(_) => Plan(NoAuthentication)
//      case com.gu.pandomainauth.model.NotAuthorized(authedUser) => Plan(internal.NotAuthorized(authedUser))
//      case acceptable: AcceptableAuthForApiRequests =>
//        Plan(AllowAccess(acceptable.authedUser.user), Some(CredentialRefreshing(suggest = acceptable.shouldBeRefreshed)))
//    }
//}

object ApiRequestHandlingStrategy extends AuthStatusHandler[ApiEndpoint.RespType, ApiEndpoint.RespMod] {
  
  def planForAuthStatus(authPersistenceStatus: AuthPersistenceStatus): Plan[ApiEndpoint.RespType, ApiEndpoint.RespMod] = authPersistenceStatus.effectiveAuthStatus match {
    case NotAuthenticated | InvalidCookie(_) | Expired(_) => Plan(NoAuthentication)
    case com.gu.pandomainauth.model.NotAuthorized(_) => Plan(ApiEndpoint.NotAuthorized)
    case acceptable: AcceptableAuthForApiRequests =>
      Plan(AllowAccess(acceptable.authedUser.user), Some(CredentialRefreshing(suggest = acceptable.shouldBeRefreshed)))
  }
}
