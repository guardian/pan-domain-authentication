package com.gu.pandomainauth

import com.amazonaws.auth.{DefaultAWSCredentialsProviderChain, AWSCredentialsProvider}
import com.amazonaws.regions.{Regions, Region}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.{ActorRef, Props, Actor, ActorSystem}
import akka.agent.Agent
import akka.event.Logging
import com.gu.pandomainauth.model.PanDomainAuthSettings
import com.gu.pandomainauth.service.{ProxyConfiguration, S3Bucket}

import scala.concurrent.duration.FiniteDuration


trait PanDomainAuth {

  lazy val actorSystem = ActorSystem()

  /**
   * the domain you are authin agains
   * @return
   */
  def domain: String

  /**
   * the identifier for your app, typically the same as the subdomain your app runs on
   * @return
   */
  def system: String

  /**
   * the AwsCredentials to access the configuration bucket, defaults to None so the instance credentials are used
   * @return
   */
  def awsCredentialsProvider: AWSCredentialsProvider = new DefaultAWSCredentialsProviderChain()

  /**
   * the aws region the configuration bucket is in, defaults to eu-west-1 as that's where the guardian tends to run stuff
   * @return
   */
  def awsRegion: Option[Region] = Option( Region getRegion Regions.EU_WEST_1 )

  /**
   * the proxy configuration to use when connecting to aws
   * @return
   */
  def proxyConfiguration: Option[ProxyConfiguration] = None

  lazy val bucket = new S3Bucket(awsCredentialsProvider, awsRegion, proxyConfiguration)

  lazy val settingsMap = bucket.readDomainSettings(domain)
  lazy val authSettings: Agent[PanDomainAuthSettings] = Agent(PanDomainAuthSettings(settingsMap))

  lazy val domainSettingsRefreshActor = actorSystem.actorOf(Props(classOf[DomainSettingsRefreshActor], domain, bucket, authSettings), "PanDomainAuthSettingsRefresher")

  actorSystem.scheduler.scheduleOnce(1 minute, domainSettingsRefreshActor, Refresh)

  def shutdown = actorSystem.terminate

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




