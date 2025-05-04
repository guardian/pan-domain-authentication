package com.gu.pandomainauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.ApiResponse.{AllowAccess, DisallowApiAccess}
import com.gu.pandomainauth.PageResponse.{AllowAccess, NotAuthorized, Redirect}
import com.gu.pandomainauth.model.*
import com.gu.pandomainauth.oauth.OAuthCallbackPlanner
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter.*

class AuthPlanner[AuthResponseType](authStatusHandler: AuthStatusHandler[AuthResponseType])(implicit
  authStatusFromRequest: AuthStatusFromRequest
) {
  def planFor(request: PageRequest): Plan[AuthResponseType] =
    authStatusHandler.planForAuthStatus(request.authenticationStatus())
}

abstract class TopLevelAuthThing[Req: PageRequestAdapter, Resp, AuthResponseType, F[_]: Monad](
  authPlanner: AuthPlanner[AuthResponseType],
  responseAdapter: WebFrameworkAdapter.ResponseAdapter[Resp]
) {
  val F: Monad[F] = Monad[F]

  def modifyResponseWith(responseModification: ResponseModification): Resp => Resp =
    responseAdapter.responseModifier.apply(responseModification)

  def authenticateRequest(request: Req)(produceResultGivenAuthedUser: User => F[Resp]): F[Resp] = {
    val plan = authPlanner.planFor(request.asPandaRequest)

    poop(plan.typ)(produceResultGivenAuthedUser).map(modifyResponseWith(plan.responseModification))
  }

  def poop(pandaResp: AuthResponseType)(produceResultGivenAuthedUser: User => F[Resp]): F[Resp]
}

class TopLevelPageThing[Req: PageRequestAdapter, Resp: WebFrameworkAdapter.PageResponseAdapter, F[+_]: Monad](
  authPlanner: AuthPlanner[PageResponse],
  oAuthCallbackPlanner: OAuthCallbackPlanner[F],
  responseAdapter: WebFrameworkAdapter.PageResponseAdapter[Resp],
  logoutResponse: Resp
) extends TopLevelAuthThing[Req, Resp, PageResponse, F](authPlanner, responseAdapter) {

  def poop(pandaResp: PageResponse)(produceResultGivenAuthedUser: User => F[Resp]): F[Resp] = pandaResp match {
    case PageResponse.AllowAccess(user) => produceResultGivenAuthedUser(user)
    case NotAuthorized(user) => F.pure(responseAdapter.handleNotAuthorised(user))
    case Redirect(uri) => F.pure(responseAdapter.handleRedirect(uri))
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

  override def poop(pandaResp: ApiResponse)(produceResultGivenAuthedUser: User => F[Resp]): F[Resp] = pandaResp match {
    case ApiResponse.AllowAccess(user) => produceResultGivenAuthedUser(user)
    case disallow: DisallowApiAccess => F.pure(responseAdapter.handleDisallow(disallow.statusCode))
  }
}
