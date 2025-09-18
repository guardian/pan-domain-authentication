import software.amazon.awssdk.regions.Region
import com.gu.pandomainauth.S3BucketLoader.forAwsSdkV2
import com.gu.pandomainauth.model.{Authenticated, AuthenticatedUser, GracePeriod}
import com.gu.pandomainauth.service.CryptoConf
import com.gu.pandomainauth.{PanDomain, PublicSettings, Settings}

import java.time.Duration
import software.amazon.awssdk.services.s3.S3Client

object VerifyExample {
  // Change this to point to the S3 bucket and key for the settings file
  val settingsFileKey = "local.dev-gutools.co.uk.settings.public"
  val bucketName = "pan-domain-auth-settings"

  val region = Region.EU_WEST_1
  // Customise as appropriate depending on how you manage your AWS credentials
  val s3Client = S3Client
      .builder()
      .region(region)
      .build()

  val loader = new Settings.Loader(forAwsSdkV2(s3Client, bucketName), settingsFileKey)
  val publicSettings = PublicSettings(loader)

  // Call the start method when your application starts up to ensure the settings are kept up to date
  publicSettings.start()

  val verification: CryptoConf.Verification = publicSettings.verification

  // The name of this particular application
  val system = "test"

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
  val status = PanDomain.authStatus("<<cookie data>>>", verification, validateUser, system, cacheValidation, forceExpiry = false)

  status match {
    case Authenticated(_) | GracePeriod(_) =>
      // Continue to handle the request

    case _ =>
      // The user is not valid. Reject the request.
      // See `AuthenticationStatus` for the various reasons this can happen.
  }
}
