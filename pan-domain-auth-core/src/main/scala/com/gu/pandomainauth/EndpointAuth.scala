package com.gu.pandomainauth

import cats.Monad
import com.gu.pandomainauth.model.User
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter.RequestAdapter

/**
 * This is the Panda API exposed for frameworks (Play, Scalatra) that want to invoke Panda authentication.
 * 
 * The API is designed so that it should be easy to delegate to this interface to handle a request - for
 * instance, we do this within [[com.gu.pandomainauth.action.AuthActions.AuthAction]], which is a Play-based
 * usage, but it should also be easy to use from Scalatra.
 * 
 * To create instances of these services, adapters must be supplied.
 */
trait EndpointAuth[Req, Resp, F[_]: Monad] {
  /**
   * This method signature gives Panda control of the response - eg it will only invoke the supplied
   * function if it determines that the user is correctly authed, will add Panda cookies as
   * required, and may redirect if OAuth is required.
   */
  def authenticateRequest(request: Req)(produceResultGivenAuthedUser: User => F[Resp]): F[Resp]
}

object EndpointAuth {
  trait Page[Req, Resp, F[_]: Monad] extends EndpointAuth[Req, Resp, F] {
    def processOAuthCallback(request: Req): F[Resp]

    def processLogout(): Resp
  }

  trait Api[Req, Resp, F[_] : Monad] extends EndpointAuth[Req, Resp, F]
}

