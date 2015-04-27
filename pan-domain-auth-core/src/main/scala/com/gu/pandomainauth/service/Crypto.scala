package com.gu.pandomainauth.service

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, Security, Signature}

import org.apache.commons.codec.binary.Base64._
import org.bouncycastle.jce.provider.BouncyCastleProvider


object Crypto {
  /**
   * You can generate a das key pair as follows:
   *
   * openssl dsaparam -genkey -noout 2048 | openssl pkcs8 -topk8 -nocrypt > priv.pem
   * openssl dsa -in priv.pem -pubout > pub.pem
   *
   * Note: you only need to pass the key ie the blob of base64 between the start and end markers in the pem file.
   */

  Security.addProvider(new BouncyCastleProvider())

  val signatureAlgorithm: String = "SHA256withDSA"
  val keyFactory = KeyFactory.getInstance("DSA")

  def getSignature(data: Array[Byte], prvKeyStr: String): Array[Byte] = {
    val dsa = Signature.getInstance(signatureAlgorithm, "BC")
    val privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decodeBase64(prvKeyStr)))
    dsa.initSign(privateKey)

    dsa.update(data)
    dsa.sign()
  }

  def verifySignature(data: Array[Byte], signature: Array[Byte], pubKeyStr: String) : Boolean = {
    val dsa = Signature.getInstance(signatureAlgorithm, "BC")
    val publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodeBase64(pubKeyStr)))
    dsa.initVerify(publicKey)

    dsa.update(data)
    dsa.verify(signature)
  }
}
