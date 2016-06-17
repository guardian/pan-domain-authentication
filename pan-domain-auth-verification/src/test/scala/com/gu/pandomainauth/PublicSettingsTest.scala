package com.gu.pandomainauth

import java.io.IOException

import com.gu.pandomainauth.PublicSettings.{PublicKeyFormatException, PublicKeyNotFoundException, PublicSettingsAcquisitionException}
import com.gu.pandomainauth.service.TestKeys
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, FreeSpec, Matchers}


class PublicSettingsTest extends FreeSpec with Matchers with EitherValues with ScalaFutures {
  "validateKey" - {
    "returns an error if the key looks invalid" in {
      val invalidKey = PublicKey("not a valid key")
      PublicSettings.validateKey(invalidKey).left.value shouldEqual PublicKeyFormatException
    }

    "returns an error if we have an S3 error instead of a key value" in {
      val invalidKey = PublicKey("""<?xml version="1.0" encoding="UTF-8"?>
                         |<Error><Code>AccessDenied</Code><Message>Access Denied</Message><RequestId>5E5E15297AEDE946</RequestId><HostId>QmRYsD7HQmq7GtKC7CC9rZsv0MC/nWrppQQrAoMSNWku2ySkox5TlFIF8hk5wAUETeUa3xG9Jo4=</HostId></Error>""")
      PublicSettings.validateKey(invalidKey).left.value shouldEqual PublicKeyFormatException
    }

    "returns the key if it is valid" in {
      val key = TestKeys.testPublicKey
      PublicSettings.validateKey(key).right.value shouldEqual key
    }
  }

  "extractPublicKey" - {
    "will get a public key from a valid settings map" in {
      PublicSettings.extractPublicKey(Map("publicKey" -> TestKeys.testPublicKey.key)).futureValue shouldEqual TestKeys.testPublicKey
    }

    "will reject a key that is not correctly formatted" in {
      whenReady(PublicSettings.extractPublicKey(Map("publicKey" -> "improperly formatted public key!!")).failed) { e =>
        e shouldBe a [PublicKeyFormatException.type]
      }
    }

    "will fail if the key is not present in the settings" in {
      whenReady(PublicSettings.extractPublicKey(Map("another key" -> "bar")).failed) { e =>
        e shouldBe a [PublicKeyNotFoundException.type]
      }
    }
  }

  "extractSettings" - {
    "extracts properties from a valid body" in {
      val body =
        """key=value
          |foo=bar
        """.stripMargin
      PublicSettings.extractSettings(Right(body)).futureValue shouldEqual Map("key" -> "value", "foo" -> "bar")
    }

    "handles a provided failure" in {
      val exception = new IOException
      whenReady(PublicSettings.extractSettings(Left(exception)).failed) { err =>
        err shouldBe a [PublicSettingsAcquisitionException]
        err.getCause shouldEqual exception
      }
    }
  }
}
