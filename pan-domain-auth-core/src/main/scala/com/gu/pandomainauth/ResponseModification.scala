package com.gu.pandomainauth

import com.gu.pandomainauth.ResponseModification.CookieChanges
import com.gu.pandomainauth.ResponseModification.CookieChanges.NameAndDomain


/**
 * This class is a simple, concrete, framework-adapter-facing representation of changes that should be made to
 * the HTTP response. The fields are intentionally as simple to facilitate the [[WebFramework]] adapter translating
 * them into its own response type.
 *
 * Contrast with the RespMod of the [[Plan]] class - where RespMod embodies higher-level concepts 
 * (like [[PageEndpoint.Logout]], or [[ApiEndpoint.CredentialRefreshing]]) that are easier
 * to write tests around.
 */
case class ResponseModification(
  cookies: Option[CookieChanges] = None,
  responseHeaders: Map[String, String] = Map.empty
)

object ResponseModification {
  def apply(c: CookieChanges): ResponseModification = ResponseModification(cookies = Some(c))

  /**
   * The literal cookie-values Panda needs set & wiped.
   */
  case class CookieChanges(
    setSessionCookies: Map[NameAndDomain, String] = Map.empty,
    wipeCookies: Set[NameAndDomain] = Set.empty
  )

  object CookieChanges {
    val NoChanges = CookieChanges()

    /**
     * @param domain if populated, the domain the cookie should be dropped on (eg gutools.co.uk). If empty, leave the
     *               cookie domain unspecified when creating the cookie - browsers interpret this as meaning that
     *               the cookie domain should be the exact domain of the site storing the cookie (eg composer.gutools.co.uk)
     *               ...this is appropriate for temporary OAuth-callback-related cookies.
     */
    case class NameAndDomain(name: String, domain: Option[String])
  }
}
