package com.gu.pandomainauth

import com.gu.pandomainauth.ResponseModification.CookieChanges
import com.gu.pandomainauth.internal.planning.*
import com.gu.pandomainauth.internal.planning.ApiEndpoint.{CredentialRefreshing, NoAuthentication}
import com.gu.pandomainauth.model.*
import com.gu.pandomainauth.oauth.OAuthUrl

import java.math.BigInteger
import java.security.SecureRandom


object PageEndpointAuthStatusHandler {
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

class PageEndpointAuthStatusHandler(oAuthUrl: OAuthUrl)
  extends AuthStatusHandler[PageEndpoint.RespType, PageEndpoint.RespMod] {
  
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

object ApiEndpointAuthStatusHandler extends AuthStatusHandler[ApiEndpoint.RespType, ApiEndpoint.RespMod] {
  
  def planForAuthStatus(authPersistenceStatus: AuthPersistenceStatus): Plan[ApiEndpoint.RespType, ApiEndpoint.RespMod] = authPersistenceStatus.effectiveAuthStatus match {
    case NotAuthenticated | InvalidCookie(_) | Expired(_) => Plan(NoAuthentication)
    case com.gu.pandomainauth.model.NotAuthorized(_) => Plan(ApiEndpoint.NotAuthorized)
    case acceptable: AcceptableAuthForApiRequests =>
      Plan(AllowAccess(acceptable.authedUser.user), Some(CredentialRefreshing(suggest = acceptable.shouldBeRefreshed)))
  }
}
