package com.gu.pandomainauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.ApiResponse.DisallowApiAccess
import com.gu.pandomainauth.PageResponse.{NotAuthorized, Redirect}
import com.gu.pandomainauth.model.*
import com.gu.pandomainauth.oauth.OAuthCallbackPlanner
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter.*

class AuthPlanner[AuthResponseType <: AuthedEndpointResponse](authStatusHandler: AuthStatusHandler[AuthResponseType])(implicit
  authStatusFromRequest: AuthStatusFromRequest
) {
  def planFor(request: PageRequest): Plan[AuthResponseType] =
    authStatusHandler.planForAuthStatus(request.authenticationStatus())
}

abstract class TopLevelAuthThing[Req: PageRequestAdapter, Resp, AuthResponseType <: AuthedEndpointResponse, F[_]: Monad](
  authPlanner: AuthPlanner[AuthResponseType],
  responseAdapter: WebFrameworkAdapter.ResponseAdapter[Resp]
) {
  val F: Monad[F] = Monad[F]

  def modifyResponseWith(responseModification: ResponseModification): Resp => Resp =
    responseAdapter.responseModifier.apply(responseModification)

  def authenticateRequest(request: Req)(produceResultGivenAuthedUser: User => F[Resp]): F[Resp] = {
    val plan = authPlanner.planFor(request.asPandaRequest)

    handleAuthResponse(plan.typ)(produceResultGivenAuthedUser).map(modifyResponseWith(plan.responseModification))
  }

  def handleAuthResponse(pandaResp: AuthResponseType)(produceResultGivenAuthedUser: User => F[Resp]): F[Resp] = pandaResp match {
    case AllowAccess(user) => produceResultGivenAuthedUser(user)
    case withholdAccess: AuthResponseType with WithholdAccess => F.pure(handleWithholdAccess(withholdAccess))
  }

  def handleWithholdAccess(pandaResp: AuthResponseType with WithholdAccess): Resp
}

class TopLevelPageThing[Req: PageRequestAdapter, Resp, F[+_]: Monad](
  authPlanner: AuthPlanner[PageResponse],
  oAuthCallbackPlanner: OAuthCallbackPlanner[F],
  responseAdapter: WebFrameworkAdapter.PageResponseAdapter[Resp],
  logoutResponse: Resp
) extends TopLevelAuthThing[Req, Resp, PageResponse, F](authPlanner, responseAdapter) {

  override def handleWithholdAccess(pandaResp: PageResponse with WithholdAccess): Resp = pandaResp match {
    case NotAuthorized(user) => responseAdapter.handleNotAuthorised(user)
    case Redirect(uri) => responseAdapter.handleRedirect(uri)
  }

  def processOAuthCallback(request: Req): F[Resp] = for {
    plan <- oAuthCallbackPlanner.processOAuthCallback(request.asPandaRequest)
  } yield modifyResponseWith(plan.responseModification)(plan.typ match {
    case NotAuthorized(user) => responseAdapter.handleNotAuthorised(user)
    case Redirect(uri) => responseAdapter.handleRedirect(uri)
  })

  def processLogout(): Resp = modifyResponseWith(oAuthCallbackPlanner.cookieResponses.processLogout)(logoutResponse)
}

class TopLevelApiThing[Req: PageRequestAdapter, Resp, F[_]: Monad](
  authPlanner: AuthPlanner[ApiResponse],
  responseAdapter: WebFrameworkAdapter.ApiResponseAdapter[Resp]
) extends TopLevelAuthThing[Req, Resp, ApiResponse, F](authPlanner, responseAdapter) {

  override def handleWithholdAccess(pandaResp: ApiResponse with WithholdAccess): Resp = pandaResp match {
    case disallow: DisallowApiAccess => responseAdapter.handleDisallow(disallow.statusCode)
  }
}
