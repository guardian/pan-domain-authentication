package com.gu.pandomainauth.model

import java.util.Date

case class AuthenticatedUser(user: User, authenticatingSystem: String, authenticatedIn: Set[String], expires: Long, multiFactor: Boolean) {

  def isExpired = expires < new Date().getTime
  def isInGracePeriod(period: Long) = (expires + period) > new Date().getTime
}
