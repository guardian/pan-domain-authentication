package com.gu.pandomainauth

import com.gu.pandomainauth.model._
import com.gu.pandomainauth.service.{LegacyCookie, CookieUtils}


object PanDomain {
  /**
   * Check the authentication status of the provided credentials by examining the signed cookie data.
   */
  def authStatus(cookieData: String, publicKey: PublicKey, validateUser: AuthenticatedUser => Boolean = guardianValidation): AuthenticationStatus = {
    try {
      val authedUser = CookieUtils.parseCookieData(cookieData, publicKey)
      checkStatus(authedUser, validateUser)
    } catch {
      case e: Exception =>
        InvalidCookie(e)
    }
  }

  /**
   * Cookie check that includes the legacy cookie. This is required during a period of transition
   * between the old cookie and the new assymetric one.
   */
  def authStatusWithLegacyCheck(cookieData: String, publicKey: PublicKey, secret: Secret): AuthenticationStatus = {
    try {
      val authedUser = try {
        CookieUtils.parseCookieData(cookieData, publicKey)
      } catch {
        case e: Exception =>
          LegacyCookie.parseCookieData(cookieData, secret)
      }
      checkStatus(authedUser, _ => true)
    } catch {
      case e: Exception =>
        InvalidCookie(e)
    }
  }

  private def checkStatus(authedUser: AuthenticatedUser, validateUser: AuthenticatedUser => Boolean = guardianValidation): AuthenticationStatus = {
    if (authedUser.isExpired) {
      Expired(authedUser)
    } else if (validateUser(authedUser)) {
      Authenticated(authedUser)
    } else {
      NotAuthorized(authedUser)
    }
  }

  val guardianValidation: AuthenticatedUser => Boolean = { authedUser =>
    (authedUser.user.emailDomain == "guardian.co.uk") && authedUser.multiFactor
  }
}
