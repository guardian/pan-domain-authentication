package com.gu.pandomainauth.internal

import com.gu.pandomainauth.PageEndpointAuthStatusHandler.LOGIN_ORIGIN_KEY
import com.gu.pandomainauth.ResponseModification
import com.gu.pandomainauth.ResponseModification.CookieChanges
import com.gu.pandomainauth.ResponseModification.CookieChanges.NameAndDomain
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.mvc.{Cookie, Results}

class PlayFrameworkAdapterTest extends AnyFlatSpec with Matchers {

  "PlayFrameworkAdapter" should "foo" in {
    def cookiesFor(cookieChanges: CookieChanges) = PlayFrameworkAdapter.responseModifier(
      ResponseModification(cookies = Some(cookieChanges))
    )(Results.Ok).newCookies

    val returnUrl = "%2Fcontent%2F69c467708f0861c75ee0b1ff%2Fversions"
    cookiesFor(CookieChanges(setSessionCookies = Map(
      LOGIN_ORIGIN_KEY -> returnUrl
    ))) shouldBe Seq(Cookie(LOGIN_ORIGIN_KEY.name, returnUrl, secure = true, httpOnly = true, Some(Cookie.SameSite.None)))

    val foo = "funky"
    cookiesFor(CookieChanges(setSessionCookies = Map(
      NameAndDomain("gutoolsAuth-assym", Some("code.dev-gutools.co.uk")) -> foo
    ))) shouldBe Seq(
      Cookie("gutoolsAuth-assym", foo, domain = Some("code.dev-gutools.co.uk"), secure = true, httpOnly = true, Some(Cookie.SameSite.None))
    )
  }
}
