package com.gu.pandomainauth.service

import java.util.Date

import com.gu.pandomainauth.model.{AuthenticatedUser, CookieParseException, CookieSignatureInvalidException, User}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers


class CookieUtilsTest extends AnyFreeSpec with Matchers {
  import TestKeys._

  val authUser = AuthenticatedUser(User("test", "üsér", "test.user@example.com", None), "testsuite", Set("testsuite", "another"), new Date().getTime + 86400, multiFactor = true)

  "generateCookieData" - {
    "generates a a base64-encoded 'data.signature' cookie value" in {
      CookieUtils.generateCookieData(authUser, testPrivateKey.key) should fullyMatch regex "[\\w+/]+=*\\.[\\w+/]+=*".r
    }
  }

  "parseCookieData" - {
    "can extract an authenticatedUser from real cookie data" in {
      val cookieData = CookieUtils.generateCookieData(authUser, testPrivateKey.key)
      CookieUtils.parseCookieData(cookieData, testPublicKey.key) should equal(authUser)
    }

    "fails to extract invalid data with a CookieSignatureInvalidException" in {
      val cookieData = CookieUtils.generateCookieData(authUser, testINCORRECTPrivateKey.key)
      intercept[CookieSignatureInvalidException] {
        CookieUtils.parseCookieData("data.invalidSignature", testPublicKey.key)
      }
    }

    "fails to extract incorrectly signed data with a CookieSignatureInvalidException" - {
      val cookieData = CookieUtils.generateCookieData(authUser, testINCORRECTPrivateKey.key)
      intercept[CookieSignatureInvalidException] {
        CookieUtils.parseCookieData(cookieData, testPublicKey.key)
      }
    }

    "fails to extract completely incorrect cookie data with a CookieParseException" - {
      intercept[CookieParseException] {
        CookieUtils.parseCookieData("Completely incorrect cookie data", testPublicKey.key)
      }
    }
  }

  "serialize/deserialize preserves data" in {
    CookieUtils.deserializeAuthenticatedUser(CookieUtils.serializeAuthenticatedUser(authUser)) should equal(authUser)
  }
}
