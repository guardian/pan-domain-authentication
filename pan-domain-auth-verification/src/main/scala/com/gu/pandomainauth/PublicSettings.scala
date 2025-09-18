package com.gu.pandomainauth

import software.amazon.awssdk.services.s3.S3Client
import com.gu.pandomainauth.Settings.{Loader, SettingsResult}
import com.gu.pandomainauth.service.CryptoConf
import com.gu.pandomainauth.service.CryptoConf.Verification

import java.security.PublicKey
import java.time.Duration
import java.time.Duration.ofMinutes
import java.util.concurrent.Executors.newScheduledThreadPool
import java.util.concurrent.{Executors, ScheduledExecutorService}

/**
 * Class that contains the static public settings and includes mechanism for fetching the public key. Once you have an
 * instance, call the `start()` method to load the public data.
 */
class PublicSettings(loader: Settings.Loader, scheduler: ScheduledExecutorService) {

  def this(settingsFileKey: String, bucketName: String, s3Client: S3Client,
    scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)) = this(
    new Settings.Loader(S3BucketLoader.forAwsSdkV2(s3Client, bucketName), settingsFileKey), scheduler
  )

  private val settingsRefresher = new Settings.Refresher[Verification](
    loader,
    CryptoConf.SettingsReader(_).verificationConf,
    identity,
    scheduler
  )

  def start(interval: Duration = ofMinutes(1)): Unit = settingsRefresher.start(interval)

  def verification: Verification = settingsRefresher.get()

  @deprecated("Use `verification` instead, to allow smooth transition to new public keys")
  def publicKey: PublicKey = verification.activePublicKey
}

/**
 * Static PublicSettings for applications that do not want to use the provided mechanism for auto-refreshing
 * public data.
 */
object PublicSettings {

  def apply(loader: Settings.Loader): PublicSettings = new PublicSettings(loader, newScheduledThreadPool(1))

  def getVerification(loader: Loader): SettingsResult[Verification] =
    loader.loadAndParseSettingsMap().flatMap(CryptoConf.SettingsReader(_).verificationConf)
}
