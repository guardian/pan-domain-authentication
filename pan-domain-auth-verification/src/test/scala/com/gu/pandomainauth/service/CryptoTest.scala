package com.gu.pandomainauth.service

import java.security.SignatureException

import org.scalatest.{FunSuite, Matchers}


class CryptoTest extends FunSuite with Matchers {
  import TestKeys._

  test("a valid signature can be successfully verified") {
    val data = "Example payload".getBytes("UTF-8")
    val signature = Crypto.getSignature(data, testPrivateKey)
    Crypto.verifySignature(data, signature, testPublicKey) should equal(true)
  }

  test("an invalid signature will not be verified") {
    val data = "Example payload".getBytes("UTF-8")
    intercept[SignatureException] {
      Crypto.verifySignature(data, "not a valid signature".getBytes("UTF-8"), testPublicKey) should equal(false)
    }
  }

  test("a valid but incorrect signature will not be verified") {
    val data = "Example payload".getBytes("UTF-8")
    val signature = Crypto.getSignature(data, testINCORRECTPrivateKey)
    Crypto.verifySignature(data, signature, testPublicKey) should equal(false)
  }
}
