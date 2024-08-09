package com.gu.pandomainauth

import com.gu.pandomainauth.service.CryptoConf.SettingsReader
import com.gu.pandomainauth.service.TestKeys.testPublicKey
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers


class CryptoConfTest extends AnyFreeSpec with Matchers with EitherValues {
  "CryptoConf.SettingsReader" - {
    "returns an error if the key looks invalid" in {
      SettingsReader.publicKeyFor("not a valid key").left.value shouldEqual PublicKeyFormatFailure
    }

    "returns the key if it is valid" in {
      SettingsReader.publicKeyFor(testPublicKey.base64Encoded) shouldEqual Right(testPublicKey.key)
    }
  }

  "CryptoConf.SettingsReader activePublicKey" - {
    "will get a public key from a valid settings map" in {
      SettingsReader(Map("publicKey" -> testPublicKey.base64Encoded)).activePublicKey shouldEqual Right(testPublicKey.key)
    }

    "will reject a key that is not correctly formatted" in {
      SettingsReader(Map("publicKey" -> "improperly formatted public key!!")).activePublicKey.left.value should be(InvalidBase64)
    }

    "will fail if the key is not present in the settings" in {
      SettingsReader(Map("another key" -> "bar")).activePublicKey.left.value should be(MissingSetting("publicKey"))
    }
  }
}
