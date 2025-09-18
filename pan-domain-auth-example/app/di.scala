import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import com.gu.pandomainauth.PanDomainAuthSettingsRefresher
import com.gu.pandomainauth.S3BucketLoader.forAwsSdkV2
import controllers.AdminController
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext}
import play.filters.HttpFiltersComponents
import router.Routes
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider

class AppLoader extends ApplicationLoader {
  def load(context: Context): Application = {
    new AppComponents(context).application
  }
}

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context)
  with AhcWSComponents
  with HttpFiltersComponents {

  // Change this to point to the S3 bucket containing the settings file
  val bucketName = "pan-domain-auth-settings"

  val region = Region.EU_WEST_1

  // Customise as appropriate depending on how you manage your AWS credentials
  val credentials: AwsCredentialsProviderChain =
    AwsCredentialsProviderChain
      .builder()
      .credentialsProviders(
        ProfileCredentialsProvider.create("workflow"),
        DefaultCredentialsProvider.builder().build()
      )
      .build()

  val s3Client = S3Client.builder().region(region).credentialsProvider(credentials).build()

  val panDomainSettings = PanDomainAuthSettingsRefresher(
    domain = "local.dev-gutools.co.uk",
    system = "example",
    s3BucketLoader = forAwsSdkV2(s3Client, bucketName)
  )

  val controller = new AdminController(controllerComponents, configuration, wsClient, panDomainSettings)

  def router: Router = new Routes(
    httpErrorHandler,
    controller
  )
}
