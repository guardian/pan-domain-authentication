package com.gu.pandomainauth.oauth

import cats.Monad

import java.net.URI

abstract class OAuthHttpClient[F[_] : Monad] {

  /**
   * Make an HTTP POST request to the supplied URI, with the supplied params as the request body, return the body response as a string.
   *
   * Used for step 4 of the OpenID Connect Server flow: "Exchange code for access token and ID token"
   * https://developers.google.com/identity/openid-connect/openid-connect#exchangecode
   */
  def httpPost(uri: URI, bodyParams: Map[String, String]): F[String]

  /**
   * Make an HTTP GET request to the supplied URI with the http headers, return the body response as a string.
   *
   * Used for step 5 of the OpenID Connect Server flow: "Obtain user information from the ID token"
   * https://developers.google.com/identity/openid-connect/openid-connect#obtainuserinfo
   */
  def httpGet(uri: URI, httpHeaders: Map[String, String]): F[String]
}
