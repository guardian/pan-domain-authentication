package com.gu.pandomainauth

import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI

class PageRequestTest extends AnyFreeSpec with Matchers with EitherValues {

  "Query string parsing provides *url-decoded* values for query string params" - {
    "from a small example" in {
      val pageRequest =
        PageRequest(URI.create("https://example.com/?foo=comma%2Cslash%2Fpercent%25"), Map.empty)

      pageRequest.queryParams("foo") shouldBe "comma,slash/percent%"
    }

    "from a real OAuth callback request" in {
      val pageRequest =
        PageRequest(URI.create("https://tagmanager.code.dev-gutools.co.uk/oauthCallback?state=p3gotepknm1umnau3drqo36sr9&code=4%2F0Ab_5qlmCfesGyerRg5GyRmt9E9LflBbBEMpSLJPp33besBE_EjzcFRuVIbtT-cQ"), Map.empty)

      pageRequest.queryParams("state") shouldBe "p3gotepknm1umnau3drqo36sr9"
      pageRequest.queryParams("code") shouldBe "4/0Ab_5qlmCfesGyerRg5GyRmt9E9LflBbBEMpSLJPp33besBE_EjzcFRuVIbtT-cQ"
    }
  }
}


