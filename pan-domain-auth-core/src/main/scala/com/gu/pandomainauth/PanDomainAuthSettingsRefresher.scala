package com.gu.pandomainauth

import com.amazonaws.auth.{DefaultAWSCredentialsProviderChain, AWSCredentialsProvider}
import com.amazonaws.regions.{Regions, Region}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.{Props, Actor, ActorSystem}
import akka.agent.Agent
import akka.event.Logging
import com.gu.pandomainauth.model.PanDomainAuthSettings
import com.gu.pandomainauth.service.{ProxyConfiguration, S3Bucket}

import scala.concurrent.duration.FiniteDuration

/**
  * PanDomainAuthSettingsRefresher will periodically refresh the pan domain settings and expose them via the "settings" method
  *
  * @param domain the domain you are authin agains
  * @param system the identifier for your app, typically the same as the subdomain your app runs on
  * @param bucketName the bucket where the settings are stored
  * @param actorSystem the actor system to create the refresh actor
  * @param awsCredentialsProvider AWS credential provider
  * @param awsRegion AWS region
  * @param proxyConfiguration optional proxy configuration
  */
class PanDomainAuthSettingsRefresher(
  val domain: String,
  val system: String,
  bucketName: String,
  actorSystem: ActorSystem,
  awsCredentialsProvider: AWSCredentialsProvider,
  awsRegion: Regions,
  proxyConfiguration: Option[ProxyConfiguration] = None
) {
  lazy val bucket = new S3Bucket(bucketName, awsCredentialsProvider, awsRegion, proxyConfiguration)

  private lazy val settingsMap = bucket.readDomainSettings(domain)
  private lazy val authSettings: Agent[PanDomainAuthSettings] = Agent(PanDomainAuthSettings(settingsMap))

  private lazy val domainSettingsRefreshActor = actorSystem.actorOf(Props(classOf[DomainSettingsRefreshActor], domain, bucket, authSettings), "PanDomainAuthSettingsRefresher")

  actorSystem.scheduler.scheduleOnce(1 minute, domainSettingsRefreshActor, Refresh)

  def settings = authSettings.get()
}

class DomainSettingsRefreshActor(domain: String, bucket: S3Bucket, authSettings: Agent[PanDomainAuthSettings]) extends Actor {

  val frequency: FiniteDuration = 1 minute
  val log = Logging(context.system, this)

  override def receive: Receive = {
    case Refresh => {
      try {
        val settingsMap = bucket.readDomainSettings(domain)

        val settings = PanDomainAuthSettings(settingsMap)

        authSettings send settings
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




