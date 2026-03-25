package com.gu.pandomainauth

import com.gu.pandomainauth.internal.planning.PageEndpoint.PrepareForOAuth
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI

class CookieResponsesTest extends AnyFreeSpec with Matchers with EitherValues {

  "Return url" - {
    "should be URL-encoded before it's stored to a cookie" in {
      val responses: CookieResponses = ???

      responses.pageEndpoint(PrepareForOAuth(URI.create("/content/69c26ad58f08a301f86bb7cd/versions", "my-anti-forgery-token")))
    }

    "from a real OAuth callback request" in {
      val pageRequest =
        PageRequest(URI.create("https://tagmanager.code.dev-gutools.co.uk/oauthCallback?state=p3gotepknm1umnau3drqo36sr9&code=4%2F0Ab_5qlmCfesGyerRg5GyRmt9E9LflBbBEMpSLJPp33besBE_EjzcFRuVIbtT-cQ"), Map.empty)

      pageRequest.queryParams("state") shouldBe "p3gotepknm1umnau3drqo36sr9"
      pageRequest.queryParams("code") shouldBe "4/0Ab_5qlmCfesGyerRg5GyRmt9E9LflBbBEMpSLJPp33besBE_EjzcFRuVIbtT-cQ"
    }
  }
}
