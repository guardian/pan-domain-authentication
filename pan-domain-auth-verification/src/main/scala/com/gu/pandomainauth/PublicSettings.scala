package com.gu.pandomainauth

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import com.amazonaws.services.s3.AmazonS3
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * Class that contains the static public settings and includes mechanism for fetching the public key. Once you have an
 * instance, call the `start()` method to load the public data.
  *
  * @param settingsFileKey the settings file for the domain in the S3 bucket (eg local.dev.gutools.co.uk.public.settings)
  * @param bucketName      the name of the S3 bucket (eg pan-domain-auth-settings)
  * @param s3Client        the AWS S3 client that will be used to download the settings from the bucket
  * @param scheduler       optional scheduler that will be used to run the code that updates the bucket
 */
class PublicSettings(settingsFileKey: String, bucketName: String, s3Client: AmazonS3,
                     scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)) {

  private val agent = new AtomicReference[Option[PublicKey]](None)

  private val logger = LoggerFactory.getLogger(this.getClass)
  implicit private val executionContext: ExecutionContext = ExecutionContext.fromExecutor(scheduler)

  def start(interval: FiniteDuration = 60.seconds): Unit = {
    scheduler.scheduleAtFixedRate(() => refresh(), 0, interval.toMillis, TimeUnit.MILLISECONDS)
  }

  def refresh(): Unit = {
    PublicSettings.getPublicKey(settingsFileKey, bucketName, s3Client) match {
      case Right(publicKey) =>
        agent.set(Some(publicKey))
        logger.info("Successfully updated pan-domain public settings")

      case Left(err) =>
        logger.error("Failed to update pan-domain public settings")
        Settings.logError(err, logger)
    }
  }

  def publicKey: Option[PublicKey] = agent.get()
}

/**
 * Static PublicSettings for applications that do not want to use the provided mechanism for auto-refreshing
 * public data.
 */
object PublicSettings {
  import Settings._

  /**
   * Fetches the public key from the public S3 bucket
   *
   * @param domain the domain to fetch the public key for
   * @param client implicit dispatch.Http to use for fetching the key
   * @param ec     implicit execution context to use for fetching the key
   */
  def getPublicKey(settingsFileKey: String, bucketName: String, s3Client: AmazonS3): Either[SettingsFailure, PublicKey] = {
    fetchSettings(settingsFileKey, bucketName, s3Client) flatMap extractSettings flatMap extractPublicKey
  }

  private[pandomainauth] def extractPublicKey(settings: Map[String, String]): Either[SettingsFailure, PublicKey] = for {
    rawKey <- settings.get("publicKey").toRight(PublicKeyNotFoundFailure).right
    publicKey <- validateKey(PublicKey(rawKey)).right
  } yield publicKey

  private[pandomainauth] def validateKey(pubKey: PublicKey): Either[SettingsFailure, PublicKey] = {
    if ("[a-zA-Z0-9+/\n]+={0,3}".r.pattern.matcher(pubKey.key).matches) Right(pubKey)
    else Left(PublicKeyFormatFailure)
  }
}
