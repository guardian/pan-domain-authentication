package com.gu.pandomainauth

import com.gu.pandomainauth.model.AuthenticatedUser

case class SystemAuthorisation(system: String, validateUser: AuthenticatedUser => Boolean, cacheValidation: Boolean) {
  def isAuthorised(authenticatedUser: AuthenticatedUser, disableCache: Boolean = false): Boolean = {
    val cachedValidationSatisfies = cacheValidation && !disableCache && authenticatedUser.authenticatedIn(system)

    cachedValidationSatisfies || validateUser(authenticatedUser)
  }
}
