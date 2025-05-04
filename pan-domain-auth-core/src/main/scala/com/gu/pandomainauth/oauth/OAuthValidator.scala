package com.gu.pandomainauth.oauth

import cats.*
import com.gu.pandomainauth.model.{AuthenticatedUser, OAuthSettings, User}

import java.net.URI
import java.time.Instant
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
abstract class OAuthValidator[F[_]: Monad] {
  /**
   * @param code
   */
  def validate(code: String): F[AuthenticatedUser]
}

class OAuthException(val message: String, val throwable: Throwable = null) extends Exception(message, throwable)

object OAuthValidator {
  case class TokenRequestParamsGenerator(oAuthSettings: OAuthSettings, oAuthCallbackUri: URI) {
    def paramsFor(code: String): Map[String, String] = Map(
      "code" -> code,
      "client_id" -> oAuthSettings.clientId,
      "client_secret" -> oAuthSettings.clientSecret,
      "redirect_uri" -> oAuthCallbackUri.toString,
      "grant_type" -> "authorization_code"
    )
  }

  def authenticatedUserFor(
    userInfo: UserInfo,
    jwtClaimsExpiry: Instant,
    jwtClaimEmail: Option[String],
    system: String
  ): AuthenticatedUser = AuthenticatedUser(
    user = User(
      userInfo.given_name,
      userInfo.family_name,
      jwtClaimEmail.getOrElse(userInfo.email),
      userInfo.picture
    ),
    authenticatingSystem = system,
    authenticatedIn = Set(system),
    expires = jwtClaimsExpiry,
    multiFactor = false
  )
}