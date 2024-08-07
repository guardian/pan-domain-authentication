package com.gu.pandomainauth

import com.gu.pandomainauth.service.CryptoConf.{SettingsReader, SigningAndVerification}
import com.gu.pandomainauth.service.TestKeys.testPublicKey
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets.UTF_8


class CryptoConfTest extends AnyFreeSpec with Matchers with EitherValues {
  "loading crypto configuration" - {
    "funky" in {
      val legacyConf = loadExample("0.legacy")
      legacyConf.alsoAccepted shouldBe empty

      val rotationUpcomingConf = loadExample("1.rotation-upcoming")
      rotationUpcomingConf.activeKeyPair should === (legacyConf.activeKeyPair)
      rotationUpcomingConf.alsoAccepted should not be empty

      val rotationInProgressConf = loadExample("2.rotation-in-progress")
      rotationInProgressConf.activeKeyPair should !== (legacyConf.activeKeyPair)
      rotationInProgressConf.alsoAccepted shouldBe Seq(legacyConf.activeKeyPair.publicKey)

      val rotationCompleteConf = loadExample("3.rotation-complete")
      rotationCompleteConf.activeKeyPair should === (rotationInProgressConf.activeKeyPair)
      rotationCompleteConf.alsoAccepted shouldBe empty
    }
  }

  private def loadExample(name: String): SigningAndVerification = {
    val settingsText =
      new String(getClass.getResourceAsStream(s"/crypto-conf-rotation-example/$name.settings").readAllBytes(), UTF_8)
    SettingsReader(Settings.extractSettings(settingsText).value).signingAndVerificationConf.value
  }

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
