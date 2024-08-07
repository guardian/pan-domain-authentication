import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.gu.pandomainauth.S3BucketLoader.forAwsSdkV1
import com.gu.pandomainauth.model.{Authenticated, AuthenticatedUser, GracePeriod}
import com.gu.pandomainauth.{PanDomain, PublicSettings, Settings}

object VerifyExample {
  // Change this to point to the S3 bucket and key for the settings file
  val settingsFileKey = "local.dev-gutools.co.uk.settings.public"
  val bucketName = "pan-domain-auth-settings"

  val region = Regions.EU_WEST_1
  // Customise as appropriate depending on how you manage your AWS credentials
  val credentials = DefaultAWSCredentialsProviderChain.getInstance()
  val s3Client = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(credentials).build()

  val loader = new Settings.Loader(forAwsSdkV1(s3Client, bucketName), settingsFileKey)
  val publicSettings = new PublicSettings(loader)

  // Call the start method when your application starts up to ensure the settings are kept up to date
  publicSettings.start()

  val publicKey = publicSettings.publicKey

  // The name of this particular application
  val system = "test"

  // Adding a grace period allows for XHR requests to continue for a period if there a delay between a user expiring and
  // their re-authentication with the OAuth provider, especially if this is done by inserting an iframe client-side.
  val apiGracePeriod = 0

  // Check the user is valid for your app by inspecting the fields provided
  // The `PanDomain.guardianValidation` helper should be used for Guardian apps
  def validateUser(authUser: AuthenticatedUser): Boolean = {
    authUser.user.emailDomain == "test.com" && authUser.multiFactor
  }

  // If your validateUser function is expensive (hitting a database perhaps) then setting cacheValidation to true will
  // only call the function when authentication is performed for the first time on this system. In other cases it should
  // be false.
  val cacheValidation = false

  // To verify, call the authStatus method with the encoded cookie data
  val status = PanDomain.authStatus("<<cookie data>>>", publicKey, validateUser, apiGracePeriod, system, cacheValidation, forceExpiry = false)

  status match {
    case Authenticated(_) | GracePeriod(_) =>
      // Continue to handle the request

    case _ =>
      // The user is not valid. Reject the request.
      // See `AuthenticationStatus` for the various reasons this can happen.
  }
}
