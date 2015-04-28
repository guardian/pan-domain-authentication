package com.gu.pandomainauth

import com.gu.pandomainauth.model._
import com.gu.pandomainauth.service.CookieUtils


object PanDomain {
  /**
   * Check the authentication status of the provided credentials by examining the signed cookie data.
   */
  def authStatus(cookieData: String, publicKey: String, validateUser: AuthenticatedUser => Boolean): AuthenticationStatus = {
    try {
      val authedUser = CookieUtils.parseCookieData(cookieData, publicKey)

      if (authedUser.isExpired) {
        Expired(authedUser)
      } else if (validateUser(authedUser)) {
        Authenticated(authedUser)
      } else {
        NotAuthorized(authedUser)
      }
    } catch {
      case e: Exception =>
        InvalidCookie(e)
    }
  }
}
