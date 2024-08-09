package com.gu.pandomainauth.service

import com.gu.pandomainauth.SettingsFailure.SettingsResult
import com.gu.pandomainauth.service.Crypto.keyFactory
import com.gu.pandomainauth.service.CryptoConf.SettingsReader.{privateKeyFor, publicKeyFor}
import com.gu.pandomainauth.{InvalidBase64, MissingSetting, PublicKeyFormatFailure}
import org.apache.commons.codec.binary.Base64.{decodeBase64, isBase64}

import java.security.spec.{InvalidKeySpecException, KeySpec, PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}
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
    private def bytesFromBase64(base64Encoded: String): SettingsResult[Array[Byte]] =
      Either.cond(isBase64(base64Encoded), decodeBase64(base64Encoded), InvalidBase64)

    private def keyFor[A](
      base64EncodedKey: String,
      keySpecFor: Array[Byte] => KeySpec,
      keyForSpec: KeyFactory => KeySpec => A
    ): SettingsResult[A] = for {
      bytes <- bytesFromBase64(base64EncodedKey)
      key <- Try(keyForSpec(keyFactory)(keySpecFor(bytes))).map(Right(_)).recover {
        case _: InvalidKeySpecException => Left(PublicKeyFormatFailure)
      }.get
    } yield key

    def publicKeyFor(base64Key: String): SettingsResult[PublicKey] =
      keyFor(base64Key, new X509EncodedKeySpec(_), _.generatePublic)
    def privateKeyFor(base64Key: String): SettingsResult[PrivateKey] =
      keyFor(base64Key, new PKCS8EncodedKeySpec(_), _.generatePrivate)
  }
}