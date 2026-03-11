package com.gu.pandomainauth.oauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.*
import com.gu.pandomainauth.PageEndpointAuthStatusHandler.{ANTI_FORGERY_KEY, LOGIN_ORIGIN_KEY}
import com.gu.pandomainauth.internal.planning.{AuthPersistenceStatus, AuthStatusFromRequest, NotAuthorized, OAuthCallbackEndpoint, PersistAuth, Plan, Planner, Redirect}
import com.gu.pandomainauth.model.AuthenticatedUser
import com.gu.pandomainauth.oauth.OAuthCallbackPlanner.CallbackPayload
import com.gu.pandomainauth.service.TwoFactorAuthChecker

import java.net.URI

class OAuthCallbackPlanner[F[_]: Monad](oAuthCodeToUser: OAuthCodeToUser[F], twoFactorAuthChecker: Option[TwoFactorAuthChecker[F]])(
  implicit authStatusFromRequest: AuthStatusFromRequest
) {
  val F: Monad[F] = Monad[F]

  private val systemAuthorisation: SystemAuthorisation = authStatusFromRequest.systemAuthorisation

  /**
   * processOAuthCallback
   */
  def planFor(request: PageRequest): F[Plan[OAuthCallbackEndpoint.RespType, OAuthCallbackEndpoint.RespMod]] =
    CallbackPayload.from(request).fold(F.pure(Plan[OAuthCallbackEndpoint.RespType, OAuthCallbackEndpoint.RespMod](OAuthCallbackEndpoint.BadRequest))) { payload =>
      oAuthCodeToUser.validate(payload.code).flatMap(augmentWithMultiFactor).map { newAuthedUser =>
        planFor(newAuthedUser, priorAuth = request.authenticationStatus(), payload.returnUrl)
      }
    }

  private def augmentWithMultiFactor(newlyClaimedAuth: AuthenticatedUser): F[AuthenticatedUser] =
    twoFactorAuthChecker.traverse(_.check(newlyClaimedAuth.user.email)).map { multiFactorOpt =>
      newlyClaimedAuth.copy(multiFactor = multiFactorOpt.getOrElse(false))
    }

  private def planFor(newlyClaimedAuth: AuthenticatedUser, priorAuth: AuthPersistenceStatus, returnUrl: URI): Plan[OAuthCallbackEndpoint.RespType, OAuthCallbackEndpoint.RespMod] =
    if (systemAuthorisation.isAuthorised(newlyClaimedAuth, disableCache = true))
      Plan(Redirect(returnUrl), Some(PersistAuth(newlyClaimedAuth.augmentWith(priorAuth.effectiveAuthStatus), wipeTemporaryCookiesUsedForOAuth = true)))
    else Plan(NotAuthorized(newlyClaimedAuth))
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