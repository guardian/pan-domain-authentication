package com.gu.pandomainauth

import java.util.concurrent.atomic.AtomicReference

import com.amazonaws.auth.{AWSCredentialsProvider, DefaultAWSCredentialsProviderChain}
import com.amazonaws.regions.{Region, Regions}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.{Actor, ActorSystem, Props}
import akka.event.Logging
import com.gu.pandomainauth.model.PanDomainAuthSettings
import com.gu.pandomainauth.service.{ProxyConfiguration, S3Bucket}

import scala.concurrent.duration.FiniteDuration

/**
  * PanDomainAuthSettingsRefresher will periodically refresh the pan domain settings and expose them via the "settings" method.
  *
  * This is effectively a disguised singleton with a state
  *
  * @param domain the domain you are "authing" against
  * @param system the identifier for your app, typically the same as the subdomain your app runs on
  * @param actorSystem the actor system to create the refresh actor
  * @param awsCredentialsProvider optional credential provider
  * @param awsRegion optional region
  * @param proxyConfiguration optional proxy configuration
  */
class PanDomainAuthSettingsRefresher(
  val domain: String,
  val system: String,
  actorSystem: ActorSystem,
  awsCredentialsProvider: AWSCredentialsProvider = new DefaultAWSCredentialsProviderChain(),
  awsRegion: Option[Region] = Option(Region getRegion Regions.EU_WEST_1),
  proxyConfiguration: Option[ProxyConfiguration] = None
) {
  lazy val bucket = new S3Bucket(awsCredentialsProvider, awsRegion, proxyConfiguration)

  private lazy val settingsMap = bucket.readDomainSettings(domain)
  private lazy val authSettings: AtomicReference[PanDomainAuthSettings] = new AtomicReference(PanDomainAuthSettings(settingsMap))

  private lazy val domainSettingsRefreshActor = actorSystem.actorOf(Props(classOf[DomainSettingsRefreshActor], domain, bucket, authSettings), "PanDomainAuthSettingsRefresher")

  actorSystem.scheduler.scheduleOnce(1 minute, domainSettingsRefreshActor, Refresh)

  def settings = authSettings.get()
}

class DomainSettingsRefreshActor(domain: String, bucket: S3Bucket, authSettings: AtomicReference[PanDomainAuthSettings]) extends Actor {

  val frequency: FiniteDuration = 1 minute
  val log = Logging(context.system, this)

  override def receive: Receive = {
    case Refresh => {
      try {
        val settingsMap = bucket.readDomainSettings(domain)

        val settings = PanDomainAuthSettings(settingsMap)

        authSettings.set(settings)
        log.debug("reloaded settings for {}", domain)
      } catch {
        case e: Exception => log.error(e, "failed to refresh domain {} settings", domain)
      }
      reschedule
    }
  }

  override def postRestart(reason: Throwable) {
    reschedule
  }

  def reschedule {
    context.system.scheduler.scheduleOnce(frequency, self, Refresh)
  }
}

case object Refresh




