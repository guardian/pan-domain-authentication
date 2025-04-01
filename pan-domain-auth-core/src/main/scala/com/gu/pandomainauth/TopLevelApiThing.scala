package com.gu.pandomainauth

import cats.Monad
import com.gu.pandomainauth.internal.planning.{ApiEndpoint, AuthPlanner, AuthStatusFromRequest, PageEndpoint, WithholdAccess}
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter.RequestAdapter

class TopLevelApiThing[Req: RequestAdapter, Resp, F[_] : Monad](
  authPlanner: AuthPlanner[ApiEndpoint.RespType, ApiEndpoint.RespMod],
  responseAdapter: WebFrameworkAdapter.ApiResponseAdapter[Resp],
) extends TopLevelAuthThing[Req, ApiEndpoint.RespType, ApiEndpoint.RespMod, Resp, F](
  ApiEndpoint.respModReifier,
  authPlanner,
  responseAdapter
) with EndpointAuth[Req, Resp, F] {

  override def handleWithholdAccess(pandaResp: ApiEndpoint.RespType with WithholdAccess): Resp = pandaResp match {
    case disallow: ApiEndpoint.DisallowApiAccess => responseAdapter.handleDisallow(disallow.statusCode)
  }
}

object TopLevelApiThing {
  def apply[Req: RequestAdapter, Resp, F[_] : Monad](
    responseAdapter: WebFrameworkAdapter.ApiResponseAdapter[Resp]
  )(
    implicit authStatusFromRequest: AuthStatusFromRequest
  ) = new TopLevelApiThing[Req, Resp, F](new AuthPlanner[ApiEndpoint.RespType, ApiEndpoint.RespMod](ApiEndpointAuthStatusHandler), responseAdapter)
}
