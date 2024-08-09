package com.gu.pandomainauth.service

import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.security._


object Crypto {
  /**
   * You can generate a das key pair as follows:
   *
   * openssl genrsa -out private_key.pem 4096
   * openssl rsa -pubout -in private_key.pem -out public_key.pem
   *
   * Note: you only need to pass the key ie the blob of base64 between the start and end markers in the pem file.
   */

  Security.addProvider(new BouncyCastleProvider())

  val signatureAlgorithm: String = "SHA256withRSA"
  val keyFactory = KeyFactory.getInstance("RSA")

  def signData(data: Array[Byte], prvKey: PrivateKey): Array[Byte] = {
    val rsa = Signature.getInstance(signatureAlgorithm, "BC")
    rsa.initSign(prvKey)

    rsa.update(data)
    rsa.sign()
  }

  def verifySignature(data: Array[Byte], signature: Array[Byte], pubKey: PublicKey) : Boolean = {
    val rsa = Signature.getInstance(signatureAlgorithm, "BC")
    rsa.initVerify(pubKey)

    rsa.update(data)
    rsa.verify(signature)
  }
}
