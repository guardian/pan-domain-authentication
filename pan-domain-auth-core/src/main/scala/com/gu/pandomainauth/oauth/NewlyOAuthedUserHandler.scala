package com.gu.pandomainauth.oauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.*
import com.gu.pandomainauth.internal.planning.*
import com.gu.pandomainauth.model.AuthenticatedUser
import com.gu.pandomainauth.service.TwoFactorAuthChecker

import java.net.URI

class NewlyOAuthedUserHandler[F[_] : Monad](systemAuthorisation: SystemAuthorisation, twoFactorAuthChecker: Option[TwoFactorAuthChecker[F]]) {

  private def augmentWithMultiFactor(newlyClaimedAuth: AuthenticatedUser): F[AuthenticatedUser] =
    twoFactorAuthChecker.traverse(_.check(newlyClaimedAuth.user.email)).map { multiFactorOpt =>
      newlyClaimedAuth.copy(multiFactor = multiFactorOpt.getOrElse(false))
    }

  def planFor(newlyClaimedAuth: AuthenticatedUser, priorAuth: AuthPersistenceStatus, returnUrl: URI): F[Plan[OAuthCallbackEndpoint.RespType, OAuthCallbackEndpoint.RespMod]] = for {
    userWithMultifactorStatus <- augmentWithMultiFactor(newlyClaimedAuth)
  } yield planForUserWithMultifactorStatus(userWithMultifactorStatus, priorAuth, returnUrl)

  private def planForUserWithMultifactorStatus(user: AuthenticatedUser, priorAuth: AuthPersistenceStatus, returnUrl: URI) =
    if (systemAuthorisation.isAuthorised(user, disableCache = true)) Plan(
      Redirect(returnUrl),
      Some(PersistAuth(user.withAuthorisationIn(systemAuthorisation.system).augmentWith(priorAuth.effectiveAuthStatus),
      wipeTemporaryCookiesUsedForOAuth = true))
    ) else Plan(NotAuthorized(user))
}
