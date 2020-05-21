package controllers

import com.gu.pandomainauth.PanDomain
import com.gu.pandomainauth.action.AuthActions
import com.gu.pandomainauth.model.AuthenticatedUser
import org.slf4j.LoggerFactory
import play.api.{Configuration}

trait ExampleAuthActions extends AuthActions {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def config: Configuration

  override def validateUser(authedUser: AuthenticatedUser): Boolean = {
    logger.info(s"validating user $authedUser")
    PanDomain.guardianValidation(authedUser)
  }

  /**
    * By default, the user validation method is called every request. If your validation
    * method has side-effects or is expensive (perhaps hitting a database), setting this
    * to true will ensure that validateUser is only called when the OAuth session is refreshed
    */
  override def cacheValidation = false

  override def authCallbackUrl: String = "https://" + config.get[String]("host") + "/oauthCallback"
}
