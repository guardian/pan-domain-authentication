import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.gu.pandomainauth.PanDomainAuthSettingsRefresher
import controllers.AdminController
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext}
import play.filters.HttpFiltersComponents
import router.Routes

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

  val region = Regions.EU_WEST_1
  // Customise as appropriate depending on how you manage your AWS credentials
  val credentials = DefaultAWSCredentialsProviderChain.getInstance()
  val s3Client = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(credentials).build()

  val panDomainSettings = new PanDomainAuthSettingsRefresher(
    domain = "local.dev-gutools.co.uk",
    system = "example",
    bucketName = bucketName,
    settingsFileKey = "local.dev-gutools.co.uk.settings",
    s3Client = s3Client
  )

  val controller = new AdminController(controllerComponents, configuration, wsClient, panDomainSettings)

  def router: Router = new Routes(
    httpErrorHandler,
    controller
  )
}
