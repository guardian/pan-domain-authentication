package com.gu.pandomainauth.oauth

import com.gu.pandomainauth.model.AuthenticatedUser

import scala.concurrent.{ExecutionContext, Future}

/**
 * Performs steps 4 & 5 of the OpenID Connect Server flow:
 *
 * 4. Exchange code for access token and ID token
 *    https://developers.google.com/identity/openid-connect/openid-connect#exchangecode
 * 5. Obtain user information from the ID token
 *    https://developers.google.com/identity/openid-connect/openid-connect#obtainuserinfo
 *
 * ...basically, makes two consecutive API calls to oauth2.googleapis.com .
 *
 * Will need to know
 * "client_id" -> Seq(config.clientId),
 * "client_secret" -> Seq(config.clientSecret),
 * "redirect_uri" -> Seq(redirectUrl), # the OAuth callback endpoint
 * DiscoveryDocument for token_endpoint & userinfo_endpoint
 * Also needs to be able to parse JSON and JWT
 */
trait OAuthValidator {
  /**
   *
   * @param code
   * @return
   */
  def validate(code: String)(implicit ec: ExecutionContext): Future[AuthenticatedUser]
}

class OAuthException(val message: String, val throwable: Throwable = null) extends Exception(message, throwable)