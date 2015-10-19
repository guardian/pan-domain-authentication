package com.gu.pandomainauth.service

import java.util.Properties
import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.{Regions, Region}
import com.gu.pandomainauth.PublicSettings

import scala.collection.JavaConversions._
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest

class S3Bucket(credentialsProvider: AWSCredentialsProvider, regionOption: Option[Region] = None, proxyConfiguration: Option[ProxyConfiguration] = None) {

  val region = regionOption getOrElse (Region getRegion Regions.EU_WEST_1)
  val s3Client = region.createClient(classOf[AmazonS3Client], credentialsProvider, awsClientConfiguration)

  val bucketName = PublicSettings.bucketName

  def readDomainSettings(domain: String) = {

    val domainSecretFile = s3Client.getObject(new GetObjectRequest(bucketName, domain + ".settings"))
    val props = new Properties()

    props.load(domainSecretFile.getObjectContent)
    props.toMap

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
