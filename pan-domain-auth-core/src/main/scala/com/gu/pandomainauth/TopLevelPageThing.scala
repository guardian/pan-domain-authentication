package com.gu.pandomainauth

import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter.*
import com.gu.pandomainauth.model.*
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter

import scala.concurrent.{ExecutionContext, Future}

class TopLevelAuthThing[Req: PageRequestAdapter, Resp, PandaRespType](
  authenticationStatusHandler: AuthStatusHandler[PandaRespType],
  responseHandler: ResponseHandler[PandaRespType, Resp]
)(implicit
  authStatusFromRequest: AuthStatusFromRequest
) {
  def planFor(request: Req): Plan[PandaRespType] =
    authenticationStatusHandler.planForAuthStatus(request.asPandaRequest.authenticationStatus())

  def authenticateRequest(request: Req)(produceResultGivenAuthedUser: User => Future[Resp])(implicit ec: ExecutionContext): Future[Resp] =
    responseHandler.handle(planFor(request))(produceResultGivenAuthedUser)
}

class TopLevelPageThing[Req: PageRequestAdapter, Resp: WebFrameworkAdapter.PageResponseAdapter](
  requestHandlingStrategy: PageRequestHandlingStrategy,
  pageResponseHandler: PageResponseHandler[Resp],
  logoutResponse: Resp
)(implicit
  authStatusFromRequest: AuthStatusFromRequest
) extends TopLevelAuthThing[Req, Resp, PageResponse](requestHandlingStrategy, pageResponseHandler) {

  def processOAuthCallback(request: Req)(implicit ec: ExecutionContext): Future[Resp] = for {
    pandaResponse <- requestHandlingStrategy.processOAuthCallback(request.asPandaRequest)
  } yield pageResponseHandler.handle(pandaResponse)

  def processLogout(): Resp = pageResponseHandler.modifyResponseWith(requestHandlingStrategy.processLogout)(logoutResponse)
}

class TopLevelApiThing[Req: PageRequestAdapter, Resp](
  apiResponseHandler: ApiResponseHandler[Resp]
)(implicit
  authStatusFromRequest: AuthStatusFromRequest
) extends TopLevelAuthThing[Req, Resp, ApiResponse](ApiRequestHandlingStrategy, apiResponseHandler)
