package com.gu.pandomainauth.oauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.PageRequestHandlingStrategy.{ANTI_FORGERY_KEY, LOGIN_ORIGIN_KEY}
import com.gu.pandomainauth.model.{AuthenticatedUser, AuthenticationStatus}
import com.gu.pandomainauth.*

import java.net.{URI, URLDecoder}
import java.nio.charset.StandardCharsets.UTF_8

class OAuthCallbackPlanner[F[_]: Monad](system: String, val cookieResponses: CookieResponses, oAuthValidator: OAuthCodeToUser[F])(implicit authStatusFromRequest: AuthStatusFromRequest) {
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

  private def planFor(newlyClaimedAuth: AuthenticatedUser, priorAuth: AuthenticationStatus, returnUrl: URI): Plan[OAuthCallbackResponse] = {
    val authedUserData = newlyClaimedAuth.copy(
      multiFactor = ??? // checkMultifactor(claimedAuth)
    )

    if (???) { // validateUser(authedUserData)
      Plan(PageResponse.Redirect(returnUrl), cookieResponses.cookieResponseFor(authedUserData.augmentWithSystemsFrom(priorAuth), wipeTemporaryCookiesUsedForOAuth = true))
    } else Plan(PageResponse.NotAuthorized(newlyClaimedAuth))
  }
}