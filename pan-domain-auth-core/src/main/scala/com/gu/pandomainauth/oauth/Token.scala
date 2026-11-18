package com.gu.pandomainauth.oauth

import upickle.implicits.key
import upickle.default._

import java.net.URI

/**
 * This token is returned by step 4, and used for sending step 5, of the OpenID Connect Server flow:
 *
 * 4. Exchange code for access token and ID token
 *    https://developers.google.com/identity/openid-connect/openid-connect#exchangecode
 * 5. Obtain user information from the ID token
 *    https://developers.google.com/identity/openid-connect/openid-connect#obtainuserinfo
 * 
 **/
case class Token(
  @key("access_token") accessToken: String,
  @key("id_token") idToken: String
)
// val jwt = JsonWebToken(id_token)

object Token {
  implicit val tokenRW: ReadWriter[Token] = macroRW[Token]
}