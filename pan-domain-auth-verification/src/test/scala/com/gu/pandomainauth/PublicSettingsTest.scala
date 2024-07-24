package com.gu.pandomainauth

import com.gu.pandomainauth.service.TestKeys.testPublicKey
import com.gu.pandomainauth.service.{Crypto, TestKeys}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers


class PublicSettingsTest extends AnyFreeSpec with Matchers with EitherValues with ScalaFutures {
  "validateKey" - {
    "returns an error if the key looks invalid" in {
      val invalidKeyText = "not a valid key"
      PublicSettings.validateAndParseKeyText(invalidKeyText).left.value shouldEqual PublicKeyFormatFailure
    }

    "returns the key if it is valid" in {
      PublicSettings.validateAndParseKeyText(testPublicKey.base64Encoded) shouldEqual Right(testPublicKey.key)
    }
  }

  "extractPublicKey" - {
    "will get a public key from a valid settings map" in {
      PublicSettings.extractPublicKey(Map("publicKey" -> testPublicKey.base64Encoded)) shouldEqual Right(testPublicKey.key)
    }

    "will reject a key that is not correctly formatted" in {
      PublicSettings.extractPublicKey(Map("publicKey" -> "improperly formatted public key!!")).left.value should be(PublicKeyFormatFailure)
    }

    "will fail if the key is not present in the settings" in {
      PublicSettings.extractPublicKey(Map("another key" -> "bar")).left.value should be(PublicKeyNotFoundFailure)
    }
  }

  "extractSettings" - {
    "extracts properties from a valid body" in {
      val body =
        """key=value
          |foo=bar
        """.stripMargin

      Settings.extractSettings(body) shouldEqual Right(Map("key" -> "value", "foo" -> "bar"))
    }
  }
}
