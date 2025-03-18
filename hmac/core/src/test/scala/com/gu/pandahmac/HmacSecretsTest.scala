package com.gu.pandahmac

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.net.URI
import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, ZoneId}

class HMACHeadersTest extends AnyWordSpec with Matchers {

  val uri = new URI("http:///www.theguardian.com/signin?query=someData")
  val validSecret = "secret"
  val expectedHMAC = "3AQ08uT4ToOISOXWMr68UvzrgrqIx3KK/pKEenwVES8="
  val dateHeaderValue = "Tue, 15 Nov 1994 08:12:00 GMT"
  val someTimeInThePast = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(dateHeaderValue))

  val fixedClock =
    Clock.fixed(someTimeInThePast, ZoneId.systemDefault)

  "HMACSecrets" when {
    "validateHMACHeaders" should {
      "throw an exception if called without secret or secretKeys set" in {
        val hmacSecrets = new HMACSecrets {}

        intercept[Exception] {
          hmacSecrets.validateHMACHeaders(dateHeaderValue, s"HMAC $expectedHMAC", uri)
        }
      }

      "return true if a valid secret is set" in {
        val hmacSecrets = new HMACSecrets {
          override val secret = validSecret
          override val clock: Clock = fixedClock
        }

        hmacSecrets.validateHMACHeaders(dateHeaderValue, s"HMAC $expectedHMAC", uri) should be(true)
      }

      "return true if any valid secret is in secretKeys" in {
        val hmacSecrets = new HMACSecrets {
          override val secretKeys = List("invalid", validSecret, "invalid")
          override val clock: Clock = fixedClock
        }

        hmacSecrets.validateHMACHeaders(dateHeaderValue, s"HMAC $expectedHMAC", uri) should be(true)
      }

      "preferentially use secretKeys over secret if both are provided" in {
        val hmacSecrets = new HMACSecrets {
          override val secret = "invalid"
          override val secretKeys = List("invalid", validSecret, "invalid")
          override val clock: Clock = fixedClock
        }

        hmacSecrets.validateHMACHeaders(dateHeaderValue, s"HMAC $expectedHMAC", uri) should be(true)
      }
    }
  }
}
