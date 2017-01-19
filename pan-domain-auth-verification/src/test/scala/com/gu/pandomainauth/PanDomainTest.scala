package com.gu.pandomainauth

import java.util.Date

import com.gu.pandomainauth.model._
import com.gu.pandomainauth.service.{LegacyCookie, CookieUtils}
import org.scalatest.{Inside, Matchers, FreeSpec}

class PanDomainTest extends FreeSpec with Matchers with Inside {
  import com.gu.pandomainauth.service.TestKeys._

  "authStatus" - {
    val authUser = AuthenticatedUser(User("test", "user", "test.user@example.com", None), "testsuite", Set("system"), new Date().getTime + 86400, multiFactor = true)
    val validCookieData = CookieUtils.generateCookieData(authUser, testPrivateKey)

    "returns `Authenticated` for valid cookie data that passes the validation check" in {
      PanDomain.authStatus(validCookieData, testPublicKey, _ => true, 0, "system", false) shouldBe a [Authenticated]
    }

    "gives back the provided auth user if successful" in {
      val cookieData = CookieUtils.generateCookieData(authUser, testPrivateKey)

      PanDomain.authStatus(cookieData, testPublicKey, _ => true, 0, "system", false) should equal(Authenticated(authUser))
    }

    "returns `InvalidCookie` if the cookie is not valid" in {
      PanDomain.authStatus("invalid cookie data", testPublicKey, _ => true, 0, "system", false) shouldBe a [InvalidCookie]
    }

    "returns `InvalidCookie` if the cookie fails its signature check" in {
      val incorrectCookieData = CookieUtils.generateCookieData(authUser, testINCORRECTPrivateKey)

      PanDomain.authStatus(incorrectCookieData, testPublicKey, _ => true, 0, "system", false) shouldBe a [InvalidCookie]
    }

    "when the user's login has expired" - {
      "returns `Expired` if the time is after the cookie's expiry" in {
        val expiredAuthUser = authUser.copy(expires = new Date().getTime - 86400)
        val cookieData = CookieUtils.generateCookieData(expiredAuthUser, testPrivateKey)

        PanDomain.authStatus(cookieData, testPublicKey, _ => true, 0, "system", false) shouldBe a [Expired]
      }

      "returns `Expired` if the cookie has expired and is outside the grace period" in {
        val expiredAuthUser = authUser.copy(expires = new Date().getTime - 86400)
        val cookieData = CookieUtils.generateCookieData(expiredAuthUser, testPrivateKey)

        PanDomain.authStatus(cookieData, testPublicKey, _ => true, 3600, "system", false) shouldBe a [Expired]
      }

      "returns grace period if the cookie has expired but is within the grace period" in {
        val expiredAuthUser = authUser.copy(expires = new Date().getTime - 3000)
        val cookieData = CookieUtils.generateCookieData(expiredAuthUser, testPrivateKey)

        PanDomain.authStatus(cookieData, testPublicKey, _ => true, 3600, "system", false) shouldBe a [GracePeriod]
      }
    }

    "correctle handles the verification check" - {
      val invalid: AuthenticatedUser => Boolean = _ => false
      val valid: AuthenticatedUser => Boolean = _ => true

      "without cached validation, " - {
        val notCached = false

        "returns NotAuthorized if the user fails the validation check" in {
          PanDomain.authStatus(validCookieData, testPublicKey, invalid, 0, "system", notCached) shouldBe a [NotAuthorized]
        }

        "returns Authenticated if the user passes the validation check" in {
          PanDomain.authStatus(validCookieData, testPublicKey, valid, 0, "system", notCached) shouldBe a [Authenticated]
        }
      }

      "when validation is cached, " - {
        val cached = true

        "returns NotAuthorized if the user is not authorized in this system" in {
          val status = PanDomain.authStatus(validCookieData, testPublicKey, invalid, 0, "not-the-system", cacheValidation = true)
          status shouldBe a [NotAuthorized]
        }

        "returns Authenticated if the user was authenticated in this system, even if they would fail the validation check" in {
          val status = PanDomain.authStatus(validCookieData, testPublicKey, invalid, 0, "system", cacheValidation = true)
          status shouldBe a [Authenticated]
        }

        "does not call the validateUser function if caching is enabled" in {
          var called = false
          val validateUser: AuthenticatedUser => Boolean = { _ =>
            called = true
            false
          }
          PanDomain.authStatus(validCookieData, testPublicKey, validateUser, 0, "system", cacheValidation = true)
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
