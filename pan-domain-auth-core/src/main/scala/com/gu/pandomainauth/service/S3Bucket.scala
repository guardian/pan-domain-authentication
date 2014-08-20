package com.gu.pandomainauth.service

import java.util.Properties
import scala.collection.JavaConversions._
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest

import scala.io.Source


class S3Bucket(keyId: String, secretAccessKey: String) {

  val awsCreds = new BasicAWSCredentials(keyId, secretAccessKey)
  val s3Client = new AmazonS3Client(awsCreds)

  val bucketName = "pan-domain-auth-settings"

  def readDomainSettings(domain: String) = {

    val domainSecretFile = s3Client.getObject(new GetObjectRequest(bucketName, domain + ".settings"))
    val props = new Properties()

    props.load(domainSecretFile.getObjectContent)
    props.toMap

  }
}
