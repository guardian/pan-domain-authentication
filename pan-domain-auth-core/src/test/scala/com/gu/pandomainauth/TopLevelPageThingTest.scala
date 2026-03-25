package com.gu.pandomainauth

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI

class TopLevelPageThingTest extends AnyFreeSpec with Matchers {

  "Redirect url should roundtrip" - {
    "boom" in {
      val thing: EndpointAuth.Page[RequestHeader, Result, Id] = ???
      
      val response = thing.authenticateRequest(req)(_ => ???)

      thing.processOAuthCallback().resp shouldBe Redirect(originalUrl)
      
      
      val pageRequest =
        PageRequest(URI.create("https://example.com/?foo=comma%2Cslash%2Fpercent%25"), Map.empty)

      pageRequest.queryParams("foo") shouldBe "comma,slash/percent%"
    }
  }
}