package com.gu.pandomainauth.internal.planning

import com.gu.pandomainauth.model.{AcceptableAuthForApiRequests, AuthenticationStatus}

/**
 * Critical information about the user's authentication, captured from the [[com.gu.pandomainauth.PageRequest]].
 */
case class AuthPersistenceStatus(
  effectiveAuthStatus: AuthenticationStatus,
  systemsAuthorisationsCurrentlyPersistedWithUser: Set[String]
) {

  /**
   * If this is true, we will need to ensure that the user's auth cookie is freshly-persisted.
   */
  val hasUnpersistedSystemAuthorisations: Boolean = effectiveAuthStatus match {
    case acceptable: AcceptableAuthForApiRequests => acceptable.authedUser.isAuthorisedInMoreThan(systemsAuthorisationsCurrentlyPersistedWithUser)
    case _ => false
  }
}
