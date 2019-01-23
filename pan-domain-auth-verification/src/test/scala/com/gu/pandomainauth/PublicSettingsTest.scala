package com.gu.pandomainauth

import com.gu.pandomainauth.service.TestKeys
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, FreeSpec, Matchers}


class PublicSettingsTest extends FreeSpec with Matchers with EitherValues with ScalaFutures {
  "validateKey" - {
    "returns an error if the key looks invalid" in {
      val invalidKey = PublicKey("not a valid key")
      PublicSettings.validateKey(invalidKey).left.value shouldEqual PublicKeyFormatFailure
    }

    "returns the key if it is valid" in {
      val key = TestKeys.testPublicKey
      PublicSettings.validateKey(key).right.value shouldEqual key
    }
  }

  "extractPublicKey" - {
    "will get a public key from a valid settings map" in {
      PublicSettings.extractPublicKey(Map("publicKey" -> TestKeys.testPublicKey.key)).right.value shouldEqual TestKeys.testPublicKey
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

      Settings.extractSettings(body).right.value shouldEqual Map("key" -> "value", "foo" -> "bar")
    }
  }
}
