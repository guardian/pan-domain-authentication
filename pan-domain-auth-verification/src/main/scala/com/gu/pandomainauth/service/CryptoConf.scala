package com.gu.pandomainauth.service

import com.gu.pandomainauth.SettingsFailure.SettingsResult
import com.gu.pandomainauth.service.Crypto.keyFactory
import com.gu.pandomainauth.service.CryptoConf.SettingsReader.{privateKeyFor, publicKeyFor}
import com.gu.pandomainauth.{InvalidBase64, MissingSetting, PublicKeyFormatFailure}
import org.apache.commons.codec.binary.Base64.{decodeBase64, isBase64}

import java.security.spec.{InvalidKeySpecException, PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{PrivateKey, PublicKey}
import scala.util.Try



object CryptoConf {
  case class SettingsReader(settingMap: Map[String,String]) {
    def setting(key: String): SettingsResult[String] = settingMap.get(key).toRight(MissingSetting(key))

    val activePublicKey: SettingsResult[PublicKey] = setting("publicKey").flatMap(publicKeyFor)

    def activeKeyPair: SettingsResult[KeyPair] = for {
      publicKey <- activePublicKey
      privateKey <- setting("privateKey").flatMap(privateKeyFor)
    } yield KeyPair(publicKey, privateKey)
  }

  object SettingsReader {
    def publicKeyFor(data: Array[Byte]) = keyFactory.generatePublic(new X509EncodedKeySpec(data))
    def privateKeyFor(data: Array[Byte]) = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(data))

    def bytesFromBase64(base64Encoded: String): SettingsResult[Array[Byte]] =
      Either.cond(isBase64(base64Encoded), decodeBase64(base64Encoded), InvalidBase64)

    private def keyFor[A](keyConstructor: Array[Byte] => A, base64EncodedKey: String): SettingsResult[A] = for {
      bytes <- bytesFromBase64(base64EncodedKey)
      key <- Try(keyConstructor(bytes)).map(Right(_)).recover {
        case _: InvalidKeySpecException => Left(PublicKeyFormatFailure)
      }.get
    } yield key

    def publicKeyFor(base64EncodedKey: String): SettingsResult[PublicKey] = keyFor(publicKeyFor, base64EncodedKey)
    def privateKeyFor(base64EncodedKey: String): SettingsResult[PrivateKey] = keyFor(privateKeyFor, base64EncodedKey)
  }
}