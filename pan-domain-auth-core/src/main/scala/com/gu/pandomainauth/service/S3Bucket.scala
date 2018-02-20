package com.gu.pandomainauth.service

import java.util.Properties

import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.{Region, Regions}
import com.gu.pandomainauth.PublicSettings

import scala.collection.JavaConverters._
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future, Promise}
import scala.util.Try

class S3Bucket(credentialsProvider: AWSCredentialsProvider, regionOption: Option[Region] = None, proxyConfiguration: Option[ProxyConfiguration] = None) {

  val region = regionOption getOrElse (Region getRegion Regions.EU_WEST_1)
  val s3Client = region.createClient(classOf[AmazonS3Client], credentialsProvider, awsClientConfiguration)

  val bucketName = PublicSettings.bucketName

  def readDomainSettingsBlocking(domain: String): Map[String, String] = {
    Await.result(readDomainSettingsAsync(domain), 30.seconds)
  }

  def readDomainSettingsAsync(domain: String): Future[Map[String, String]] = {
    val promise = Promise[Map[String, String]]

    def fetch: Unit = {
      val result = Try {
        val domainSecretFile = s3Client.getObject(new GetObjectRequest(bucketName, domain + ".settings"))
        val props = new Properties()

        props.load(domainSecretFile.getObjectContent)
        props.asScala.toMap
      }
      promise.complete(result)
    }

    // The java way to span a new thread in order to block elsewhere than on Play's actor system
    val thread = new Thread(() => fetch)
    thread.setName("readDomainSettings")
    thread.setDaemon(true)
    thread.run

    promise.future
  }

  def getObjectInputStream(objectPath: String) = {
    val s3File = s3Client.getObject(new GetObjectRequest(bucketName, objectPath))
    s3File.getObjectContent
  }

  lazy val awsClientConfiguration: ClientConfiguration = {
    proxyConfiguration.map { c =>
      val awsClientConf = new ClientConfiguration()
      awsClientConf.setProxyHost(c.host)
      awsClientConf.setProxyPort(c.port)
      awsClientConf
    } getOrElse new ClientConfiguration()
  }
}
