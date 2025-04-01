package com.gu.pandomainauth

import com.gu.pandomainauth.internal.planning.PageEndpoint.*
import com.gu.pandomainauth.PageEndpointAuthStatusHandler.{ANTI_FORGERY_KEY, TemporaryCookiesUsedForOAuth}
import com.gu.pandomainauth.ResponseModification.CookieChanges
import com.gu.pandomainauth.internal.planning.{OAuthCallbackEndpoint, PageEndpoint, PersistAuth}
import com.gu.pandomainauth.model.CookieSettings
import com.gu.pandomainauth.service.CookieUtils.generateCookieData
import com.gu.pandomainauth.service.CryptoConf.Signing

class CookieResponses(
  val cookieSettings: CookieSettings,
  signing: () => Signing,
  val system: String,
  domain: String
) {

  private val authCookie = CookieChanges.NameAndDomain(cookieSettings.cookieName, Some(domain))
  
  def pageEndpoint(respMod: PageEndpoint.RespMod): ResponseModification = ResponseModification(respMod match {
    case prepareForOAuth: PrepareForOAuth => CookieChanges(
      setSessionCookies = Map(ANTI_FORGERY_KEY -> prepareForOAuth.antiForgeryToken),
      wipeCookies = if (prepareForOAuth.wipeAuthCookie) Set(authCookie) else Set.empty
    )
    case persistAuth: PersistAuth => responseModificationFor(persistAuth)
    case Logout => CookieChanges(wipeCookies = Set(authCookie))
  })

  def oAuthCallbackEndpoint(respMod: OAuthCallbackEndpoint.RespMod): ResponseModification = ResponseModification(respMod match {
    case persistAuth: PersistAuth => responseModificationFor(persistAuth)
  })
  
  private def responseModificationFor(persistAuth: PersistAuth) = CookieChanges(
    setSessionCookies = Map(authCookie -> generateCookieData(persistAuth.authenticatedUser, signing())),
    wipeCookies = if (persistAuth.wipeTemporaryCookiesUsedForOAuth) TemporaryCookiesUsedForOAuth else Set.empty
  )
}

object CookieResponses {
  def apply(settingsRefresher: PanDomainAuthSettingsRefresher): CookieResponses = new CookieResponses(
    settingsRefresher.settings.cookieSettings,
    () => settingsRefresher.settings.signingAndVerification,
    system = settingsRefresher.system, domain = settingsRefresher.domain
  )
}