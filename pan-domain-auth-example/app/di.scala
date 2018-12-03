import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain}
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

  val awsRegion = "eu-west-1"

  // Change this to point to the S3 bucket containing the settings file
  val bucketName = "pan-domain-auth-settings"

  // Prefer profile credentials over static credentials
  // If you work at the Guardian you can get these credentials from Janus
  val awsCredentialsProvider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("workflow"),
    DefaultAWSCredentialsProviderChain.getInstance()
  )

  val panDomainSettings = new PanDomainAuthSettingsRefresher(
    domain = "local.dev-gutools.co.uk",
    system = "example",
    bucketName = bucketName,
    actorSystem = actorSystem,
    awsRegion = awsRegion,
    awsCredentialsProvider = awsCredentialsProvider
  )

  val controller = new AdminController(controllerComponents, configuration, wsClient, panDomainSettings)

  def router: Router = new Routes(
    httpErrorHandler,
    controller
  )
}
