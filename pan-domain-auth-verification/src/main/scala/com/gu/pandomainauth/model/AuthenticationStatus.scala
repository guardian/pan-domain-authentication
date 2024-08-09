package com.gu.pandomainauth.model

import com.gu.pandomainauth.service.CookieUtils.CookieIntegrityFailure

sealed trait AuthenticationStatus
case class Expired(authedUser: AuthenticatedUser) extends AuthenticationStatus
case class GracePeriod(authedUser: AuthenticatedUser) extends AuthenticationStatus
case class Authenticated(authedUser: AuthenticatedUser) extends AuthenticationStatus
case class NotAuthorized(authedUser: AuthenticatedUser) extends AuthenticationStatus
case class InvalidCookie(e: CookieIntegrityFailure) extends AuthenticationStatus
case object NotAuthenticated extends AuthenticationStatus
