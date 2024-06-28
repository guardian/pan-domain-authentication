package com.gu.pandomainauth.service

import com.gu.pandomainauth.service.Crypto.{privateKeyFor, publicKeyFor}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers


class CryptoTest extends AnyFunSuite with Matchers {
  import TestKeys._

  test("a valid signature can be successfully verified") {
    val data = "Example payload".getBytes("UTF-8")
    val signature = Crypto.signData(data, testPrivateKey.key)
    Crypto.verifySignature(data, signature, testPublicKey.key) shouldEqual true
  }

  test("an invalid signature will not be verified") {
    val data = "Example payload".getBytes("UTF-8")
    Crypto.verifySignature(data, "not a valid signature".getBytes("UTF-8"), testPublicKey.key) shouldEqual false
  }

  test("a valid signature created with the wrong key will not be verified") {
    val data = "Example payload".getBytes("UTF-8")
    val signature = Crypto.signData(data, testINCORRECTPrivateKey.key)
    Crypto.verifySignature(data, signature, testPublicKey.key) shouldEqual false
  }
}
