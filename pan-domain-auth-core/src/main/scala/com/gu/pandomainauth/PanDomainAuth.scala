package com.gu.pandomainauth

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.{ActorRef, Props, Actor, ActorSystem}
import akka.agent.Agent
import akka.event.Logging
import com.gu.pandomainauth.model.PanDomainAuthSettings
import com.gu.pandomainauth.service.S3Bucket

import scala.concurrent.duration.FiniteDuration


trait PanDomainAuth {

  lazy val actorSystem = ActorSystem()

  def domain: String
  def system: String
  def awsKeyId: String
  def awsSecretAccessKey: String

  lazy val bucket = new S3Bucket(awsKeyId, awsSecretAccessKey)

  lazy val settingsMap = bucket.readDomainSettings(domain)
  lazy val authSettings: Agent[PanDomainAuthSettings] = Agent(PanDomainAuthSettings(settingsMap))

  lazy val domainSettingsRefreshActor = actorSystem.actorOf(Props(classOf[DomainSettingsRefreshActor], domain, bucket, authSettings), "PanDomainAuthSettingsRefresher")

  actorSystem.scheduler.scheduleOnce(1 minute, domainSettingsRefreshActor, Refresh)

  def shutdown = actorSystem.shutdown

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




