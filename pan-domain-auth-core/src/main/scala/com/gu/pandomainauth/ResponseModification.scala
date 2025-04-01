package com.gu.pandomainauth

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
}