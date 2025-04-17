package com.gu.pandomainauth.oauth

import com.gu.pandomainauth.model.AuthenticatedUser

import scala.concurrent.Future


/**
 * Will need to know
 * "client_id" -> Seq(config.clientId),
 * "client_secret" -> Seq(config.clientSecret),
 * "redirect_uri" -> Seq(redirectUrl), # the OAuth callback endpoint
 * DiscoveryDocument for token_endpoint & userinfo_endpoint
 * Also needs to be able to parse JSON and JWT
 *
 * https://developers.google.com/identity/openid-connect/openid-connect#exchangecode
 * https://developers.google.com/identity/openid-connect/openid-connect#obtainuserinfo
 */
trait OAuthValidator {
  /**
   *
   * @param code
   * @return
   */
  def validate(code: String): Future[AuthenticatedUser]
}
