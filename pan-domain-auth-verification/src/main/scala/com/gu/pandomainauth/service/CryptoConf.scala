package com.gu.pandomainauth.service

import com.gu.pandomainauth.Settings._
import com.gu.pandomainauth.internal.{KeyHashId, NonActiveKeyMonitoring}
import com.gu.pandomainauth.service.Crypto.keyFactory
import com.gu.pandomainauth.service.CryptoConf.Change.ActiveKey.TransitionCriteria
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

    lazy val acceptedPublicKeys: LazyList[PublicKey] = LazyList(activePublicKey) ++ alsoAccepted

    private[CryptoConf] lazy val activeKeyId = KeyHashId.calculateFor(activePublicKey)

    private lazy val acceptedKeysWithIds: LazyList[(PublicKey, KeyHashId)] =
      acceptedPublicKeys.map(key => key -> KeyHashId.calculateFor(key))

    private[CryptoConf] lazy val alsoAcceptedKeyIds: Seq[KeyHashId] = alsoAccepted.map(KeyHashId.calculateFor)

    private[CryptoConf] def acceptsActiveKeyFrom(other: Verification): Boolean = acceptedPublicKeys.contains(other.activePublicKey)

    def decode[A](f: PublicKey => Option[A]): Option[A] = {
      (for {
        (key, keyId) <- acceptedKeysWithIds
        result <- f(key)
      } yield {
        NonActiveKeyMonitoring.instance.monitor(keyId, activeKeyId)
        result
      }).headOption
    }
  }

  case class SigningAndVerification(activeKeyPair: KeyPair, alsoAccepted: Seq[PublicKey]) extends Signing with Verification {
    val activePublicKey: PublicKey = activeKeyPair.publicKey
    val activePrivateKey: PrivateKey = activeKeyPair.privateKey
  }

  case class OnlyVerification(activePublicKey: PublicKey, alsoAccepted: Seq[PublicKey] = Seq.empty) extends Verification

  case class SettingsReader(settingMap: Map[String,String]) {
    def setting(key: String): SettingsResult[String] = settingMap.get(key).toRight(MissingSetting(key))

    def signingAndVerificationConf: SettingsResult[SigningAndVerification] = makeConfWith(activeKeyPair)(SigningAndVerification(_, _))
    def verificationConf: SettingsResult[Verification] = makeConfWith(activePublicKey)(OnlyVerification(_, _))

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
    def compare(oldConf: Verification, newConf: Verification): Option[CryptoConf.Change] =
      Option.when(newConf != oldConf)(Change(
        activeKey = Option.when(newConf.activePublicKey != oldConf.activePublicKey)(ActiveKey(
          oldConf.activeKeyId,
          newConf.activeKeyId,
          TransitionCriteria.failedCriteriaFor(oldConf, newConf)
        )),
        newConf.alsoAcceptedKeyIds, SeqDiff.compare(oldConf.alsoAcceptedKeyIds, newConf.alsoAcceptedKeyIds)
      ))

    /**
     * CryptoConf.Change.ActiveKey details the consequences of a change to the active key,
     * allowing us to know if the change could disrupt existing user sessions.
     */
    case class ActiveKey(oldId: KeyHashId, newId: KeyHashId, failedTransitionCriteria: Seq[TransitionCriteria]) {
      val isBreakingChange: Boolean = failedTransitionCriteria.nonEmpty
      val summary: String = s"Active key changed from $oldId to $newId${if (isBreakingChange) s" (FAILED transition criteria: ${failedTransitionCriteria.mkString(", ")})" else ""}."
    }

    object ActiveKey {
      sealed trait TransitionCriteria {
        def passes(oldConf: Verification, newConf: Verification): Boolean
      }
      case object TolerateOldKey extends TransitionCriteria {
        override def passes(oldConf: Verification, newConf: Verification): Boolean = newConf.acceptsActiveKeyFrom(oldConf)
      }
      case object PreAcceptNewKey extends TransitionCriteria {
        override def passes(oldConf: Verification, newConf: Verification): Boolean = oldConf.acceptsActiveKeyFrom(newConf)
      }
      object TransitionCriteria {
        val All: Seq[TransitionCriteria] = Seq(TolerateOldKey, PreAcceptNewKey)
        def failedCriteriaFor(oldConf: Verification, newConf: Verification): Seq[TransitionCriteria] =
          All.filterNot(_.passes(oldConf, newConf))
      }
    }
  }

  /**
   * CryptoConf.Change denotes that there's been a change to the crypto settings. If the active key
   * has changed, we'll have a CryptoConf.Change.ActiveKey detailing if the update is safe.
   */
  case class Change(activeKey: Option[Change.ActiveKey], alsoAcceptedKeys: Seq[KeyHashId], alsoAcceptedKeysDiff: SeqDiff[KeyHashId]) {
    val isBreakingChange: Boolean = activeKey.exists(_.isBreakingChange)
    val summary: String = (activeKey.map(_.summary).toSeq :+ s"alsoAcceptedKeys: ${conciseList(alsoAcceptedKeys)} (${alsoAcceptedKeysDiff.summary})").mkString(" ")
  }

  private def conciseList[T](s: Seq[T]): String = s.mkString("[", ",", "]")

  case class SeqDiff[T](added: Seq[T], removed: Seq[T]) {
    val summary: String = s"added ${conciseList(added)}, removed ${conciseList(removed)}"
  }
  object SeqDiff {
    def compare[T](oldItems: Seq[T], newItems: Seq[T]): SeqDiff[T] =
      SeqDiff(added = newItems.diff(oldItems), removed = oldItems.diff(newItems))
  }
}