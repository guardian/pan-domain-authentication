package com.gu.pandomainauth.oauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.PageRequestHandlingStrategy.{ANTI_FORGERY_KEY, LOGIN_ORIGIN_KEY}
import com.gu.pandomainauth.model.{AuthenticatedUser, AuthenticationStatus}
import com.gu.pandomainauth.{AuthStatusFromRequest, CookieResponses, OAuthCallbackResponse, PageRequest, PageResponse, Plan}

import java.nio.charset.StandardCharsets.UTF_8
import com.gu.pandomainauth.model.{Authenticated, GracePeriod}

import java.net.{URI, URLDecoder}

class OAuthCallbackPlanner[F[+_]: Monad](
  oAuthValidator: OAuthCodeToUser[F],
  val cookieResponses: CookieResponses,
  system: String
)(implicit authStatusFromRequest: AuthStatusFromRequest) {
  val F: Monad[F] = Monad[F]

  def processOAuthCallback(request: PageRequest): F[Plan[OAuthCallbackResponse]] = {
    def decodeCookie(name: String): Option[String] =
      request.cookies.get(name).map(value => URLDecoder.decode(value, UTF_8))

    (for {
      expectedAntiForgeryToken <- decodeCookie(ANTI_FORGERY_KEY)
      antiForgeryTokenFromQueryParams <- request.queryParams.get("state") if antiForgeryTokenFromQueryParams == expectedAntiForgeryToken
      returnUrl <- decodeCookie(LOGIN_ORIGIN_KEY)
      code <- request.queryParams.get("code")
    } yield oAuthValidator.validate(code).map(newAuth => planFor(newAuth, request.authenticationStatus(), URI.create(returnUrl)))
      ) getOrElse F.pure(Plan[OAuthCallbackResponse](???, ???)) // Future.successful(BadRequest("Missing cookies, bad anti-forgery, etc"))
  }

  private def planFor(newlyClaimedAuth: AuthenticatedUser, priorAuth: AuthenticationStatus, returnUrl: URI) = {
    val authedSystemsFromPriorAuth: Set[String] = (priorAuth match {
      case Authenticated(authedUser) => Some(authedUser)
      case GracePeriod(authedUser) => Some(authedUser)
      case _ => None
    }).filter(_.user.email == newlyClaimedAuth.user.email).toSet.flatMap[String](_.authenticatedIn)
    val authedUserData = newlyClaimedAuth.copy(
      authenticatingSystem = system,
      authenticatedIn = authedSystemsFromPriorAuth + system,
      multiFactor = ??? // checkMultifactor(claimedAuth)
    )

    if (???) { // validateUser(authedUserData)
      Plan(PageResponse.Redirect(returnUrl), cookieResponses.cookieResponseFor(authedUserData, wipeTemporaryCookiesUsedForOAuth = true))
    } else Plan(PageResponse.NotAuthorized(newlyClaimedAuth.user))
  }
}