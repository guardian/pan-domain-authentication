package controllers

import com.gu.pandomainauth.PanDomainAuthSettingsRefresher
import play.api.Configuration
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.api.libs.ws.WSClient


class AdminController(
  override val controllerComponents: ControllerComponents,
  override val config: Configuration,
  override val wsClient: WSClient,
  override val panDomainSettings: PanDomainAuthSettingsRefresher
) extends AbstractController(controllerComponents) with ExampleAuthActions {

  // No authentication
  def index: Action[AnyContent] = Action {
    Ok("hello")
  }

  // This is a normal user-interactive request that will redirect to the OAuth provider
  // to re-negotiate a login on expiry.
  def showUser: Action[AnyContent] = AuthAction { req =>
    // The user information is available as a field on the request
    Ok(req.user.toString)
  }

  // This is a request that is issued from JS. If the user has expired it will return an
  // error code that can be handled by the front-end webapp.
  def showUserApi: Action[AnyContent] = APIAuthAction { req =>
    Ok(req.user.toString)
  }

  // Shows how to use the engineers group check, e.g. to decide whether to render a "you are in PROD" banner.
  // This needs `engineersGroupId` (and the other Google properties) in the domain's settings file.
  def showProdWarning: Action[AnyContent] = AuthAction { req =>
    // isEngineer returns Either[MembershipCheckError, Boolean]: a Left means the lookup failed, i.e. "unknown".
    // For a banner we treat unknown as "don't show"
    val isEngineer = googleGroupChecker
      .flatMap(_.isEngineer(req.user.email).toOption)
      .contains(true)

    Ok(if (isEngineer) "You are an engineer: show the PROD warning" else "Not an engineer: no PROD warning")
  }

  // Required to allow the provider to redirect back to us so we can issue the new cookie
  // This route must be added to the provider whitelist
  def oauthCallback: Action[AnyContent] = Action.async { implicit request =>
    processOAuthCallback()
  }

  // Note: this is potentially confusing depending on your use-case as currently only the
  // panda cookie is removed and the user is not logged out of the OAuth provider
  def logout: Action[AnyContent] = Action { implicit request =>
    processLogout(request)
  }
}
