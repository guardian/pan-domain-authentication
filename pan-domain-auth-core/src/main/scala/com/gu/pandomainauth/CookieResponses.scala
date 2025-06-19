package com.gu.pandomainauth

import com.gu.pandomainauth.CookieAction.{Logout, PersistAuth, PrepareForOAuth}
import com.gu.pandomainauth.CookieChanges.NameAndDomain
import com.gu.pandomainauth.PageRequestHandlingStrategy.{ANTI_FORGERY_KEY, TemporaryCookiesUsedForOAuth}
import com.gu.pandomainauth.ResponseModification.NoResponseModification
import com.gu.pandomainauth.model.{AuthenticatedUser, CookieSettings}
import com.gu.pandomainauth.service.CookieUtils.generateCookieData
import com.gu.pandomainauth.service.CryptoConf.Signing

class CookieResponses(
  val cookieSettings: CookieSettings,
  signing: () => Signing,
  val system: String,
  domain: String
) {
  
  val authCookie: CookieChanges.NameAndDomain = CookieChanges.NameAndDomain(cookieSettings.cookieName, Some(domain))
  
  def handle(cookieAction: CookieAction): CookieChanges = cookieAction match {
    case Logout => CookieChanges(wipeCookies = Set(authCookie))
    case PersistAuth(authedUser, wipeTemporaryCookiesUsedForOAuth) => CookieChanges(
      setSessionCookies = Map(authCookie -> generateCookieData(authedUser, signing())),
      wipeCookies = if (wipeTemporaryCookiesUsedForOAuth) TemporaryCookiesUsedForOAuth else Set.empty
    )
    case PrepareForOAuth(antiForgeryToken, wipeAuthCookie) => CookieChanges(
      setSessionCookies = Map(ANTI_FORGERY_KEY -> antiForgeryToken),
      wipeCookies = if (wipeAuthCookie) Set(authCookie) else Set.empty
    )
  }
}

object CookieResponses {
  def apply(settingsRefresher: PanDomainAuthSettingsRefresher): CookieResponses = new CookieResponses(
    settingsRefresher.settings.cookieSettings,
    () => settingsRefresher.settings.signingAndVerification,
    system = settingsRefresher.system, domain = settingsRefresher.domain
  )
}