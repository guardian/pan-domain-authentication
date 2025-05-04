package com.gu.pandomainauth

import com.gu.pandomainauth.model.{AuthenticatedUser, OAuthSettings, User}
import com.gu.pandomainauth.oauth.OAuthValidator.TokenRequestParamsGenerator
import com.gu.pandomainauth.oauth.{OAuthException, OAuthValidator}
import com.gu.pandomainauth.service.{Error, Token, UserInfo}
import play.api.libs.json.JsValue
import play.api.libs.ws.*

import java.net.URI
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class PlayOAuthValidator(
  tokenRequestParamsGenerator: TokenRequestParamsGenerator,
  discoveryDocument: () => com.gu.pandomainauth.oauth.DiscoveryDocument,
  ws: WSClient,
  system: String,
)(implicit ec: ExecutionContext) extends OAuthValidator[Future] {

  override def validate(code: String): Future[AuthenticatedUser] = 
    fetchTokenFor(code).flatMap(token => authenticatedUserFor(token))

  private def fetchTokenFor(code: String): Future[Token] = 
    ws.url(discoveryDocument().token_endpoint).post(
      tokenRequestParamsGenerator.paramsFor(code).view.mapValues(Seq(_)).toMap
    ).map(response => oAuthResponse(response)(com.gu.pandomainauth.service.Token.fromJson))

  private def authenticatedUserFor(token: Token): Future[AuthenticatedUser] = 
    ws.url(discoveryDocument().userinfo_endpoint)
      .withHttpHeaders("Authorization" -> s"Bearer ${token.access_token}")
      .get().map { response =>
        oAuthResponse(response) { json =>
          val jwt = token.jwt
          OAuthValidator.authenticatedUserFor(
            UserInfo.fromJson(json),
            // The JWT standard specifies that `exp` is a `NumericDate`,
            // which is defined as an epoch time in *seconds*
            // (unlike the Panda cookie `expires` which is in milliseconds)
            // https://www.rfc-editor.org/rfc/rfc7519#section-4.1.4
            Instant.ofEpochSecond(jwt.claims.exp),
            jwt.claims.email,
            system
          )
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
