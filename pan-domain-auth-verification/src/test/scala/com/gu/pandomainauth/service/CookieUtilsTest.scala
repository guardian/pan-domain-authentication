package com.gu.pandomainauth.service

import java.util.Date

import com.gu.pandomainauth.model.{AuthenticatedUser, CookieParseException, CookieSignatureInvalidException, User}
import org.scalatest.{FreeSpec, Matchers}


class CookieUtilsTest extends FreeSpec with Matchers {
  import TestKeys._

  val authUser = AuthenticatedUser(User("test", "user", "test.user@example.com", None), "testsuite", Set("testsuite"), new Date().getTime + 86400, multiFactor = true)

  "generateCookieData" - {
    "generates a a base64-encoded 'data.signature' cookie value" in {
      CookieUtils.generateCookieData(authUser, testPrivateKey) should fullyMatch regex "(?i)[a-z0-9+/]+=*\\.[a-z0-9+/]+=*".r
    }
  }

  "parseCookieData" - {
    "can extract an authenticatedUser from real cookie data" in {
      val cookieData = CookieUtils.generateCookieData(authUser, testPrivateKey)
      CookieUtils.parseCookieData(cookieData, testPublicKey) should equal(authUser)
    }

    "fails to extract invalid data with a CookieSignatureInvalidException" in {
      val cookieData = CookieUtils.generateCookieData(authUser, testINCORRECTPrivateKey)
      intercept[CookieSignatureInvalidException] {
        CookieUtils.parseCookieData("data.invalidSignature", testPublicKey)
      }

    }

    "fails to extract incorrectly signed data with a CookieSignatureInvalidException" - {
      val cookieData = CookieUtils.generateCookieData(authUser, testINCORRECTPrivateKey)
      intercept[CookieSignatureInvalidException] {
        CookieUtils.parseCookieData(cookieData, testPublicKey)
      }
    }

    "fails to extract completely incorrect cookie data with a CookieParseException" - {
      intercept[CookieParseException] {
        CookieUtils.parseCookieData("Completely incorrect cookie data", testPublicKey)
      }
    }
  }
}
