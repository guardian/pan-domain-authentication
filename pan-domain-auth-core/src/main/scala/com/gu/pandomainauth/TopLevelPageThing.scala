package com.gu.pandomainauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.ApiResponse.DisallowApiAccess
import com.gu.pandomainauth.PageResponse.{NotAuthorized, Redirect}
import com.gu.pandomainauth.model.*
import com.gu.pandomainauth.oauth.*
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

    (plan.typ match {
      case AllowAccess(user) => produceResultGivenAuthedUser(user)
      case withholdAccess: AuthResponseType with WithholdAccess => F.pure(handleWithholdAccess(withholdAccess))
    }).map(modifyResponseWith(plan.responseModification))
  }

  def handleWithholdAccess(pandaResp: AuthResponseType with WithholdAccess): Resp
}

case class PagePlanners[F[_]: Monad](
  auth: AuthPlanner[PageResponse],
  oAuthCallback: OAuthCallbackPlanner[F]
) {
  // check that cookieResponses is the same on both auth & oAuthCallback ?

  val cookieResponses: CookieResponses = oAuthCallback.cookieResponses
}

object PagePlanners {
  def apply[F[_]: Monad](
    cookieResponses: CookieResponses,
    oAuth: OAuthInteractions[F],
    system: String
  )(implicit authStatus: AuthStatusFromRequest): PagePlanners[F] = PagePlanners(
    new AuthPlanner[PageResponse](new PageRequestHandlingStrategy[F](system, cookieResponses, oAuth.providerUrl)),
    new OAuthCallbackPlanner(system, cookieResponses, oAuth.codeToUser)
  )

  def apply[F[_]: Monad](
    settingsRefresher: PanDomainAuthSettingsRefresher,
    appSpecifics: OAuthInteractions.AppSpecifics[F]
  )(implicit authStatus: AuthStatusFromRequest): PagePlanners[F] = PagePlanners(
    CookieResponses(settingsRefresher),
    OAuthInteractions(settingsRefresher.system, settingsRefresher.settings.oAuthSettings, appSpecifics),
    settingsRefresher.system
  )
}

class TopLevelPageThing[Req: PageRequestAdapter, Resp, F[_]: Monad](
  pagePlanners: PagePlanners[F],
  responseAdapter: WebFrameworkAdapter.PageResponseAdapter[Resp],
  logoutResponse: Resp
) extends TopLevelAuthThing[Req, Resp, PageResponse, F](pagePlanners.auth, responseAdapter) {

  override def handleWithholdAccess(pandaResp: PageResponse with WithholdAccess): Resp = pandaResp match {
    case NotAuthorized(user) => responseAdapter.handleNotAuthorised(user)
    case Redirect(uri) => responseAdapter.handleRedirect(uri)
  }

  def processOAuthCallback(request: Req): F[Resp] = for {
    plan <- pagePlanners.oAuthCallback.processOAuthCallback(request.asPandaRequest)
  } yield modifyResponseWith(plan.responseModification)(plan.typ match {
    case NotAuthorized(authenticatedUser) => responseAdapter.handleNotAuthorised(authenticatedUser)
    case Redirect(uri) => responseAdapter.handleRedirect(uri)
  })

  def processLogout(): Resp =
    modifyResponseWith(pagePlanners.cookieResponses.processLogout)(logoutResponse)
}

class TopLevelApiThing[Req: PageRequestAdapter, Resp, F[_]: Monad](
  authPlanner: AuthPlanner[ApiResponse],
  responseAdapter: WebFrameworkAdapter.ApiResponseAdapter[Resp]
) extends TopLevelAuthThing[Req, Resp, ApiResponse, F](authPlanner, responseAdapter) {

  override def handleWithholdAccess(pandaResp: ApiResponse with WithholdAccess): Resp = pandaResp match {
    case disallow: DisallowApiAccess => responseAdapter.handleDisallow(disallow.statusCode)
  }
}

object TopLevelApiThing {
  def apply[Req: PageRequestAdapter, Resp, F[_]: Monad](responseAdapter: WebFrameworkAdapter.ApiResponseAdapter[Resp])(
    implicit authStatusFromRequest: AuthStatusFromRequest
  ) = new TopLevelApiThing[Req, Resp, F](new AuthPlanner[ApiResponse](ApiRequestHandlingStrategy), responseAdapter)
}