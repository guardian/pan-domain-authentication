package controllers

import com.gu.pandomainauth.action.AuthActions
import com.gu.pandomainauth.model.AuthenticatedUser

trait ExampleAuthActions extends AuthActions {

  import play.api.Play.current
  lazy val config = play.api.Play.configuration

  override def validateUser(authedUser: AuthenticatedUser): Boolean = {
    println(authedUser.toString)
    (authedUser.user.email endsWith ("@guardian.co.uk")) && authedUser.multiFactor
  }

  override def redirectUrl: String = config.getString("host").get + "/oathCallback"

  override lazy val domain: String = "local.dev-gutools.co.uk"
  override lazy val awsSecretAccessKey: String = config.getString("aws.secret").get
  override lazy val awsKeyId: String = config.getString("aws.keyId").get
  override lazy val system: String = "example"
}
