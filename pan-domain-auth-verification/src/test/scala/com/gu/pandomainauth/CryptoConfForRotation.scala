package com.gu.pandomainauth

import com.gu.pandomainauth.service.CryptoConf.SigningAndVerification
import com.gu.pandomainauth.service.{CryptoConf, KeyPair}
import org.scalatest.EitherValues

import java.io.FileInputStream
import java.nio.file.Files
import java.security.{Key, KeyPairGenerator}
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.temporal.ChronoUnit.SECONDS
import java.util.Base64

/**
 * This class can be run from the sbt console with `pan-domain-auth-verification / Test / run`.
 *
 * You need to supply the _current_ Panda .settings file as the command line argument, eg:
 *
 * {{{
 * pan-domain-auth-verification / Test / run pan-domain-auth-verification/src/test/resources/crypto-conf-rotation-example/0.legacy.
 * settings
 * }}}
 */
object CryptoConfForRotation extends App with EitherValues {

  val base64Encoder = Base64.getEncoder

  args.headOption.fold(
    Console.err.println("\nYou must supply the path to the current Panda .settings file, downloaded from S3.\n")
  )(generateForExistingConf)

  def generateForExistingConf(pathForCurrentConf: String): Unit = {
    val keyPairGenerator: KeyPairGenerator = {
      val g = KeyPairGenerator.getInstance("RSA")
      g.initialize(4096)
      g
    }

    val newTargetKeyPair = {
      val kp = keyPairGenerator.generateKeyPair
      KeyPair(kp.getPublic, kp.getPrivate)
    }

    println(s"Loading current conf from $pathForCurrentConf")
    val loader = new Settings.Loader(
      _ => new FileInputStream(pathForCurrentConf),
      ""
    )
    val originalConf = CryptoConf.SettingsReader(loader.loadAndParseSettingsMap().value).signingAndVerificationConf.value
    val rotationInProgressConf = originalConf.copy(activeKeyPair = newTargetKeyPair, alsoAccepted = Seq(originalConf.activePublicKey))

    val tempDirWithPrefix = Files.createTempDirectory("panda-rotation")
    val timeStamp = s"Generated at ${ZonedDateTime.now(UTC).truncatedTo(SECONDS).format(ISO_LOCAL_DATE_TIME)}Z"
    println(s"$timeStamp - files are in:\n\n$tempDirWithPrefix\n")
    for {
      ((description, conf), index) <- Seq(
        "upcoming" -> originalConf.copy(alsoAccepted = Seq(newTargetKeyPair.publicKey)),
        "in-progress" -> rotationInProgressConf,
        "complete" -> rotationInProgressConf.copy(alsoAccepted = Nil)
      ).zipWithIndex
    } {
      val filename = s"${1 + index}.rotation-$description.settings"
      println(filename)
      Files.writeString(tempDirWithPrefix.resolve(filename), s"# $filename - $timeStamp\n\n${textFor(conf)}")
    }
  }

  def textFor(conf: SigningAndVerification): String = {
    def base64For(key: Key): String = base64Encoder.encodeToString(key.getEncoded)

    val keyValues = Seq(
      "privateKey" -> base64For(conf.activePrivateKey),
      "publicKey" -> base64For(conf.activePublicKey)
    ) ++ conf.alsoAccepted.zipWithIndex.map {
      case (key, index) => s"alsoAccept.$index.publicKey" -> base64For(key)
    }
    assert(CryptoConf.SettingsReader(keyValues.toMap).signingAndVerificationConf.value == conf)
    keyValues.map {
      case (key, value) =>  s"$key=$value\n"
    }.mkString("\n")
  }
}
