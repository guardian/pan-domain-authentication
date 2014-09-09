package com.gu.pandomainauth.service

import com.gu.pandomainauth.model.{User, GoogleAuthSettings, AuthenticatedUser}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result, RequestHeader}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.{WSResponse, WS}
import play.api.libs.json.JsValue
import scala.language.postfixOps
import java.math.BigInteger
import java.security.SecureRandom
import play.api.Application


class GoogleAuthException(val message: String, val throwable: Throwable = null) extends Exception(message, throwable)

class GoogleAuth(config: GoogleAuthSettings, system: String, redirectUrl: String) {
  var discoveryDocumentHolder: Option[Future[DiscoveryDocument]] = None

  private def discoveryDocument(implicit context: ExecutionContext, application: Application): Future[DiscoveryDocument] =
    if (discoveryDocumentHolder.isDefined) discoveryDocumentHolder.get
    else {
      val discoveryDocumentFuture = WS.url(DiscoveryDocument.url).get().map(r => DiscoveryDocument.fromJson(r.json))
      discoveryDocumentHolder = Some(discoveryDocumentFuture)
      discoveryDocumentFuture
    }

  val random = new SecureRandom()

  def generateAntiForgeryToken() = new BigInteger(130, random).toString(32)

  def googleResponse[T](r: WSResponse)(block: JsValue => T): T = {
    r.status match {
      case errorCode if errorCode >= 400 =>
        // try to get error if google sent us an error doc
        val error = (r.json \ "error").asOpt[Error]
        error.map { e =>
          throw new GoogleAuthException(s"Error when calling Google: ${e.message}")
        }.getOrElse {
          throw new GoogleAuthException(s"Unknown error when calling Google [status=$errorCode, body=${r.body}]")
        }
      case normal => block(r.json)
    }
  }

  def redirectToGoogle(antiForgeryToken: String, email: Option[String] = None)
                      (implicit context: ExecutionContext, application: Application, request: RequestHeader): Future[Result] = {
    val queryString: Map[String, Seq[String]] = Map(
      "client_id" -> Seq(config.googleAuthClient),
      "response_type" -> Seq("code"),
      "scope" -> Seq("openid email profile"),
      "redirect_uri" -> Seq(redirectUrl),
      "state" -> Seq(antiForgeryToken)
    ) ++ email.map("login_hint" -> Seq(_))

    discoveryDocument.map(dd => Redirect(s"${dd.authorization_endpoint}", queryString))
  }

  def validatedUserIdentity(expectedAntiForgeryToken: String)
                           (implicit request: RequestHeader, context: ExecutionContext, application: Application): Future[AuthenticatedUser] = {
    if (!request.queryString.getOrElse("state", Nil).contains(expectedAntiForgeryToken)) {
      throw new IllegalArgumentException("The anti forgery token did not match")
    } else {
      discoveryDocument.flatMap { dd =>
        val code = request.queryString("code")
        WS.url(dd.token_endpoint).post {
          Map(
            "code" -> code,
            "client_id" -> Seq(config.googleAuthClient),
            "client_secret" -> Seq(config.googleAuthSecret),
            "redirect_uri" -> Seq(redirectUrl),
            "grant_type" -> Seq("authorization_code")
          )
        }.flatMap { response =>
          googleResponse(response) { json =>
            val token = Token.fromJson(json)
            val jwt = token.jwt
            WS.url(dd.userinfo_endpoint)
              .withHeaders("Authorization" -> s"Bearer ${token.access_token}")
              .get().map { response =>
              googleResponse(response) { json =>
                val userInfo = UserInfo.fromJson(json)
                AuthenticatedUser(
                  user = User(
                    userInfo.given_name,
                    userInfo.family_name,
                    jwt.claims.email,
                    userInfo.picture
                  ),
                  authenticatingSystem = system,
                  authenticatedIn = Set(system),
                  jwt.claims.exp * 1000,
                  false
                )
              }
            }
          }
        }
      }
    }
  }
}