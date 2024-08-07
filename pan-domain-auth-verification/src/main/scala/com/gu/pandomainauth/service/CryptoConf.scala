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
  trait Signing {
    val activePrivateKey: PrivateKey
  }

  trait Verification {
    val activePublicKey: PublicKey
    val alsoAccepted: Seq[PublicKey]

    val acceptedPublicKeys: Stream[PublicKey] = Stream(activePublicKey) ++ alsoAccepted

    def decode[A](f: PublicKey => Option[A]): Option[A] = acceptedPublicKeys.flatMap(f(_)).headOption
  }

  case class SigningAndVerification(activeKeyPair: KeyPair, alsoAccepted: Seq[PublicKey]) extends Signing with Verification {
    val activePublicKey: PublicKey = activeKeyPair.publicKey
    val activePrivateKey: PrivateKey = activeKeyPair.privateKey
  }

  case class OnlyVerification(activePublicKey: PublicKey, alsoAccepted: Seq[PublicKey] = Seq.empty) extends Verification

  case class SettingsReader(settingMap: Map[String,String]) {
    def setting(key: String): SettingsResult[String] = settingMap.get(key).toRight(MissingSetting(key))

    def signingAndVerificationConf: SettingsResult[SigningAndVerification] = makeConfWith(activeKeyPair)(SigningAndVerification)
    def verificationConf: SettingsResult[Verification] = makeConfWith(activePublicKey)(OnlyVerification)

    val activePublicKey: SettingsResult[PublicKey] = setting("publicKey").flatMap(publicKeyFor)

    private val alsoAcceptedPublicKeys: SettingsResult[Seq[PublicKey]] = settingMap.collect {
      case (k, v) if k.startsWith("alsoAccept.") && k.endsWith(".publicKey") => publicKeyFor(v)
    }.toSeq.sequence

    private def activeKeyPair: SettingsResult[KeyPair] = for {
      publicKey <- activePublicKey
      privateKey <- setting("privateKey").flatMap(privateKeyFor)
    } yield KeyPair(publicKey, privateKey)

    private def makeConfWith[A, T](activePartResult: SettingsResult[A])(createConf: (A, Seq[PublicKey]) => T): SettingsResult[T] = for {
      activePart <- activePartResult
      alsoAccepted <- alsoAcceptedPublicKeys
    } yield createConf(activePart, alsoAccepted)
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

  object Change {
    def compare(oldConf: SigningAndVerification, newConf: SigningAndVerification): Option[CryptoConf.Change] =
      if (newConf == oldConf) None else Some(Change(
        activeKey = if (newConf.activeKeyPair == oldConf.activeKeyPair) None else Some(ActiveKey(
          toleratingOldKey = newConf.alsoAccepted.contains(oldConf.activeKeyPair.publicKey),
          newKeyAlreadyAccepted = oldConf.alsoAccepted.contains(newConf.activeKeyPair.publicKey)
        )),
        SeqDiff.compare(oldConf.alsoAccepted, newConf.alsoAccepted)
      ))

    case class ActiveKey(toleratingOldKey: Boolean, newKeyAlreadyAccepted: Boolean) {
      val isBreakingChange: Boolean = !(toleratingOldKey && newKeyAlreadyAccepted)
      val summary: String = s"Active key changed: ${if (isBreakingChange) s"BREAKING - old-tolerated=$toleratingOldKey new-already-accepted=$newKeyAlreadyAccepted" else "non-breaking"}"
    }
  }

  case class Change(activeKey: Option[Change.ActiveKey], acceptedKeys: SeqDiff[PublicKey]) {
    val isBreakingChange: Boolean = activeKey.exists(_.isBreakingChange)
    val summary: String = (activeKey.map(_.summary).toSeq :+ s"acceptedKeys: ${acceptedKeys.summary}").mkString(" ")
  }

  case class SeqDiff[T](added: Seq[T], removed: Seq[T]) {
    val summary: String = s"added ${added.size}, removed ${removed.size}"
  }
  object SeqDiff {
    def compare[T](oldItems: Seq[T], newItems: Seq[T]): SeqDiff[T] =
      SeqDiff(added = newItems.diff(oldItems), removed = oldItems.diff(newItems))
  }
}