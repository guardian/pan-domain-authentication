package controllers

import javax.inject.Inject

import play.api.Configuration
import play.api.mvc.{Action, Controller}
import play.api.Play.configuration
import play.api.Play.current
import play.api.libs.ws.WSClient


class AdminController (override val config: Configuration, override val wsClient: WSClient)
  extends Controller with ExampleAuthActions {

  def index = Action{Ok("hello")}

  def showUser = AuthAction { req =>
    Ok(req.user.toString)
  }

  def oauthCallback = Action.async { implicit request =>
    processGoogleCallback()
  }

  def logout = Action { implicit request =>
    processLogout(request)
  }

}
