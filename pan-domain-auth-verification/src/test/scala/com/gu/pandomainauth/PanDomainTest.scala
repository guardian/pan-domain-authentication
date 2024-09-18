package com.gu.pandomainauth

import com.gu.pandomainauth.model._
import com.gu.pandomainauth.service.CookieUtils
import com.gu.pandomainauth.service.CryptoConf.OnlyVerification
import org.scalatest.Inside
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.Date

class PanDomainTest extends AnyFreeSpec with Matchers with Inside {
  import com.gu.pandomainauth.service.TestKeys._


  def authStatus(
    cookieData: String,
    validateUser: AuthenticatedUser => Boolean = _ => true,
    apiGracePeriod: Long = 0,
    system: String = "testsuite",
    cacheValidation: Boolean = false,
    forceExpiry: Boolean = false,
  ) = PanDomain.authStatus(cookieData, OnlyVerification(testPublicKey.key), validateUser, apiGracePeriod, system, cacheValidation, forceExpiry)

  "authStatus" - {
    val authUser = AuthenticatedUser(User("test", "user", "test.user@example.com", None), "testsuite", Set("testsuite"), new Date().getTime + 86400, multiFactor = true)
    val validCookieData = CookieUtils.generateCookieData(authUser, signingWith(testPrivateKey.key))

    "returns `Authenticated` for valid cookie data that passes the validation check" in {
      def validateUser(au: AuthenticatedUser): Boolean = au.multiFactor && au.user.emailDomain == "example.com"
      val cookieData = CookieUtils.generateCookieData(authUser, signingWith(testPrivateKey.key))

      authStatus(cookieData, validateUser) shouldBe a [Authenticated]
    }

    "gives back the provided auth user if successful" in {
      val cookieData = CookieUtils.generateCookieData(authUser, signingWith(testPrivateKey.key))

      authStatus(cookieData) should equal(Authenticated(authUser))
    }

    "returns `InvalidCookie` if the cookie is not valid" in {
      authStatus("invalid cookie data") shouldBe a [InvalidCookie]
    }

    "returns `InvalidCookie` if the cookie fails its signature check" in {
      val incorrectCookieData = CookieUtils.generateCookieData(authUser, signingWith(testINCORRECTPrivateKey.key))

      authStatus(incorrectCookieData) shouldBe a [InvalidCookie]
    }

    "returns `Expired` if the time is after the cookie's expiry" in {
      val expiredAuthUser = authUser.copy(expires = new Date().getTime - 86400)
      val cookieData = CookieUtils.generateCookieData(expiredAuthUser, signingWith(testPrivateKey.key))

      authStatus(cookieData) shouldBe a [Expired]
    }

    "returns `Expired` if the cookie has expired and is outside the grace period" in {
      val expiredAuthUser = authUser.copy(expires = new Date().getTime - 86400)
      val cookieData = CookieUtils.generateCookieData(expiredAuthUser, signingWith(testPrivateKey.key))

      authStatus(cookieData) shouldBe a [Expired]
    }

    "returns `Expired` if cookie has not expired, but forceExpiry is set" in {
      val validCookieData = CookieUtils.generateCookieData(authUser, signingWith(testPrivateKey.key))
      authStatus(validCookieData, forceExpiry = true) shouldBe a [Expired]
    }

    "returns `GracePeriod` if the cookie has expired but is within the grace period" in {
      val expiredAuthUser = authUser.copy(expires = new Date().getTime - 3000)
      val cookieData = CookieUtils.generateCookieData(expiredAuthUser, signingWith(testPrivateKey.key))

      authStatus(cookieData, apiGracePeriod = 3600) shouldBe a [GracePeriod]
    }

    "returns `NotAuthorized` if the cookie does not pass the verification check" in {
      def validateUser(au: AuthenticatedUser): Boolean = au.multiFactor && au.user.emailDomain == "example.com"
      val cookieData = CookieUtils.generateCookieData(authUser, signingWith(testPrivateKey.key))

      authStatus(cookieData, _ => false) shouldBe a [NotAuthorized]
    }

    "correctly handles the verification check" - {
      val invalid: AuthenticatedUser => Boolean = _ => false
      val valid: AuthenticatedUser => Boolean = _ => true

      "without cache validation, " - {
        "returns `NotAuthorized` if the user fails the validation check" in {
          authStatus(validCookieData, _ => false) shouldBe a [NotAuthorized]
        }

        "returns `Authenticated` if the user passes the validation check" in {
          authStatus(validCookieData, _ => true) shouldBe a [Authenticated]
        }
      }

      "when validation is cached" - {
        "returns `NotAuthorized` if the user is not authorized in this system" in {
          val status = authStatus(validCookieData, invalid, system = "not-the-system", cacheValidation = true)
          status shouldBe a [NotAuthorized]
        }

        "returns Authenticated if the user was authenticated in this system, even if they would fail the validation check" in {
          val status = authStatus(validCookieData, invalid, cacheValidation = true)
          status shouldBe a [Authenticated]
        }

        "does not call the validateUser function if caching is enabled" in {
          var called = false
          val validateUser: AuthenticatedUser => Boolean = { _ =>
            called = true
            false
          }
          authStatus(validCookieData, validateUser, cacheValidation = true)
          called shouldEqual false
        }
      }
    }
  }

  "guardianValidation" - {
    val validUser = AuthenticatedUser(User("example", "user", "example@guardian.co.uk", None), "tests", Set("tests"), new Date().getTime + 86400, multiFactor = true)

    "returns true for a multi-factor user with a Guardian email address" in {
      PanDomain.guardianValidation(validUser) should equal(true)
    }

    "returns false for a multi-factor user without a guardian email address" in {
      val invalidUser = validUser.copy(user = validUser.user.copy(email = "notGaurdian@example.com"))
      PanDomain.guardianValidation(invalidUser) should equal(false)
    }

    "returns false for a guardian email address that is not authenticated with multi-factor-auth" in {
      val invalidUser = validUser.copy(multiFactor = false)
      PanDomain.guardianValidation(invalidUser) should equal(false)
    }

    "returns false for something that looks a bit like a guardian domain" in {
      val invalidUser = validUser.copy(user = validUser.user.copy(email = "notQuiteGaurdian@notguardian.co.uk"))
      PanDomain.guardianValidation(invalidUser) should equal(false)
    }
  }
}
