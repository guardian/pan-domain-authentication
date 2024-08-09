package com.gu.pandomainauth

import com.amazonaws.services.s3.AmazonS3
import com.gu.pandomainauth.model.PanDomainAuthSettings
import com.gu.pandomainauth.service.CryptoConf
import org.slf4j.LoggerFactory

import java.util.concurrent.Executors.newScheduledThreadPool
import java.util.concurrent.ScheduledExecutorService

/**
  * PanDomainAuthSettingsRefresher will periodically refresh the pan domain settings and expose them via the "settings" method
  *
  * To construct a PanDomainAuthSettingsRefresher, prefer the companion object's apply method, which uses
  * reasonable defaults.
  */
class PanDomainAuthSettingsRefresher(
  val domain: String,
  val system: String,
  val s3BucketLoader: S3BucketLoader,
  settingsFileKey: String,
  scheduler: ScheduledExecutorService
) {
  /**
   * This auxiliary constructor is a convenience for legacy code - it matches the constructor signature
   * used by earlier versions of this class. Prefer the companion object's apply method if you're writing new code.
   */
  def this(
    domain: String,
    system: String,
    bucketName: String,
    settingsFileKey: String,
    s3Client: AmazonS3,
    scheduler: ScheduledExecutorService = newScheduledThreadPool(1)
  ) = this(domain, system, S3BucketLoader.forAwsSdkV1(s3Client, bucketName), settingsFileKey, scheduler)

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val settingsRefresher = new Settings.Refresher[PanDomainAuthSettings](
    new Settings.Loader(s3BucketLoader, settingsFileKey),
    PanDomainAuthSettings.apply,
    (o, n) => {
      for (change <- CryptoConf.Change.compare(o.signingAndVerification, n.signingAndVerification)) {
        val message = s"PanDomainAuthSettings have changed for $domain: ${change.summary}"
        if (change.isBreakingChange) logger.warn(message) else logger.info(message)
      }
    },
    scheduler
  )
  settingsRefresher.start(1)

  def settings: PanDomainAuthSettings = settingsRefresher.get()
}

object PanDomainAuthSettingsRefresher {
  /**
   * Preferred constructor for PanDomainAuthSettingsRefresher, uses reasonable defaults.
   * 
   * @param domain the domain you are authenticating against (e.g. 'gutools.co.uk', 'local.dev-gutools.co.uk', etc)
   * @param system the identifier for your app, typically the same as the subdomain your app runs on
   */
  def apply(
    domain: String,
    system: String,
    s3BucketLoader: S3BucketLoader
  ): PanDomainAuthSettingsRefresher =
    new PanDomainAuthSettingsRefresher(domain, system, s3BucketLoader, s"$domain.settings", newScheduledThreadPool(1))
}