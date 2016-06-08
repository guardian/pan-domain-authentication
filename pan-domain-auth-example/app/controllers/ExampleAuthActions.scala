package controllers

import com.amazonaws.auth.{AWSCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.regions.Region
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

  override def authCallbackUrl: String = config.getString("host").get + "/oauthCallback"

  override lazy val domain: String = "local.dev-gutools.co.uk"
  lazy val awsSecretAccessKey: String = config.getString("aws.secret").get
  lazy val awsKeyId: String = config.getString("aws.keyId").get

  override lazy val awsCredentialsProvider: AWSCredentialsProvider = new StaticCredentialsProvider(new BasicAWSCredentials(awsKeyId, awsSecretAccessKey))

  /**
   * the aws region the configuration bucket is in, defaults to eu-west-1 as that's where the guardian tends to run stuff
   * @return
   */
  override def awsRegion: Option[Region] = super.awsRegion

  override lazy val system: String = "example"
}
