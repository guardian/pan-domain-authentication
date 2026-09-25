package com.gu.pandomainauth.internal.planning

import com.gu.pandomainauth.*
import com.gu.pandomainauth.PanDomain.DefaultApiGracePeriod
import com.gu.pandomainauth.model.*

import java.time.Duration

/**
 * Translates a Panda-[[PageRequest]] into a [[AuthPersistenceStatus]], capturing all the critical information
 * about the user's authentication.
 */
class AuthStatusFromRequest(
  val cookieReader: CookieReader,
  val systemAuthorisation: SystemAuthorisation,
  apiGracePeriod: Duration = DefaultApiGracePeriod
) {
  def authStatusFor(request: PageRequest): AuthPersistenceStatus =
    cookieReader.extractExistingAuthFrom(request).toRight(NotAuthenticated).flatMap(_.left.map(InvalidCookie(_))).fold(
      errorAuthStatus => AuthPersistenceStatus(errorAuthStatus, systemsAuthorisationsCurrentlyPersistedWithUser = Set.empty),
      persistedAuthedUser => {
        val forceExpiry = false // TODO - see https://github.com/guardian/pan-domain-authentication/pull/177
        AuthPersistenceStatus(
          effectiveAuthStatus =
            PanDomain.checkStatus(persistedAuthedUser, systemAuthorisation, apiGracePeriod, forceExpiry),
          systemsAuthorisationsCurrentlyPersistedWithUser = persistedAuthedUser.authenticatedIn
        )
      }
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