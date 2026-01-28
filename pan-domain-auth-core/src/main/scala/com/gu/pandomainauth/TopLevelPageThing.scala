package com.gu.pandomainauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.internal.planning.OAuthCallbackEndpoint.BadRequest
import com.gu.pandomainauth.internal.planning.{AuthPlanner, AuthStatusFromRequest, OAuthCallbackEndpoint, PageEndpoint, PageResponse, Redirect, WithholdAccess}
import com.gu.pandomainauth.oauth.*
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter.*


case class PagePlanners[F[_] : Monad](
  auth: AuthPlanner[PageEndpoint.RespType, PageEndpoint.RespMod],
  oAuthCallback: OAuthCallbackPlanner[F]
)

object PagePlanners {
  def apply[F[_] : Monad](
                           oAuth: OAuthInteractions[F]
                         )(implicit authStatus: AuthStatusFromRequest): PagePlanners[F] = PagePlanners(
    new AuthPlanner(new PageEndpointAuthStatusHandler(oAuth.providerUrl)),
    new OAuthCallbackPlanner(oAuth.codeToUser)
  )

  def apply[F[_] : Monad](
                           settingsRefresher: PanDomainAuthSettingsRefresher,
                           appSpecifics: OAuthInteractions.AppSpecifics[F]
                         )(implicit authStatus: AuthStatusFromRequest): PagePlanners[F] =
    PagePlanners(OAuthInteractions(settingsRefresher.system, settingsRefresher.settings.oAuthSettings, appSpecifics))
}

class TopLevelPageThing[Req: RequestAdapter, Resp, F[_] : Monad](
  pagePlanners: PagePlanners[F],
  responseAdapter: WebFrameworkAdapter.PageResponseAdapter[Resp],
  cookieResponses: CookieResponses,
  logoutResponse: Resp
) extends TopLevelAuthThing[Req, PageEndpoint.RespType, PageEndpoint.RespMod, Resp, F](cookieResponses.pageEndpoint, pagePlanners.auth, responseAdapter)
  with EndpointAuth.Page[Req, Resp, F] {

  override def handleWithholdAccess(pandaResp: PageEndpoint.RespType with WithholdAccess): Resp = pandaResp match {
    case com.gu.pandomainauth.internal.planning.NotAuthorized(user) => responseAdapter.handleNotAuthorised(user)
    case Redirect(uri) => responseAdapter.handleRedirect(uri)
  }

  def processOAuthCallback(request: Req): F[Resp] = for {
    plan <- pagePlanners.oAuthCallback.planFor(request.asPandaRequest)
  } yield {
    val resp = plan.respType match {
      case com.gu.pandomainauth.internal.planning.NotAuthorized(authenticatedUser) => responseAdapter.handleNotAuthorised(authenticatedUser)
      case Redirect(uri) => responseAdapter.handleRedirect(uri)
      case BadRequest => ???
    }
    
    plan.respMod.fold(resp)(respMod => modify(cookieResponses.oAuthCallbackEndpoint(respMod))(resp))
  }
  
  def processLogout(): Resp = modifyResponseWith(PageEndpoint.Logout)(logoutResponse)

}



