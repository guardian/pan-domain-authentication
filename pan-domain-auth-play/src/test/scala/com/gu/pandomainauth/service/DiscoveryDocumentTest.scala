package com.gu.pandomainauth.service

import org.scalatest.concurrent.TimeLimits
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.*

import java.nio.charset.StandardCharsets.UTF_8

class DiscoveryDocumentTest extends AnyFlatSpec with Matchers with TimeLimits {

  val typicalDiscoveryDocument = com.gu.pandomainauth.oauth.DiscoveryDocument(
    authorization_endpoint = "https://accounts.google.com/o/oauth2/v2/auth",
    token_endpoint = "https://oauth2.googleapis.com/token",
    userinfo_endpoint = "https://openidconnect.googleapis.com/v1/userinfo"
  )

  "Parser for Discovery Document" should "get the 3 fields we care about" in {
    val openidConf = new String(getClass.getResourceAsStream(s"/openid-configuration.sample.json").readAllBytes(), UTF_8)
    DiscoveryDocument.fromString(openidConf) shouldEqual typicalDiscoveryDocument
  }

  "General DiscoveryDocument.Cache" should "work well" in {
    val cache = new com.gu.pandomainauth.oauth.DiscoveryDocument.Cache(DiscoveryDocument.fromString)

    cache.get() // First call will actually make an HTTP request

    failAfter(200.millis) { // 'cache.get()' should be doing very little work now, this shouldn't take long!
      for (_ <- 0 to 1000000) cache.get()
    }
  }
}
