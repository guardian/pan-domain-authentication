package com.gu.pandomainauth

import com.amazonaws.util.IOUtils
import com.gu.pandomainauth.SettingsFailure.SettingsResult
import com.gu.pandomainauth.service.CryptoConf
import com.gu.pandomainauth.service.CryptoConf.Verification
import org.slf4j.{Logger, LoggerFactory}

import java.io.ByteArrayInputStream
import java.util.Properties
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{Executors, ScheduledExecutorService}
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

sealed trait SettingsFailure {
  val description: String

  def logError(logger: Logger): Unit = logger.error(description)

  def asThrowable(): Throwable = new IllegalStateException(description)
}

trait FailureWithCause extends SettingsFailure {
  val cause: Throwable

  override def logError(logger: Logger): Unit = logger.error(description, cause)

  override def asThrowable(): Throwable = new IllegalStateException(description, cause)
}

case class SettingsDownloadFailure(cause: Throwable) extends FailureWithCause {
  override val description: String = "Unable to download public key"
}

case class MissingSetting(name: String) extends SettingsFailure {
  override val description: String = s"Key '$name' not found in settings file"
}

case class SettingsParseFailure(cause: Throwable) extends FailureWithCause {
  override val description: String = "Unable to parse public key"
}

case object PublicKeyFormatFailure extends SettingsFailure {
  override val description: String = "Public key does not match expected format"
}

case object InvalidBase64 extends SettingsFailure {
  override val description: String = "Settings file value for cryptographic key is not valid base64"
}

object SettingsFailure {
  type SettingsResult[A] = Either[SettingsFailure, A]

  implicit class RichSettingsResultSeq[A](result: Seq[SettingsResult[A]]) {
    def sequence: SettingsResult[Seq[A]] = result.foldLeft[SettingsResult[List[A]]](Right(Nil)) { // Easier with Cats!
      (acc, e) => for (keys <- acc; key <- e) yield key :: keys
    }
  }
}

object Settings {
  /**
   * @param settingsFileKey the name of the file that contains the private settings for the given domain
   */
  class Loader(s3BucketLoader: S3BucketLoader, settingsFileKey: String) {

    def loadAndParseSettingsMap(): SettingsResult[Map[String, String]] = fetchSettings().flatMap(extractSettings)

    private def fetchSettings(): SettingsResult[String] = try {
      Right(IOUtils.toString(s3BucketLoader.inputStreamFetching(settingsFileKey)))
    } catch { case NonFatal(e) => Left(SettingsDownloadFailure(e)) }
  }

  private[pandomainauth] def extractSettings(settingsBody: String): SettingsResult[Map[String, String]] = try {
    val props = new Properties()
    props.load(new ByteArrayInputStream(settingsBody.getBytes("UTF-8")))
    Right(props.asScala.toMap)
  } catch {
    case NonFatal(e) =>
      Left(SettingsParseFailure(e))
  }

  class Refresher[A](
    loader: Settings.Loader,
    settingsParser: Map[String, String] => SettingsResult[A],
    verificationIn: A => Verification,
    scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
  ) {
    // This is deliberately designed to throw an exception during construction if we cannot immediately read the settings
    private val store: AtomicReference[A] = new AtomicReference(
      loadAndParseSettings().fold(fail => throw fail.asThrowable(), identity)
    )

    private val logger = LoggerFactory.getLogger(getClass)

    def start(interval: Int): Unit = scheduler.scheduleAtFixedRate(() => refresh(), 0, interval, MINUTES)

    def loadAndParseSettings(): SettingsResult[A] =
      loader.loadAndParseSettingsMap().flatMap(settingsParser)

    private def refresh(): Unit = loadAndParseSettings() match {
      case Right(newSettings) =>
        val oldSettings = store.getAndSet(newSettings)
        for (change <- CryptoConf.Change.compare(verificationIn(oldSettings), verificationIn(newSettings))) {
          val message = s"Panda settings changed: ${change.summary}"
          if (change.isBreakingChange) logger.warn(message) else logger.info(message)
        }
      case Left(err) =>
        logger.error("Failed to update pan-domain settings for $domain")
        err.logError(logger)
    }

    def get(): A = store.get()
  }
}
