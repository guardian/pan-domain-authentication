package com.gu.pandomainauth

import com.gu.pandomainauth.PageEndpointAuthStatusHandler.LOGIN_ORIGIN_KEY
import com.gu.pandomainauth.ResponseModification.CookieChanges

object SampleCookieChanges {

  /**
   * Testing against a mainline, pre-Panda-PR-191 build of flexible-restorer
   * (https://github.com/guardian/flexible-restorer/commit/f7701baf39a80c55814b87f74ad28631a3510be3)
   * we see that if we clear auth-cookies and try to access this page:
   *
   * https://restorer.code.dev-gutools.co.uk/content/6ae326bb8f08faa78d794e9a/versions
   *
   * ...the value written by Panda in the response headers to the 'loginOriginUrl' cookie is
   * '%2Fcontent%2F6ae326bb8f08faa78d794e9a%2Fversions'
   *
   * The full Set-Cookie header is:
   *
   * ```
   * panda-loginOriginUrl-9dj0rf=%2Fcontent%2F6ae326bb8f08faa78d794e9a%2Fversions; Max-Age=300; Expires=Thu, 29 Oct 2026 11:20:23 GMT; SameSite=None; Path=/; Secure; HTTPOnly
   * ```
   */
  val sampleLoginOrigin = CookieChanges(setSessionCookies = Map(
    LOGIN_ORIGIN_KEY -> "%2Fcontent%2F69c467708f0861c75ee0b1ff%2Fversions"
  ))
}
