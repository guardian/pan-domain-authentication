package com.gu.pandomainauth.internal.planning

/**
 * 
 * Translates [[AuthPersistenceStatus]] into a [[Plan]].
 * 
 * We should have 2 implementations of this: PageEndpoint, and APIEndpoint
 * 
 * Authentication status needs to be handled differently depending on whether this is a full-page request,
 * or an API request:
 *
 * - Page request: can be redirected to an OAuth flow, so will immediately do so if credentials need refreshing - we
 * *do not* need to tolerate expired credentials for a grace period
 * - API request: can not redirect the whole page to an OAuth flow, only return a header recommending that the user
 * refresh the page - and if the user can not immediately do so, should tolerate older credentials for a grace period
 *
 */
trait AuthStatusHandler[RespType, RespMod] {
  def planForAuthStatus(authPersistenceStatus: AuthPersistenceStatus): Plan[RespType, RespMod]
}
