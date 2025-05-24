package com.gu.pandomainauth.oauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.OAuthHttpClient
import com.gu.pandomainauth.model.{AuthenticatedUser, OAuthSettings, User}
import upickle.default.*

import java.net.URI
import java.time.Instant

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
class OAuthCodeToUser[F[_]: Monad](
  tokenRequestParamsGenerator: OAuthCodeToUser.TokenRequestParamsGenerator,
  system: String,
  httpClient: OAuthHttpClient[F],
  discoveryDocument: () => DiscoveryDocument
) {

  def validate(code: String): F[AuthenticatedUser] =
    fetchTokenFor(code).flatMap(token => authenticatedUserFor(token))

  private def fetchTokenFor(code: String): F[com.gu.pandomainauth.oauth.Token] =
    httpClient.httpPost(discoveryDocument().tokenEndpoint, bodyParams = tokenRequestParamsGenerator.paramsFor(code))
    .map(read[Token](_))

  private def authenticatedUserFor(token: com.gu.pandomainauth.oauth.Token): F[AuthenticatedUser] = httpClient
    .httpGet(discoveryDocument().userinfoEndpoint, Map("Authorization" -> s"Bearer ${token.accessToken}"))
    .map { userInfoJson =>
      val jwt = JsonWebToken.claimsFrom(token.idToken)

      OAuthCodeToUser.authenticatedUserFor(
        read[UserInfo](userInfoJson),
        // The JWT standard specifies that `exp` is a `NumericDate`,
        // which is defined as an epoch time in *seconds*
        // (unlike the Panda cookie `expires` which is in milliseconds)
        // https://www.rfc-editor.org/rfc/rfc7519#section-4.1.4
        Instant.ofEpochSecond(jwt.exp),
        jwt.email,
        system
      )
    }
}

class OAuthException(val message: String, val throwable: Throwable = null) extends Exception(message, throwable)

object OAuthCodeToUser {
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