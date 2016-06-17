package com.gu.pandomainauth

import java.io.ByteArrayInputStream
import java.util.Properties

import akka.agent.Agent
import com.gu.pandomainauth.PublicSettings.FunctionJob
import dispatch._
import org.quartz._
import org.quartz.impl.StdSchedulerFactory

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * Class that contains the static public settings and includes mechanism for fetching the public key. The class is
 * parametrized by domain because the keys will differ for different domains. It also requires a dispatch.Http
 * instance for retrieving the data and an execution context.
 *
 * Once you have an instance you can call its start method to kick off loading the public data. In a Play app,
 * consider using the `Global` object's `onStart` method to start these calls when the application comes up. If
 * you'd rather use your own scheduler you can do so while still using the PublicSettings class by scheduling
 * your own calls to its `refresh` method can also schdule the refresh yourself using the instance's
 *
 * @param domain    The domain you would like to fetch settings for
 * @param callback  Optionally, a callback called when the data gets fetched, provided to allow you to perform logging
 * @param scheduler Optionally, the quartz scheduler instance to use. It defaults to the default scheduler but
 *                  customising it may be useful if you want more control over the scheduler's lifecycle
 * @param client    Implicit instance of dispatch.Http used to make the call to fetch the public settings
 * @param ec        Implicit execution context used to fetch the settings
 */
class PublicSettings(domain: String, callback: Try[Map[String, String]] => Unit = _ => (), scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler())
                    (implicit client: Http, ec: ExecutionContext) {

  private val agent = Agent[Map[String, String]](Map.empty)
  private val job = JobBuilder.newJob(classOf[FunctionJob])
    .withIdentity(s"refresh-public-key-$domain")
    .build

  def start(intervalInSeconds: Int = 60) = {
    // create job shedule
    val schedule = SimpleScheduleBuilder.simpleSchedule
      .withIntervalInSeconds(intervalInSeconds)
      .repeatForever()
    val trigger = TriggerBuilder.newTrigger()
      .withSchedule(schedule)
      .build
    // add this job's action to the global map of currently running jobs
    PublicSettings.jobs.put(job.getKey, () => refresh())
    // shedule the job (stopping current one if it is already there)
    if (scheduler.checkExists(job.getKey)) {
      scheduler.deleteJob(job.getKey)
    }
    scheduler.scheduleJob(job, trigger)
    // ensure the scheduler is running
    scheduler.start()
  }
  def stop() = {
    // remove job from map of current jobs and deschedule
    PublicSettings.jobs.remove(job.getKey)
    scheduler.deleteJob(job.getKey)
  }
  def refresh() = {
    val publicFuture = PublicSettings.getPublicSettings(domain)
    publicFuture
      .onComplete(callback)
    publicFuture
      .foreach(settings => agent.send(settings))
  }

  def publicKey = agent.get().get("publicKey")
  val bucketName = PublicSettings.bucketName
  val cookieName = PublicSettings.cookieName
  val assymCookieName = PublicSettings.assymCookieName
}

/**
 * Static PublicSettings for applications that do not want to use the provided mechanism for auto-refreshing
 * public data.
 */
object PublicSettings {
  val bucketName = sys.env.getOrElse("PANDA_BUCKET_NAME", "pan-domain-auth-settings")
  val cookieName = "gutoolsAuth"
  val assymCookieName = s"$cookieName-assym"

  def getPublicSettings(domain: String)(implicit client: Http, ec: ExecutionContext): Future[Map[String, String]] = {
    fetchSettings(domain) flatMap extractSettings
  }

  /**
   * Fetches the public key from the public S3 bucket
   *
   * @param domain the domain to fetch the public key for
   * @param client implicit dispatch.Http to use for fetching the key
   * @param ec     implicit execution context to use for fetching the key
   */
  def getPublicKey(domain: String)(implicit client: Http, ec: ExecutionContext): Future[PublicKey] = {
    getPublicSettings(domain) flatMap extractPublicKey
  }

  // internal functions for fetching and parsing the responses
  private def fetchSettings(domain: String)(implicit client: Http, ec: ExecutionContext): Future[Either[Throwable, String]] = {
    val req = host("s3-eu-west-1.amazonaws.com").secure / bucketName / s"$domain.settings.public"
    client(req OK as.String).either
  }
  private[pandomainauth] def extractSettings(settingsAttempt: Either[Throwable, String]): Future[Map[String, String]] =
    settingsAttempt match {
      case Right(settingsBody) =>
        val props = new Properties()
        props.load(new ByteArrayInputStream(settingsBody.getBytes("UTF-8")))
        Future.successful(props.toMap)
      case Left(err) =>
        Future.failed(new PublicSettingsAcquisitionException(err))
    }
  private[pandomainauth] def extractPublicKey(settings: Map[String, String]): Future[PublicKey] = {
    (for {
      rawKey <- settings.get("publicKey").toRight(PublicKeyNotFoundException).right
      publicKey <- validateKey(PublicKey(rawKey)).right
    } yield publicKey) match {
      case Right(publicKey) => Future.successful(publicKey)
      case Left(err) => Future.failed(err)
    }
  }
  private[pandomainauth] def validateKey(pubKey: PublicKey): Either[Throwable, PublicKey] = {
    if ("[a-zA-Z0-9+/\n]+={0,3}".r.pattern.matcher(pubKey.key).matches) Right(pubKey)
    else Left(PublicKeyFormatException)
  }

  class PublicSettingsAcquisitionException(cause: Throwable) extends Exception(cause.getMessage, cause)
  object PublicKeyFormatException extends Exception("Invalid public key")
  object PublicKeyNotFoundException extends Exception("Public key not found")

  // globally accessible state for the scheduler
  private val jobs = mutable.Map[JobKey, () => Unit]()
  class FunctionJob extends Job {
    def execute(context: JobExecutionContext) {
      val f = jobs(context.getJobDetail.getKey)
      f()
    }
  }
}
