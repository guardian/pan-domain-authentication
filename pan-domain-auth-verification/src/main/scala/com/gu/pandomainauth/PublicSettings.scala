package com.gu.pandomainauth

import com.amazonaws.services.s3.AmazonS3
import com.gu.pandomainauth.Settings.Loader
import com.gu.pandomainauth.SettingsFailure.SettingsResult
import com.gu.pandomainauth.service.CryptoConf

import java.security.PublicKey
import java.util.concurrent.Executors.newScheduledThreadPool
import java.util.concurrent.{Executors, ScheduledExecutorService}
import scala.concurrent.duration._

/**
 * Class that contains the static public settings and includes mechanism for fetching the public key. Once you have an
 * instance, call the `start()` method to load the public data.
 */
class PublicSettings(loader: Settings.Loader, scheduler: ScheduledExecutorService) {

  def this(settingsFileKey: String, bucketName: String, s3Client: AmazonS3,
    scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)) = this(
    new Settings.Loader(S3BucketLoader.forAwsSdkV1(s3Client, bucketName), settingsFileKey), scheduler
  )

  private val settingsRefresher = new Settings.Refresher[PublicKey](
    loader,
    CryptoConf.SettingsReader(_).activePublicKey,
    scheduler
  )

  def start(interval: FiniteDuration = 60.seconds): Unit = settingsRefresher.start(interval.toMinutes.toInt)

  def publicKey: PublicKey = settingsRefresher.get()
}

/**
 * Static PublicSettings for applications that do not want to use the provided mechanism for auto-refreshing
 * public data.
 */
object PublicSettings {

  def apply(loader: Settings.Loader): PublicSettings = new PublicSettings(loader, newScheduledThreadPool(1))

  /**
   * Fetches the public key from the public S3 bucket
   *
   * @param domain the domain to fetch the public key for
   */
  def getPublicKey(loader: Loader): SettingsResult[PublicKey] =
    loader.loadAndParseSettingsMap().flatMap(CryptoConf.SettingsReader(_).activePublicKey)
}
