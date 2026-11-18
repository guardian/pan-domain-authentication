package com.gu.pandomainauth.oauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.*
import com.gu.pandomainauth.PageEndpointAuthStatusHandler.{ANTI_FORGERY_KEY, LOGIN_ORIGIN_KEY}
import com.gu.pandomainauth.internal.planning.{AuthStatusFromRequest, OAuthCallbackEndpoint, Plan}
import com.gu.pandomainauth.oauth.OAuthCallbackPlanner.CallbackPayload
import com.gu.pandomainauth.oauth.OAuthCallbackPlanner.PayloadFailure.MissingCookie
import com.gu.pandomainauth.service.TwoFactorAuthChecker

import java.net.URI

class OAuthCallbackPlanner[F[_]: Monad](oAuthCodeToUser: OAuthCodeToUser[F], twoFactorAuthChecker: Option[TwoFactorAuthChecker[F]])(
  implicit authStatusFromRequest: AuthStatusFromRequest
) {
  val F: Monad[F] = Monad[F]

  private val newlyAuthenticatedUserHandler =
    new NewlyOAuthedUserHandler[F](authStatusFromRequest.systemAuthorisation, twoFactorAuthChecker)

  /**
   * processOAuthCallback
   */
  def planFor(request: PageRequest): F[Plan[OAuthCallbackEndpoint.RespType, OAuthCallbackEndpoint.RespMod]] =
    CallbackPayload.from(request).fold(pf => F.pure(Plan[OAuthCallbackEndpoint.RespType, OAuthCallbackEndpoint.RespMod](OAuthCallbackEndpoint.BadOAuthCallback(pf))), payload =>
      oAuthCodeToUser.validate(payload.code).flatMap { barelyAuthedUser =>
        newlyAuthenticatedUserHandler.planFor(barelyAuthedUser, priorAuth = request.authenticationStatus(), payload.returnUrl)
      }
    )
}

object OAuthCallbackPlanner {

  sealed trait PayloadFailure

  object PayloadFailure {
    case class MissingCookie(name: String) extends PayloadFailure

    case class MissingQueryParam(name: String) extends PayloadFailure

    case object AntiForgeryTokenMismatch extends PayloadFailure
  }



  case class CallbackPayload(code: String, returnUrl: URI)

  object CallbackPayload {
    def cookie(request: PageRequest, name: String): Either[OAuthCallbackPlanner.PayloadFailure, String] =
      request.getUrlDecodedCookie(name).toRight(OAuthCallbackPlanner.PayloadFailure.MissingCookie(name))

    def param(request: PageRequest, name: String): Either[OAuthCallbackPlanner.PayloadFailure, String] =
      request.queryParams.get(name).toRight(OAuthCallbackPlanner.PayloadFailure.MissingQueryParam(name))
    
    private def antiForgeryCheck(request: PageRequest): Either[OAuthCallbackPlanner.PayloadFailure, Unit] = for {
      expectedAntiForgeryToken <- cookie(request, ANTI_FORGERY_KEY.name)
      providedToken <- param(request, "state")
      _ <- Either.cond(providedToken == expectedAntiForgeryToken, (), OAuthCallbackPlanner.PayloadFailure.AntiForgeryTokenMismatch)
    } yield ()

    def from(request: PageRequest): Either[PayloadFailure, CallbackPayload] = for {
      _ <- antiForgeryCheck(request)
      returnUrl <- cookie(request, LOGIN_ORIGIN_KEY.name)
      code <- param(request, "code")
    } yield CallbackPayload(code, URI.create(returnUrl))
  }
}