package com.gu.pandomainauth.service

import java.util.Date

import com.gu.pandomainauth.Secret
import com.gu.pandomainauth.model.{AuthenticatedUser, CookieParseException, CookieSignatureInvalidException, User}
import org.scalatest.{FreeSpec, Matchers}

class LegacyCookieTest extends FreeSpec with Matchers {
  val authUser = AuthenticatedUser(User("test", "user", "test.user@example.com", None), "testsuite", Set("testsuite", "another"), new Date().getTime + 86400, multiFactor = true)
  val data = CookieUtils.serializeAuthenticatedUser(authUser)
  val secret = Secret("testSecret")

  "generateCookieData should create a cookie value with the correct structure" in {
    LegacyCookie.generateCookieData(authUser, secret) should fullyMatch regex "^^([\\w\\W]*)>>([\\w\\W]*)$".r
  }
}
