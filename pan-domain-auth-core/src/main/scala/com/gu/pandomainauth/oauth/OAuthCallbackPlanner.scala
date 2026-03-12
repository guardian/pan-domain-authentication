package com.gu.pandomainauth.oauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.*
import com.gu.pandomainauth.PageEndpointAuthStatusHandler.{ANTI_FORGERY_KEY, LOGIN_ORIGIN_KEY}
import com.gu.pandomainauth.internal.planning.{AuthStatusFromRequest, OAuthCallbackEndpoint, Plan}
import com.gu.pandomainauth.oauth.OAuthCallbackPlanner.CallbackPayload
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
    CallbackPayload.from(request).fold(F.pure(Plan[OAuthCallbackEndpoint.RespType, OAuthCallbackEndpoint.RespMod](OAuthCallbackEndpoint.BadRequest))) { payload =>
      oAuthCodeToUser.validate(payload.code).flatMap { barelyAuthedUser =>
        newlyAuthenticatedUserHandler.planFor(barelyAuthedUser, priorAuth = request.authenticationStatus(), payload.returnUrl)
      }
    }
}

object OAuthCallbackPlanner {
  case class CallbackPayload(code: String, returnUrl: URI)

  object CallbackPayload {
    private def antiForgeryCheckIsPassedBy(request: PageRequest): Boolean = (for {
      expectedAntiForgeryToken <- request.getUrlDecodedCookie(ANTI_FORGERY_KEY.name)
      providedToken <- request.queryParams.get("state")
    } yield providedToken == expectedAntiForgeryToken).getOrElse(false)

    def from(pageRequest: PageRequest): Option[CallbackPayload] = for {
      returnUrl <- pageRequest.getUrlDecodedCookie(LOGIN_ORIGIN_KEY.name) if antiForgeryCheckIsPassedBy(pageRequest)
      code <- pageRequest.queryParams.get("code")
    } yield CallbackPayload(code, URI.create(returnUrl))
  }
}