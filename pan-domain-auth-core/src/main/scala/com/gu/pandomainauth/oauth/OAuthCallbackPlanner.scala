package com.gu.pandomainauth.oauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.*
import com.gu.pandomainauth.CookieAction.PersistAuth
import com.gu.pandomainauth.PageRequestHandlingStrategy.{ANTI_FORGERY_KEY, LOGIN_ORIGIN_KEY}
import com.gu.pandomainauth.model.AuthenticatedUser
import com.gu.pandomainauth.oauth.OAuthCallbackPlanner.CallbackPayload

import java.net.URI

class OAuthCallbackPlanner[F[_]: Monad](oAuthCodeToUser: OAuthCodeToUser[F])(
  implicit authStatusFromRequest: AuthStatusFromRequest
) {
  val F: Monad[F] = Monad[F]

  private val systemAuthorisation: SystemAuthorisation = authStatusFromRequest.systemAuthorisation

  def processOAuthCallback(request: PageRequest): F[Plan[OAuthCallbackResponse]] =
    CallbackPayload.from(request).fold(F.pure(Plan[OAuthCallbackResponse](PageResponse.BadRequest))) { payload =>
      oAuthCodeToUser.validate(payload.code).map { newAuthedUser =>
        planFor(newAuthedUser, priorAuth = request.authenticationStatus(), payload.returnUrl)
      }
    }

  private def planFor(newlyClaimedAuth: AuthenticatedUser, priorAuth: AuthPersistenceStatus, returnUrl: URI): Plan[OAuthCallbackResponse] = {
    if (systemAuthorisation.isAuthorised(newlyClaimedAuth, disableCache = true)) {
      val authorisedUser = newlyClaimedAuth.copy(
        multiFactor = ??? // checkMultifactor(claimedAuth)
      ).augmentWith(priorAuth.effectiveAuthStatus)
      Plan(PageResponse.Redirect(returnUrl), PersistAuth(authorisedUser, wipeTemporaryCookiesUsedForOAuth = true))
    } else Plan(PageResponse.NotAuthorized(newlyClaimedAuth))
  }
}

object OAuthCallbackPlanner {
  case class CallbackPayload(code: String, returnUrl: URI)

  object CallbackPayload {
    private def antiForgeryCheckIsPassedBy(request: PageRequest): Boolean = (for {
      expectedAntiForgeryToken <- request.getUrlDecodedCookie(ANTI_FORGERY_KEY)
      providedToken <- request.queryParams.get("state")
    } yield providedToken == expectedAntiForgeryToken).getOrElse(false)

    def from(pageRequest: PageRequest): Option[CallbackPayload] = for {
      returnUrl <- pageRequest.getUrlDecodedCookie(LOGIN_ORIGIN_KEY) if antiForgeryCheckIsPassedBy(pageRequest)
      code <- pageRequest.queryParams.get("code")
    } yield CallbackPayload(code, URI.create(returnUrl))
  }
}