package com.gu.pandomainauth

import com.gu.pandomainauth.PanDomain.DefaultApiGracePeriod
import com.gu.pandomainauth.model.{AuthenticatedUser, AuthenticationStatus, CookieSettings, InvalidCookie, NotAuthenticated}
import com.gu.pandomainauth.service.CryptoConf.Verification

import java.time.Duration

/**
 * Is it too early to be applying SystemAuthorisation here? We can't return AuthenticationStatus NotAuthorized unless we
 * do. But if we do - do we also want to update the list of 'authenticatedIn' systems here? If so, how do we know whether
 * we need to apply cookie modification or not? Probably best to _not_ modify 'authenticatedIn' here... which is a shame.
 */
class AuthStatusFromRequest(
  val cookieReader: CookieReader,
  val systemAuthorisation: SystemAuthorisation,
  apiGracePeriod: Duration = DefaultApiGracePeriod
) {
  def authStatusFor(request: PageRequest): AuthPersistenceStatus =
    cookieReader.extractExistingAuthFrom(request).toRight(NotAuthenticated).flatMap(_.left.map(InvalidCookie(_))).fold(
      errorAuthStatus => AuthPersistenceStatus(errorAuthStatus, systemsAuthorisationsCurrentlyPersistedWithUser = Set.empty),
      persistedAuthedUser =>
        val forceExpiry = false // TODO - see https://github.com/guardian/pan-domain-authentication/pull/177
        AuthPersistenceStatus(
          effectiveAuthStatus =
            PanDomain.checkStatus(persistedAuthedUser, systemAuthorisation, apiGracePeriod, forceExpiry),
          persistedAuthedUser.authenticatedIn
        )
    )
}

object AuthStatusFromRequest {
  def apply(
    settingsRefresher: PanDomainAuthSettingsRefresher,
    systemAuthorisation: SystemAuthorisation
  ): AuthStatusFromRequest = new AuthStatusFromRequest(
    CookieReader(settingsRefresher),
    systemAuthorisation
  )
}