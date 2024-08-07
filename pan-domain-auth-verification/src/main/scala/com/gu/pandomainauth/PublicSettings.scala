package com.gu.pandomainauth

import com.amazonaws.services.s3.AmazonS3
import com.gu.pandomainauth.Settings.Loader
import com.gu.pandomainauth.SettingsFailure.SettingsResult
import com.gu.pandomainauth.service.CryptoConf
import com.gu.pandomainauth.service.CryptoConf.Verification

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

  private val settingsRefresher = new Settings.Refresher[Verification](
    loader,
    CryptoConf.SettingsReader(_).verificationConf,
    (o, n) => {
//      for (change <- CryptoConf.Change.compare(o, n)) {
//        val message = s"PanDomainAuthSettings have changed for $domain: ${change.summary}"
//        if (change.isBreakingChange) logger.warn(message) else logger.info(message)
//      }
    },
    scheduler
  )

  def start(interval: FiniteDuration = 60.seconds): Unit = settingsRefresher.start(interval.toMinutes.toInt)

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
