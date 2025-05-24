package com.gu.pandomainauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.model.AuthenticatedUser
import com.gu.pandomainauth.oauth.OAuthCodeToUser.TokenRequestParamsGenerator
import com.gu.pandomainauth.oauth.{Error, JsonWebToken, OAuthCodeToUser, OAuthException, Token}
import play.api.libs.json.JsValue
import play.api.libs.ws.*
import upickle.default.*

import java.net.URI
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PlayImplOfOAuthHttpClient(ws: WSClient)(implicit ec: ExecutionContext) extends OAuthHttpClient[Future] {
  
  override def httpPost(uri: URI, bodyParams: Map[String, String]): Future[String] = 
    ws.url(uri.toString).post(bodyParams.view.mapValues(Seq(_)).toMap).map(oAuthResponse)
  
  override def httpGet(uri: URI, httpHeaders: Map[String, String]): Future[String] =
    ws.url(uri.toString).withHttpHeaders(httpHeaders.toSeq: _*).get().map(oAuthResponse)

  private def oAuthResponse[T](r: WSResponse): String = r.status match {
    case errorCode if errorCode >= 400 =>
      // try to get error if we received an error doc (Google does this)
      Try(read[Error](r.body: String)).map { e =>
        throw new OAuthException(s"Error when calling OAuth provider: ${e.message}")
      }.getOrElse {
        throw new OAuthException(s"Unknown error when calling OAuth provider [status=$errorCode, body=${r.body}]")
      }
    case normal => r.body
  }
}

