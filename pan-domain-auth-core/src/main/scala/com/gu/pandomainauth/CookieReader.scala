package com.gu.pandomainauth

import com.gu.pandomainauth.model.{AuthenticatedUser, CookieSettings}
import com.gu.pandomainauth.service.CookieUtils
import com.gu.pandomainauth.service.CookieUtils.CookieResult
import com.gu.pandomainauth.service.CryptoConf.Verification

class CookieReader(
  val cookieSettings: CookieSettings,
  verification: () => Verification
) {
  def extractExistingAuthFrom(pageRequest: PageRequest): Option[CookieResult[AuthenticatedUser]] = 
    pageRequest.cookies.get(cookieSettings.cookieName).map { cookieString =>
      CookieUtils.parseCookieData(cookieString, verification())
    }
}

object CookieReader {
  def apply(settingsRefresher: PanDomainAuthSettingsRefresher): CookieReader = new CookieReader(
    settingsRefresher.settings.cookieSettings,
    () => settingsRefresher.settings.signingAndVerification
  )
}