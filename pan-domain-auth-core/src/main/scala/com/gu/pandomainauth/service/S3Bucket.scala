package com.gu.pandomainauth.service

import java.util.Properties

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.GetObjectRequest

import scala.collection.JavaConverters._

class S3Bucket(bucketName: String, credentialsProvider: AWSCredentialsProvider, region: String, proxyConfiguration: Option[ProxyConfiguration] = None) {
  val s3Client = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(credentialsProvider).build()

  def readDomainSettings(domain: String): Map[String, String] = {

    val domainSecretFile = s3Client.getObject(new GetObjectRequest(bucketName, domain + ".settings"))
    val props = new Properties()

    props.load(domainSecretFile.getObjectContent)
    props.asScala.toMap
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
