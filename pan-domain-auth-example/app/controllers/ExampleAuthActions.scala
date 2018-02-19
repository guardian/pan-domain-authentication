package controllers

import com.gu.pandomainauth.PanDomain
import com.gu.pandomainauth.action.AuthActions
import com.gu.pandomainauth.model.AuthenticatedUser
import play.api.{Configuration, Logger}

trait ExampleAuthActions extends AuthActions {

  def config: Configuration

  override def validateUser(authedUser: AuthenticatedUser): Boolean = {
    Logger.info(s"validating user $authedUser")
    PanDomain.guardianValidation(authedUser)
  }

  override def cacheValidation = false

  override def authCallbackUrl: String = config.get[String]("host") + "/oauthCallback"
}
