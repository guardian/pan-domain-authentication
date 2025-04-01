package com.gu.pandomainauth

import org.apache.http.NameValuePair
import org.apache.http.client.utils.URIBuilder
import org.apache.http.message.BasicNameValuePair

import java.net.URI
import scala.jdk.CollectionConverters._

/**
 * https://developers.google.com/identity/openid-connect/openid-connect#sendauthrequest
 */
class OAuthUrl(
  clientId: String,
  oAuthCallbackUrl: URI,
  organizationDomain: Option[String], // eg guardian.co.uk
  authorizationEndpoint: () => URI // this can be caching endpoint that reads from the discovery document
) {
  def redirectToOAuthProvider(antiForgeryToken: String, loginHintEmail: Option[String] = None): URI = {
    val queryString: Map[String, String] = Map(
      "client_id" -> clientId,
      "response_type" -> "code",
      "scope" -> "openid email profile",
      "redirect_uri" -> oAuthCallbackUrl.toString, // TODO
      "state" -> antiForgeryToken
    ) ++ loginHintEmail.map("login_hint" -> _) ++ organizationDomain.map("hd" -> _)

    new URIBuilder(authorizationEndpoint()).addParameters(queryString.map {
      case (k, v) => new BasicNameValuePair(k, v): NameValuePair
    }.toBuffer.asJava).build()
  }
}
