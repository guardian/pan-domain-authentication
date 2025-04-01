package com.gu.pandomainauth

import com.gu.pandomainauth.model.{AuthenticatedUser, OAuthSettings, User}
import com.gu.pandomainauth.oauth.{OAuthException, OAuthValidator}
import com.gu.pandomainauth.service.{Error, Token, UserInfo}
import play.api.libs.json.JsValue
import play.api.libs.ws._

import java.net.URI
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class PlayOAuthValidator(
  oAuthSettings: OAuthSettings,
  discoveryDocument: () => com.gu.pandomainauth.oauth.DiscoveryDocument,
  ws: WSClient,
  system: String,
  oAuthCallbackUri: URI
) extends OAuthValidator {

  override def validate(code: String)(implicit ec: ExecutionContext): Future[AuthenticatedUser] = {
      ws.url(discoveryDocument().token_endpoint).post {
        Map(
          "code" -> Seq(code),
          "client_id" -> Seq(oAuthSettings.clientId),
          "client_secret" -> Seq(oAuthSettings.clientSecret),
          "redirect_uri" -> Seq(oAuthCallbackUri.toString),
          "grant_type" -> Seq("authorization_code")
        )
      }.flatMap { response =>
        oAuthResponse(response) { json =>
          val token = Token.fromJson(json)
          val jwt = token.jwt
          ws.url(discoveryDocument().userinfo_endpoint)
            .withHttpHeaders("Authorization" -> s"Bearer ${token.access_token}")
            .get().map { response =>
              oAuthResponse(response) { json =>
                val userInfo = UserInfo.fromJson(json)
                AuthenticatedUser(
                  user = User(
                    userInfo.given_name,
                    userInfo.family_name,
                    jwt.claims.email.getOrElse(userInfo.email),
                    userInfo.picture
                  ),
                  authenticatingSystem = system,
                  authenticatedIn = Set(system),
                  // The JWT standard specifies that `exp` is a `NumericDate`,
                  // which is defined as an epoch time in *seconds*
                  // (unlike the Panda cookie `expires` which is in milliseconds)
                  // https://www.rfc-editor.org/rfc/rfc7519#section-4.1.4
                  expires = Instant.ofEpochSecond(jwt.claims.exp),
                  multiFactor = false
                )
              }
            }
        }
      }
    }

  def oAuthResponse[T](r: WSResponse)(block: JsValue => T): T = {
    r.status match {
      case errorCode if errorCode >= 400 =>
        // try to get error if we received an error doc (Google does this)
        val error = (r.json \ "error").asOpt[Error]
        error.map { e =>
          throw new OAuthException(s"Error when calling OAuth provider: ${e.message}")
        }.getOrElse {
          throw new OAuthException(s"Unknown error when calling OAuth provider [status=$errorCode, body=${r.body}]")
        }
      case normal => block(r.json)
    }
  }
}
