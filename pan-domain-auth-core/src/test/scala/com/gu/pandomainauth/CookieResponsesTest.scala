package com.gu.pandomainauth

import com.gu.pandomainauth.internal.planning.PageEndpoint.PrepareForOAuth
import com.gu.pandomainauth.model.CookieSettings
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI

class CookieResponsesTest extends AnyFreeSpec with Matchers with OptionValues {

  "Return url" - {
    "should be URL-encoded before it's stored to a cookie" in {
      val responses: CookieResponses = new CookieResponses(
        cookieSettings = CookieSettings("cookie-name???"),
        signing = () => ???,
        system = "system???",
        domain = "domain???"
      )

      val responseModification =
        responses.pageEndpoint(PrepareForOAuth(URI.create("/content/69c26ad58f08a301f86bb7cd/versions"), "my-anti-forgery-token"))

      val cookieChanges = responseModification.cookies.value

      // TODO: Need to gather a real bit of data from a login for this... _should_ it be url-encoded?!
      // cookieChanges.setSessionCookies(LOGIN_ORIGIN_KEY) shouldBe "/content/69c26ad58f08a301f86bb7cd/versions"
    }
  }
}
