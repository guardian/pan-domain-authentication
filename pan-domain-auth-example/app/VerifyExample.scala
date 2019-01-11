import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.gu.pandomainauth.model.AuthenticatedUser
import com.gu.pandomainauth.{PanDomain, PublicSettings}

object VerifyExample {
  // Change this to point to the S3 bucket and key for the settings file
  val settingsFileKey = "local.dev-gutools.co.uk.settings.public"
  val bucketName = "pan-domain-auth-settings"

  val region = Regions.EU_WEST_1
  // Customise as appropriate depending on how you manage your AWS credentials
  val credentials = DefaultAWSCredentialsProviderChain.getInstance()
  val s3Client = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(credentials).build()

  val publicSettings = new PublicSettings(settingsFileKey, bucketName, s3Client)

  // Call the start method when your application starts up to ensure the settings are kept refreshed
  publicSettings.start()

  // `publicKey` will return None if a value has not been successfully obtained
  val publicKey = publicSettings.publicKey.get

  // The name of this particular application
  val system = "test"

  // Adding a grace period allows for XHR requests to continue for a period if there a delay between a user expiring and
  // their re-authentication with the OAuth provider, especially if this is done by inserting an iframe client-side.
  val apiGracePeriod = 0

  // To verify, call the authStatus method with the encoded cookie data
  // The `PanDomain.guardianValidation` helper should be used for Guardian apps
  def validateUser(authUser: AuthenticatedUser): Boolean = {
    authUser.user.emailDomain == "test.com" && authUser.multiFactor
  }

  // If your validateUser function is expensive (hitting a database perhaps) then setting cacheValidation to true will
  // only call the function when authentication is performed for the first time on this system. In other cases it should
  // be false.
  val cacheValidation = false

  PanDomain.authStatus("<<cookie data>>>", publicKey, validateUser, apiGracePeriod, system, cacheValidation)
}
