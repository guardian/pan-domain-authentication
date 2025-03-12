package com.gu.pandomainauth

import com.gu.pandomainauth.SampleConf.loadExample
import com.gu.pandomainauth.service.CryptoConf
import com.gu.pandomainauth.service.CryptoConf.{SettingsReader, SigningAndVerification}
import com.gu.pandomainauth.service.TestKeys.testPublicKey
import org.scalatest.{EitherValues, OptionValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets.UTF_8

object SampleConf {
  def loadExample(name: String): SigningAndVerification = {
    val settingsText =
      new String(getClass.getResourceAsStream(s"/crypto-conf-rotation-example/$name.settings").readAllBytes(), UTF_8)
    SettingsReader(Settings.extractSettings(settingsText).toOption.get).signingAndVerificationConf.toOption.get
  }
}

class CryptoConfTest extends AnyFreeSpec with Matchers with EitherValues with OptionValues {
  val legacyConf = loadExample("0.legacy")
  val rotationUpcomingConf = loadExample("1.rotation-upcoming")
  val rotationInProgressConf = loadExample("2.rotation-in-progress")
  val rotationCompleteConf = loadExample("3.rotation-complete")

  "loading crypto configuration" - {
    "follow a safe set of transition steps" in {
      legacyConf.alsoAccepted shouldBe empty

      rotationUpcomingConf.activeKeyPair should === (legacyConf.activeKeyPair)
      rotationUpcomingConf.alsoAccepted should not be empty
      val expectedAcceptedPublicKeys = rotationUpcomingConf.activeKeyPair.publicKey +: rotationUpcomingConf.alsoAccepted
      rotationUpcomingConf.acceptedPublicKeys should === (expectedAcceptedPublicKeys)

      rotationInProgressConf.activeKeyPair should !== (legacyConf.activeKeyPair)
      rotationInProgressConf.alsoAccepted shouldBe Seq(legacyConf.activeKeyPair.publicKey)

      rotationCompleteConf.activeKeyPair should === (rotationInProgressConf.activeKeyPair)
      rotationCompleteConf.alsoAccepted shouldBe empty
    }

    "have transitions that are reported as safe" in {
      val stages = Seq(legacyConf, rotationUpcomingConf, rotationInProgressConf, rotationCompleteConf)
      for {
        (before, after) <- stages.zip(stages.tail)
      } {
        val change = CryptoConf.Change.compare(before, after).value
        println(change.summary)
        change.isBreakingChange shouldBe false
      }
    }

    "report a bad transition if the old active key is not still tolerated" in {
      val change = CryptoConf.Change.compare(rotationUpcomingConf, rotationCompleteConf).value
      println(change.summary)
      change.isBreakingChange shouldBe true
    }

    "report a bad transition if the new active key wasn't already accepted by the old config" in {
      val change = CryptoConf.Change.compare(legacyConf, rotationInProgressConf).value
      println(change.summary)
      change.isBreakingChange shouldBe true
    }
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
