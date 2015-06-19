package com.gu.pandomainauth

import com.gu.pandomainauth.PublicSettings.PublicKeyFormatException
import com.gu.pandomainauth.service.TestKeys
import org.scalatest.{EitherValues, Matchers, FreeSpec}

class PublicSettingsTest extends FreeSpec with Matchers with EitherValues {
  "validateKey" - {
    "returns an error if the key looks invalid" in {
      val invalidKey = "not a valid key"
      PublicSettings.validateKey(invalidKey).left.value shouldEqual PublicKeyFormatException
    }

    "returns an error if we have an S3 error instead of a key value" in {
      val invalidKey = """<?xml version="1.0" encoding="UTF-8"?>
                         |<Error><Code>AccessDenied</Code><Message>Access Denied</Message><RequestId>5E5E15297AEDE946</RequestId><HostId>QmRYsD7HQmq7GtKC7CC9rZsv0MC/nWrppQQrAoMSNWku2ySkox5TlFIF8hk5wAUETeUa3xG9Jo4=</HostId></Error>"""
      PublicSettings.validateKey(invalidKey).left.value shouldEqual PublicKeyFormatException
    }

    "returns the key if it is valid" in {
      val key = TestKeys.testPublicKey
      PublicSettings.validateKey(key).right.value shouldEqual key
    }
  }
}
