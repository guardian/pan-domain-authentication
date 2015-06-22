package com.gu.pandomainauth.service

import org.scalatest.{FunSuite, Matchers}


class CryptoTest extends FunSuite with Matchers {
  import TestKeys._

  test("a valid signature can be successfully verified") {
    val data = "Example payload".getBytes("UTF-8")
    val signature = Crypto.signData(data, testPrivateKey)
    Crypto.verifySignature(data, signature, testPublicKey) should equal(true)
  }

  test("an invalid signature will not be verified") {
    val data = "Example payload".getBytes("UTF-8")
    Crypto.verifySignature(data, "not a valid signature".getBytes("UTF-8"), testPublicKey) should equal(false)
  }

  test("a valid signature created with the wrong key will not be verified") {
    val data = "Example payload".getBytes("UTF-8")
    val signature = Crypto.signData(data, testINCORRECTPrivateKey)
    Crypto.verifySignature(data, signature, testPublicKey) should equal(false)
  }
}
