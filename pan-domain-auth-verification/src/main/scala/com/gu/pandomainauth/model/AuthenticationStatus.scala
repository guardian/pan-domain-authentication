package com.gu.pandomainauth.model

import com.gu.pandomainauth.service.CookieUtils.CookieIntegrityFailure

sealed trait AuthenticationStatus
sealed trait HasUser {
  val authedUser: AuthenticatedUser
}
sealed trait StaleUserAuthentication extends HasUser
sealed trait AcceptableAuthForApiRequests extends HasUser {
  val shouldBeRefreshed: Boolean
}

case class Expired(authedUser: AuthenticatedUser) extends AuthenticationStatus with StaleUserAuthentication
case class GracePeriod(authedUser: AuthenticatedUser) extends AuthenticationStatus with StaleUserAuthentication with AcceptableAuthForApiRequests {
  override val shouldBeRefreshed: Boolean = true
}
case class Authenticated(authedUser: AuthenticatedUser) extends AuthenticationStatus with AcceptableAuthForApiRequests {
  override val shouldBeRefreshed: Boolean = false
}
case class NotAuthorized(authedUser: AuthenticatedUser) extends AuthenticationStatus
case class InvalidCookie(e: CookieIntegrityFailure) extends AuthenticationStatus
case object NotAuthenticated extends AuthenticationStatus
