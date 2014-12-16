package controllers

import com.amazonaws.auth.{BasicAWSCredentials, AWSCredentials}
import com.amazonaws.regions.Region
import com.gu.pandomainauth.action.AuthActions
import com.gu.pandomainauth.model.AuthenticatedUser

trait ExampleAuthActions extends AuthActions {

  import play.api.Play.current
  lazy val config = play.api.Play.configuration

  override def validateUser(authedUser: AuthenticatedUser): Boolean = {
    println(authedUser.toString)
    (authedUser.user.email endsWith ("@guardian.co.uk")) && authedUser.multiFactor
  }

  override def authCallbackUrl: String = config.getString("host").get + "/oathCallback"

  override lazy val domain: String = "local.dev-gutools.co.uk"
  lazy val awsSecretAccessKey: String = config.getString("aws.secret").get
  lazy val awsKeyId: String = config.getString("aws.keyId").get

  override lazy val awsCredentials: Option[AWSCredentials] = Some(new BasicAWSCredentials(awsKeyId, awsSecretAccessKey))

  /**
   * the aws region the configuration bucket is in, defaults to eu-west-1 as that's where the guardian tends to run stuff
   * @return
   */
  override def awsRegion: Option[Region] = super.awsRegion

  override lazy val system: String = "example"
}
