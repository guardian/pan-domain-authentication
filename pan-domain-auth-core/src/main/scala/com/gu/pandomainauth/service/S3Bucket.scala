package com.gu.pandomainauth.service

import java.util.Properties
import com.amazonaws.ClientConfiguration

import scala.collection.JavaConversions._
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest

import scala.io.Source


class S3Bucket(keyId: String, secretAccessKey: String, proxyConfiguration: Option[ProxyConfiguration] = None) {

  val awsCreds = new BasicAWSCredentials(keyId, secretAccessKey)
  val s3Client = new AmazonS3Client(awsCreds, awsClientConfiguration)

  val bucketName = "pan-domain-auth-settings"

  def readDomainSettings(domain: String) = {

    val domainSecretFile = s3Client.getObject(new GetObjectRequest(bucketName, domain + ".settings"))
    val props = new Properties()

    props.load(domainSecretFile.getObjectContent)
    props.toMap

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
