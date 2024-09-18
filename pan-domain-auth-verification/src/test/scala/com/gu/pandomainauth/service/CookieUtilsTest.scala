package com.gu.pandomainauth.service

import com.gu.pandomainauth.model.{AuthenticatedUser, User}
import com.gu.pandomainauth.service.CookieUtils.CookieIntegrityFailure.{MalformedCookieText, SignatureNotValid}
import com.gu.pandomainauth.service.CookieUtils.{deserializeAuthenticatedUser, parseCookieData, serializeAuthenticatedUser}
import com.gu.pandomainauth.service.CryptoConf.{OnlyVerification, Signing}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, OptionValues}

import java.security.PrivateKey
import java.util.Date


class CookieUtilsTest extends AnyFreeSpec with Matchers with EitherValues with OptionValues {
  import TestKeys._

  val authUser = AuthenticatedUser(User("test", "üsér", "test.user@example.com", None), "testsuite", Set("testsuite", "another"), new Date().getTime + 86400, multiFactor = true)

  "generateCookieData" - {
    "generates a base64-encoded 'data.signature' cookie value" in {
      CookieUtils.generateCookieData(authUser, signingWith(testPrivateKey.key)) should fullyMatch regex "[\\w+/]+=*\\.[\\w+/]+=*".r
    }
  }

  "parseCookieData" - {
    val cryptoConf = OnlyVerification(testPublicKey.key)

    "can extract an authenticatedUser from real cookie data" in {
      val cookieData = CookieUtils.generateCookieData(authUser, signingWith(testPrivateKey.key))

      parseCookieData(cookieData, cryptoConf).value should equal(authUser)
    }

    "fails to extract invalid data with a SignatureNotValid" in {
      parseCookieData("data.invalidSignature", cryptoConf).left.value shouldBe SignatureNotValid
    }

    "fails to extract incorrectly signed data with a CookieSignatureInvalidException" in {
      val cookieData = CookieUtils.generateCookieData(authUser, signingWith(testINCORRECTPrivateKey.key))
      parseCookieData(cookieData, cryptoConf).left.value should equal(SignatureNotValid)
    }

    "fails to extract completely incorrect cookie data with a CookieParseException" in {
      parseCookieData("Completely incorrect cookie data", cryptoConf).left.value shouldBe MalformedCookieText
    }
  }

  "serialize/deserialize preserves data" in {
    deserializeAuthenticatedUser(serializeAuthenticatedUser(authUser)).value shouldEqual authUser
  }
}
