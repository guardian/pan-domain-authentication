package com.gu.pandomainauth.service

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import TestKeys._

class CookiePayloadTest extends AnyFreeSpec with Matchers with OptionValues {

  "CookiePayload" - {
    "round-trip from payload text to CookiePayload if matching public-private keys are used" in {
      val payloadText = "Boom"
      val payload = CookiePayload.generateForPayloadText(payloadText, testPrivateKey.key)
      payload.payloadTextVerifiedSignedWith(testPublicKey.key).value shouldBe payloadText
    }

    "not return payload text if a rogue key was used" in {
      val payload = CookiePayload.generateForPayloadText("Boom", testINCORRECTPrivateKey.key)
      payload.payloadTextVerifiedSignedWith(testPublicKey.key) shouldBe None
    }

    "reject cookie text that does not contain a ." in {
      CookiePayload.parse("AQIDBAUG") shouldBe None
    }

    "reject cookie text that does not contain valid BASE64 text" in {
      CookiePayload.parse("AQ!D.BAUG") shouldBe None
      CookiePayload.parse("AQID.BA!G") shouldBe None
    }

    "round-trip from correctly-formatted cookie-text to CookiePayload instance and back again" in {
      val correctlyFormattedCookieText = "AQID.BAUG"
      val cookiePayload = CookiePayload.parse(correctlyFormattedCookieText).value
      cookiePayload.asCookieText shouldBe correctlyFormattedCookieText
    }
  }
}