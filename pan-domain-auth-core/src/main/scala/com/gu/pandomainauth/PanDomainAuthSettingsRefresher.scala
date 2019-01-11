package com.gu.pandomainauth

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import com.amazonaws.services.s3.AmazonS3
import com.gu.pandomainauth.model.PanDomainAuthSettings
import org.slf4j.LoggerFactory

import scala.language.postfixOps

/**
  * PanDomainAuthSettingsRefresher will periodically refresh the pan domain settings and expose them via the "settings" method
  *
  * @param domain the domain you are authenticating against
  * @param system the identifier for your app, typically the same as the subdomain your app runs on
  * @param bucketName the bucket where the settings are stored
  * @param settingsFileKey the name of the file that contains the private settings for the given domain
  * @param s3Client the AWS S3 client that will be used to download the settings from the bucket
  * @param scheduler optional scheduler that will be used to run the code that updates the bucket
  */
class PanDomainAuthSettingsRefresher(
  val domain: String,
  val system: String,
  val bucketName: String,
  settingsFileKey: String,
  val s3Client: AmazonS3,
  scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  // This is deliberately designed to throw an exception during construction if we cannot immediately read the settings
  private val authSettings: AtomicReference[PanDomainAuthSettings] = new AtomicReference[PanDomainAuthSettings](loadSettings() match {
    case Right(settings) => PanDomainAuthSettings(settings)
    case Left(err) => throw Settings.errorToThrowable(err)
  })

  scheduler.scheduleAtFixedRate(() => refresh(), 1, 1, TimeUnit.MINUTES)

  def settings: PanDomainAuthSettings = authSettings.get()

  private def loadSettings(): Either[SettingsFailure, Map[String, String]] = {
    Settings.fetchSettings(settingsFileKey, bucketName, s3Client).flatMap(Settings.extractSettings)
  }

  private def refresh(): Unit = {
    loadSettings() match {
      case Right(settings) =>
        logger.info(s"Updated pan-domain settings for $domain")
        authSettings.set(PanDomainAuthSettings(settings))

      case Left(err) =>
        logger.error(s"Failed to update pan-domain settings for $domain")
        Settings.logError(err, logger)
    }
  }
}




