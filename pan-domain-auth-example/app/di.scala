import controllers.AdminController
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext}
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
import router.Routes

class AppLoader extends ApplicationLoader {
  def load(context: Context): Application = {
    new AppComponents(context).application
  }
}

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with AhcWSComponents {
  val controller = new AdminController(configuration, wsClient)

  def router: Router = new Routes(
    httpErrorHandler,
    controller
  )
}
