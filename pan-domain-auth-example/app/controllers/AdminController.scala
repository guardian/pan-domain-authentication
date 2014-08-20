package controllers

import com.gu.pandomainauth.PanDomainAuth
import com.gu.pandomainauth.model.{User, AuthenticatedUser}
import com.gu.pandomainauth.service.CookieUtils
import models.BannedUsers
import org.joda.time.DateTime
import play.api.mvc.{Cookie, Action, Controller}
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
}
