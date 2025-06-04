package com.gu.pandomainauth

import com.gu.pandomainauth.PanDomain.DefaultApiGracePeriod
import com.gu.pandomainauth.model.{AuthenticatedUser, AuthenticationStatus, CookieSettings, NotAuthenticated}
import com.gu.pandomainauth.service.CryptoConf.Verification

import java.time.Duration

class AuthStatusFromRequest(
  val cookieSettings: CookieSettings,
  val system: String,
  verification: () => Verification,
  validateUser: AuthenticatedUser => Boolean,
  cacheValidation: Boolean,
  apiGracePeriod: Duration = DefaultApiGracePeriod
) {
  def authStatusFor(request: PageRequest): AuthenticationStatus = request.cookies.get(cookieSettings.cookieName).map { cookie =>
    val forceExpiry: Boolean = ???
    PanDomain.authStatus(cookie, verification(), validateUser, system, cacheValidation, forceExpiry, apiGracePeriod)
  } getOrElse NotAuthenticated
}

object AuthStatusFromRequest {
  def apply(
    settingsRefresher: PanDomainAuthSettingsRefresher,
    validateUser: AuthenticatedUser => Boolean,
    cacheValidation: Boolean
  ): AuthStatusFromRequest = new AuthStatusFromRequest(
    settingsRefresher.settings.cookieSettings, settingsRefresher.system,
    () => settingsRefresher.settings.signingAndVerification,
    validateUser: AuthenticatedUser => Boolean,
    cacheValidation)
}