package com.gu.pandomainauth

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.gu.pandomainauth.model.AuthenticatedUser
import com.gu.pandomainauth.service.CookieUtils
import com.gu.pandomainauth.service.CryptoConf.SigningAndVerification
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, OptionValues}

import java.time.Instant
import java.time.temporal.ChronoUnit.HOURS

class PanDomainAuthSettingsRefresherTest extends AnyFreeSpec with Matchers with EitherValues with OptionValues {

  "Give an expired cookie" in {
    val myRealCookie = ""// "Paste in your real cookie value here - from the 'gutoolsAuth-assym' cookie"

    val s3Client =
      AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1)
        .withCredentials(new ProfileCredentialsProvider("workflow")).build()

    val signingAndVerification: SigningAndVerification =
      PanDomainAuthSettingsRefresher("gutools.co.uk", "testing",
        S3BucketLoader.forAwsSdkV1(s3Client, "pan-domain-auth-settings")
      ).settings.signingAndVerification
    val user: AuthenticatedUser = CookieUtils.parseCookieData(myRealCookie, signingAndVerification).value
    val expiredUser = user.copy(expires = Instant.now.minus(48, HOURS).toEpochMilli)
    val expiredCookieText = CookieUtils.generateCookieData(expiredUser, signingAndVerification)
    println(expiredCookieText)
  }
}
