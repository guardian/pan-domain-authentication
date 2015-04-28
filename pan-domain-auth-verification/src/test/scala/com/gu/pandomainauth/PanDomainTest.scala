package com.gu.pandomainauth

import java.util.Date

import com.gu.pandomainauth.model._
import com.gu.pandomainauth.service.CookieUtils
import org.scalatest.{Inside, Matchers, FreeSpec}

class PanDomainTest extends FreeSpec with Matchers with Inside {
  import com.gu.pandomainauth.service.TestKeys._

  "authStatus" - {
    val authUser = AuthenticatedUser(User("test", "user", "test.user@example.com", None), "testsuite", Set("testsuite"), new Date().getTime + 86400, multiFactor = true)

    "returns `Authenticated` for valid cookie data that passes the validation check" in {
      def validateUser(au: AuthenticatedUser): Boolean = au.multiFactor && au.user.emailDomain == "example.com"
      val cookieData = CookieUtils.generateCookieData(authUser, testPrivateKey)

      PanDomain.authStatus(cookieData, testPublicKey, _ => true) shouldBe a [Authenticated]
    }

    "gives back the provided auth user if successful" in {
      val cookieData = CookieUtils.generateCookieData(authUser, testPrivateKey)

      PanDomain.authStatus(cookieData, testPublicKey, _ => true) should equal(Authenticated(authUser))
    }

    "returns `InvalidCookie` if the cookie is not valid" in {
      PanDomain.authStatus("invalid cookie data", testPublicKey, _ => true) shouldBe a [InvalidCookie]
    }

    "returns `InvalidCookie` if the cookie fails its signature check" in {
      val incorrectCookieData = CookieUtils.generateCookieData(authUser, testINCORRECTPrivateKey)

      PanDomain.authStatus(incorrectCookieData, testPublicKey, _ => true) shouldBe a [InvalidCookie]
    }

    "returns `Expired` if the time is after the cookie's expiry" in {
      val expiredAuthUser = authUser.copy(expires = new Date().getTime - 86400)
      val cookieData = CookieUtils.generateCookieData(expiredAuthUser, testPrivateKey)

      PanDomain.authStatus(cookieData, testPublicKey, _ => true) shouldBe a [Expired]
    }

    "returns `NotAuthorized` if the cookie does not pass the verification check" in {
      def validateUser(au: AuthenticatedUser): Boolean = au.multiFactor && au.user.emailDomain == "example.com"
      val cookieData = CookieUtils.generateCookieData(authUser, testPrivateKey)

      PanDomain.authStatus(cookieData, testPublicKey, _ => false) shouldBe a [NotAuthorized]
    }
  }
}
