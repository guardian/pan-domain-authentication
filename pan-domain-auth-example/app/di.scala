import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.internal.StaticCredentialsProvider
import com.gu.pandomainauth.PanDomainAuthSettingsRefresher
import controllers.AdminController
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext}
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
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

  val awsCredentialsProvider = new StaticCredentialsProvider(new BasicAWSCredentials(
    configuration.get[String]("aws.keyId"),
    configuration.get[String]("aws.secret")
  ))

  val panDomainSettings = new PanDomainAuthSettingsRefresher(
    domain = "local.dev-gutools.co.uk",
    system = "example",
    actorSystem = actorSystem,
    awsCredentialsProvider = awsCredentialsProvider
  )

  val controller = new AdminController(controllerComponents, configuration, wsClient, panDomainSettings)

  def router: Router = new Routes(
    httpErrorHandler,
    controller
  )
}
