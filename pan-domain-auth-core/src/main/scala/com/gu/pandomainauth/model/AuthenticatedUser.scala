package com.gu.pandomainauth.model

case class AuthenticatedUser(user: User, authenticatingSystem: String, authenticatedIn: Set[String], expires: Long, multiFactor: Boolean)
