package com.gu.pandomainauth

import dispatch._

import scala.concurrent.Future


object PublicSettings {
  val bucketName = "pan-domain-auth-settings"
  val cookieName = "gutoolsAuth"
  val assymCookieName = s"$cookieName-assym"

  def getPublicKey(domain: String)(implicit client: Http, ec: scala.concurrent.ExecutionContext): Future[String] = {
    val req = host("s3-eu-west-1.amazonaws.com").secure / bucketName / s"$domain.settings.public"
    client(req OK as.String).either.left.map(new PublicKeyAcquisitionException(_)).map { attemptedKey =>
      attemptedKey.right.flatMap(validateKey)
    } flatMap {
      case Right(key) => Future.successful(key)
      case Left(err) => Future.failed(err)
    }
  }

  private[pandomainauth] def validateKey(pubKey: String): Either[Throwable, String] = {
    if ("[a-zA-Z0-9+/\n]+={0,3}".r.pattern.matcher(pubKey).matches) Right(pubKey)
    else Left(PublicKeyFormatException)
  }

  class PublicKeyAcquisitionException(cause: Throwable) extends Exception(cause.getMessage, cause)
  object PublicKeyFormatException extends Exception("Invalid public key")
}
