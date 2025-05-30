package com.gu.pandomainauth

import com.gu.pandomainauth.PageRequestHandlingStrategy.{ANTI_FORGERY_KEY, TemporaryCookiesUsedForOAuth}
import com.gu.pandomainauth.ResponseModification.NoResponseModification
import com.gu.pandomainauth.model.{AuthenticatedUser, CookieSettings}
import com.gu.pandomainauth.service.CookieUtils.generateCookieData
import com.gu.pandomainauth.service.CryptoConf.Signing

class CookieResponses(
  val cookieSettings: CookieSettings,
  signing: () => Signing,
  system: String,
  domain: String
) {
  def updateCookieToAddSystemIfNecessary(authedUser: AuthenticatedUser): ResponseModification =
    authedUser.requiringAdditional(system).fold(NoResponseModification) { updatedUser => cookieResponseFor(updatedUser) }

  def cookieResponseFor(user: AuthenticatedUser, wipeTemporaryCookiesUsedForOAuth: Boolean = false): ResponseModification =
    ResponseModification(cookieChanges = Some(CookieChanges(
      domain,
      setSessionCookies = Map(cookieSettings.cookieName -> generateCookieData(user, signing())),
      wipeCookies = if (wipeTemporaryCookiesUsedForOAuth) TemporaryCookiesUsedForOAuth else Set.empty
    )))

  def responseForRedirectForAuth(antiForgeryToken: String, wipeAuthCookie: Boolean = false): ResponseModification = {
    ResponseModification(cookieChanges = Some(CookieChanges(
        domain, // ?? Should only the main auth cookie be on the shared domain, while temp OAuth cookies be on the app-specific domain, to avoid clashes?
        setSessionCookies = Map(ANTI_FORGERY_KEY -> antiForgeryToken),
        wipeCookies = if (wipeAuthCookie) Set(cookieSettings.cookieName) else Set.empty
      )))
  }

  val processLogout: ResponseModification = ResponseModification(
    cookieChanges = Some(CookieChanges(domain, wipeCookies = Set(cookieSettings.cookieName)))
  )
}

object CookieResponses {
  def apply(settingsRefresher: PanDomainAuthSettingsRefresher): CookieResponses = new CookieResponses(
    settingsRefresher.settings.cookieSettings,
    () => settingsRefresher.settings.signingAndVerification,
    system = settingsRefresher.system, domain = settingsRefresher.domain
  )
}