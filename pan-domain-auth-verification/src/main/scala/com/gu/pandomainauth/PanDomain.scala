package com.gu.pandomainauth

import com.gu.pandomainauth.model._
import com.gu.pandomainauth.service.CookieUtils
import com.gu.pandomainauth.service.CryptoConf.Verification

import java.time.Duration
import java.time.Duration.ofHours


object PanDomain {

  /**
   * The Panda cookie expires after one hour, and top-level navigation that
   * requests HTML will trigger a re-auth and refresh the session after this point.
   *
   * However, fetch requests to API endpoints are unable to do this re-auth,
   * and mechanisms for refreshing in the background without top-level nav or popups
   * (such as iframes) are increasingly locked down by third-party cookie restrictions.
   *
   * So for API requests, we provide a further 24 hours grace period within which requests
   * will continue to work, and the app UI should signal to the user that they need
   * to refresh the page to re-auth.
   *
   * Panda cookie:  issued       expires
   *                |-- 1 hour --|
   * Grace period:               [------------- 24 hours -------------]
   * API request:   [-succeeds----------------------------------------][-fails---->
   * Top-level nav: [-succeeds--][-fails-but-would-trigger-re-auth---------------->
   *
   * @return delay between cookie expiry and API requests failing
   */
  val DefaultApiGracePeriod: Duration = ofHours(24)

  /**
   * Check the authentication status of the provided credentials by examining the signed cookie data.
   */
  def authStatus(cookieData: String, verification: Verification, validateUser: AuthenticatedUser => Boolean,
                 system: String, cacheValidation: Boolean, forceExpiry: Boolean,
                 apiGracePeriod: Duration = DefaultApiGracePeriod): AuthenticationStatus = {
    CookieUtils.parseCookieData(cookieData, verification).fold(InvalidCookie(_), { authedUser =>
      checkStatus(authedUser, validateUser, apiGracePeriod, system, cacheValidation, forceExpiry)
    })
  }

  private def checkStatus(authedUser: AuthenticatedUser, validateUser: AuthenticatedUser => Boolean,
                          apiGracePeriod: Duration, system: String, cacheValidation: Boolean,
                          forceExpiry: Boolean): AuthenticationStatus = {

    if (authedUser.isExpired && authedUser.isInGracePeriod(apiGracePeriod)) {
      // expired, but in grace period - check user is valid, GracePeriod if so
      if (cacheValidation && authedUser.authenticatedIn(system)) {
        // if validation is cached, check user has been validated here
        GracePeriod(authedUser)
      } else if (validateUser(authedUser)) {
        // validation says this user is ok
        GracePeriod(authedUser)
      } else {
        // the user is in the grace period but has failed validation
        NotAuthorized(authedUser)
      }
    } else if (authedUser.isExpired || forceExpiry) {
      // expired and outside grace period
      Expired(authedUser)
    } else if (cacheValidation && authedUser.authenticatedIn(system)) {
      // if cacheValidation is enabled, check the user was validated here
      Authenticated(authedUser)
    } else if (validateUser(authedUser)) {
      // fresh validation says the user is valid
      Authenticated(authedUser)
    } else {
      // user has not expired but has failed validation checks
      NotAuthorized(authedUser)
    }
  }

  val guardianValidation: AuthenticatedUser => Boolean = { authedUser =>
    (authedUser.user.emailDomain == "guardian.co.uk") && authedUser.multiFactor
  }
}
