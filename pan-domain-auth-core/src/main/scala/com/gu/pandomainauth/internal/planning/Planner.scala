package com.gu.pandomainauth.internal.planning

import com.gu.pandomainauth.PageRequest

/**
 * Converts a generic Panda-[[PageRequest]] into a [[Plan]] - specifying how Panda believes
 * the response should be handled.
 */
trait Planner[RespType, RespMod] {
  def planFor(request: PageRequest): Plan[RespType, RespMod]
}

/**
 * Converts a generic Panda-[[PageRequest]] into a [[Plan]], specifically using the _authentication status_
 * of the request.
 */
class AuthPlanner[RespType, RespMod](authStatusHandler: AuthStatusHandler[RespType, RespMod])(implicit
  authStatusFromRequest: AuthStatusFromRequest
) extends Planner[RespType, RespMod] {
  def planFor(request: PageRequest): Plan[RespType, RespMod] =
    authStatusHandler.planForAuthStatus(request.requestUrl, request.authenticationStatus())
}
