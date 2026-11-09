package com.gu.pandomainauth

import com.gu.pandomainauth.model.User

/**
 * This is the Panda API exposed to apps/frameworks (Play, Scalatra) that want to _invoke_ Panda authentication.
 * 
 * The API is designed so that it should be easy to delegate to this interface to handle a request - for
 * instance, we do this within [[com.gu.pandomainauth.action.AuthActions.AuthAction]], which is a Play-based
 * usage, but it should also be easy to use from Scalatra.
 * 
 * To create instances of these services, adapters must be supplied - these use the traits found within
 * [[com.gu.pandomainauth.webframeworks.WebFrameworkAdapter]].
 */
trait EndpointAuth[Req, Resp, F[_]] {
  /**
   * This method signature gives Panda control of the response - eg it will only invoke the supplied
   * function if it determines that the user is correctly authed, will add Panda cookies as
   * required, and may redirect if OAuth is required.
   */
  def authenticateRequest(request: Req)(produceResultGivenAuthedUser: User => F[Resp]): F[Resp]
}

object EndpointAuth {
  /**
   * Supporting full-page endpoints requires a few more methods than just doing API-endpoints
   */
  trait Page[Req, Resp, F[_]] extends EndpointAuth[Req, Resp, F] {
    def processOAuthCallback(request: Req): F[Resp]

    def processLogout(): Resp
  }
}
