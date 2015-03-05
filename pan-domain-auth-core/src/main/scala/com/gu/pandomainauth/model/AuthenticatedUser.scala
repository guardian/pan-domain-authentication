package com.gu.pandomainauth.model

import java.util.Date

case class AuthenticatedUser(user: User, authenticatingSystem: String, authenticatedIn: Set[String], expires: Long, multiFactor: Boolean) {

  def isExpired = expires < new Date().getTime
  def hasExpiryExtension(extension: Long) = (expires + extension) > new Date().getTime
}
