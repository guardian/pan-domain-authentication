package controllers

import com.gu.pandomainauth.PanDomainAuthSettingsRefresher
import play.api.Configuration
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.libs.ws.WSClient


class AdminController(
  override val controllerComponents: ControllerComponents,
  override val config: Configuration,
  override val wsClient: WSClient,
  override val panDomainSettings: PanDomainAuthSettingsRefresher
) extends AbstractController(controllerComponents) with ExampleAuthActions {

  def index = Action{Ok("hello")}

  def showUser = AuthAction { req =>
    Ok(req.user.toString)
  }

  def oauthCallback = Action.async { implicit request =>
    processOAuthCallback()
  }

  def logout = Action { implicit request =>
    processLogout(request)
  }
}
