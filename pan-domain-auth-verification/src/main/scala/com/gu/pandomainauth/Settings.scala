package com.gu.pandomainauth

import java.io.ByteArrayInputStream
import java.util.Properties

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.util.IOUtils
import org.slf4j.Logger

import scala.util.control.NonFatal
import scala.jdk.CollectionConverters._

sealed trait SettingsFailure
case class SettingsDownloadFailure(cause: Throwable) extends SettingsFailure
case class SettingsParseFailure(cause: Throwable) extends SettingsFailure
case object PublicKeyFormatFailure extends SettingsFailure
case object PublicKeyNotFoundFailure extends SettingsFailure

object Settings {
  // internal functions for fetching and parsing the responses
  def fetchSettings(settingsFileKey: String, bucketName: String, s3Client: AmazonS3): Either[SettingsFailure, String] = try {
    val response = s3Client.getObject(bucketName, settingsFileKey)
    Right(IOUtils.toString(response.getObjectContent))
  } catch {
    case NonFatal(e) =>
      Left(SettingsDownloadFailure(e))
  }

  private[pandomainauth] def extractSettings(settingsBody: String): Either[SettingsFailure, Map[String, String]] = try {
    val props = new Properties()
    props.load(new ByteArrayInputStream(settingsBody.getBytes("UTF-8")))

    Right(props.asScala.toMap)
  } catch {
    case NonFatal(e) =>
      Left(SettingsParseFailure(e))
  }

  def logError(failure: SettingsFailure, logger: Logger) = failure match {
    case SettingsDownloadFailure(cause) =>
      logger.error("Unable to download public key", cause)

    case SettingsParseFailure(cause) =>
      logger.error("Unable to parse public key", cause)

    case PublicKeyFormatFailure =>
      logger.error("Public key does not match expected format")

    case PublicKeyNotFoundFailure =>
      logger.error("Public key not found in settings file")
  }

  def errorToThrowable(failure: SettingsFailure): Throwable = failure match {
    case SettingsDownloadFailure(cause) =>
      new IllegalStateException("Unable to download public key", cause)

    case SettingsParseFailure(cause) =>
      new IllegalStateException("Unable to parse public key", cause)

    case PublicKeyFormatFailure =>
      new IllegalStateException("Public key does not match expected format")

    case PublicKeyNotFoundFailure =>
      new IllegalStateException("Public key not found in settings file")
  }
}
