package com.gu.pandomainauth.service

import com.gu.pandomainauth.oauth.DiscoveryDocument
import org.scalatest.concurrent.TimeLimits
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.*

import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import upickle.default.*

class DiscoveryDocumentTest extends AnyFlatSpec with Matchers with TimeLimits {

  val typicalDiscoveryDocument: DiscoveryDocument = com.gu.pandomainauth.oauth.DiscoveryDocument(
    authorizationEndpoint = URI.create("https://accounts.google.com/o/oauth2/v2/auth"),
    tokenEndpoint = URI.create("https://oauth2.googleapis.com/token"),
    userinfoEndpoint = URI.create("https://openidconnect.googleapis.com/v1/userinfo")
  )

  "Parser for Discovery Document" should "get the 3 fields we care about" in {
    val openidConf = new String(getClass.getResourceAsStream(s"/openid-configuration.sample.json").readAllBytes(), UTF_8)

    val discoveryDoc: DiscoveryDocument = read[DiscoveryDocument](openidConf)

    discoveryDoc shouldEqual typicalDiscoveryDocument
  }

  "General DiscoveryDocument.Cache" should "work well" in {
    val cache = DiscoveryDocument.Cache

    val discoveryDocument = cache.get() // First call will actually make an HTTP request

    discoveryDocument shouldEqual typicalDiscoveryDocument // feel free to update the sample if this changes

    failAfter(300.millis) { // 'cache.get()' should be doing very little work now, this shouldn't take long!
      for (_ <- 0 to 1000000) cache.get()
    }
  }
}
