package controllers

import play.api.mvc.{ Action, Controller}
import play.api.Play.configuration
import play.api.Play.current


object AdminController extends Controller with ExampleAuthActions {

  def index = Action{Ok("hello")}

  def showUser = AuthAction { req =>
    Ok(req.user.toString)
  }

  def oathCallback = Action.async { implicit request =>
    processGoogleCallback()
  }

  def logout = Action { implicit request =>
    processLogout(request)
  }

}
