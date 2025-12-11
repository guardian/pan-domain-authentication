package com.gu.pandomainauth

import cats.*
import cats.syntax.all.*
import com.gu.pandomainauth.internal.planning.{AuthPlanner, AuthStatusFromRequest, PageEndpoint, PageResponse, Redirect, WithholdAccess}
import com.gu.pandomainauth.model.*
import com.gu.pandomainauth.oauth.*
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter.*



case class PagePlanners[F[_]: Monad](
  auth: AuthPlanner[PageEndpoint.RespType, PageEndpoint.RespMod],
  oAuthCallback: OAuthCallbackPlanner[F]
)

object PagePlanners {
  def apply[F[_]: Monad](
    oAuth: OAuthInteractions[F]
  )(implicit authStatus: AuthStatusFromRequest): PagePlanners[F] = PagePlanners(
    new AuthPlanner(new PageRequestHandlingStrategy[F](oAuth.providerUrl)),
    new OAuthCallbackPlanner(oAuth.codeToUser)
  )

  def apply[F[_]: Monad](
    settingsRefresher: PanDomainAuthSettingsRefresher,
    appSpecifics: OAuthInteractions.AppSpecifics[F]
  )(implicit authStatus: AuthStatusFromRequest): PagePlanners[F] =
    PagePlanners(OAuthInteractions(settingsRefresher.system, settingsRefresher.settings.oAuthSettings, appSpecifics))
}

class TopLevelPageThing[Req: RequestAdapter, Resp, F[_]: Monad](
  pagePlanners: PagePlanners[F],
  responseAdapter: WebFrameworkAdapter.PageResponseAdapter[Resp],
  cookieResponses: CookieResponses,
  logoutResponse: Resp
) extends TopLevelAuthThing[Req, PageEndpoint.RespType, PageEndpoint.RespMod, PageResponse, F](cookieResponses, pagePlanners.auth, responseAdapter) {

  override def handleWithholdAccess(pandaResp: PageResponse with WithholdAccess): Resp = pandaResp match {
    case NotAuthorized(user) => responseAdapter.handleNotAuthorised(user)
    case Redirect(uri) => responseAdapter.handleRedirect(uri)
  }

  def processOAuthCallback(request: Req): F[Resp] = for {
    plan <- pagePlanners.oAuthCallback.processOAuthCallback(request.asPandaRequest)
  } yield modifyResponseWith(plan.respMod)(plan.respType match {
    case PageEndpoint.NotAuthorized(authenticatedUser) => responseAdapter.handleNotAuthorised(authenticatedUser)
    case PageEndpoint.Redirect(uri) => responseAdapter.handleRedirect(uri)
  })

  def processLogout(): Resp =
    modifyResponseWith(ResponseModification(PageEndpoint.Logout))(logoutResponse)
}



