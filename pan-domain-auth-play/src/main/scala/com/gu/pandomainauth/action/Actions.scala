package com.gu.pandomainauth.action

import com.gu.pandomainauth.WebFrameworkAdapter.PageRequestAdapter
import com.gu.pandomainauth.model._
import com.gu.pandomainauth.service._
import com.gu.pandomainauth._
import com.gu.pandomainauth.internal.PlayFrameworkAdapter
import org.slf4j.LoggerFactory
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
import play.api.mvc._

import java.net.{URI, URLDecoder, URLEncoder}
import scala.concurrent.{ExecutionContext, Future}

class UserRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

case class PandomainCookie(cookie: Cookie, forceExpiry: Boolean)

trait AuthActions {
  private val logger = LoggerFactory.getLogger(this.getClass)

  trait AuthenticationAction extends ActionBuilder[UserRequest, AnyContent] {

    def authenticateRequest(request: RequestHeader)(produceResultGivenAuthedUser: User => Future[Result]): Future[Result]

    override def invokeBlock[A](request: Request[A], block: (UserRequest[A]) => Future[Result]): Future[Result] =
      authenticateRequest(request)(user => block(new UserRequest(user, request)))

  }

  /**
    * Play application components that you must provide in order to use AuthActions
    */
  def wsClient: WSClient
  def controllerComponents: ControllerComponents
  val panDomainSettings: PanDomainAuthSettingsRefresher


  val pageStrat: PageRequestHandlingStrategy = ???
  val apiStrat: ApiRequestHandlingStrategy = ???
  val pageResponseHandlerFoo = PageResponseHandlerFoo[Result](PlayFrameworkAdapter)

  private lazy val system: String = panDomainSettings.system
  private lazy val domain: String = panDomainSettings.domain

  private def settings: PanDomainAuthSettings = panDomainSettings.settings

  private implicit val ec: ExecutionContext = controllerComponents.executionContext

  /**
    * Returns true if the authed user is valid in the implementing system (meets your multifactor requirements, you recognise the email etc.).
    *
    * If your implementing application needs to audit logins / register new users etc then this ia also the place to do it (although in this case
    * you should strongly consider setting cacheValidation to true).
    *
    * @param authedUser
    * @return true if the user is valid in your app
    */
  def validateUser(authedUser: AuthenticatedUser): Boolean

  /**
    * By default the validity of the user is checked every request. If your validateUser implementation is expensive or has side effects you
    * can override this to true and validity will only be checked the first time the user visits your app after their login is established.
    *
    * Note the the cache is invalidated after the user's session is re-established with the OAuth provider.
    *
    * @return true if you want to only check the validity of the user once for the lifetime of the user's auth session
    */
  def cacheValidation: Boolean = false

  /**
    * The auth callback url. This is where the OAuth provider will send the user after authentication.
    * This action on should invoke processOAuthCallback
    *
    * @return
    */
  def authCallbackUrl: String

  lazy val OAuth = new OAuth(settings.oAuthSettings, system, authCallbackUrl)(ec, wsClient)

  /**
    * Application name used for initialising Google API clients for directory group checking
    */
  lazy val applicationName: String = s"pan-domain-authentication-$system"

  lazy val multifactorChecker: Option[Google2FAGroupChecker] = settings.google2FAGroupSettings.map {
    new Google2FAGroupChecker(_, panDomainSettings.s3BucketLoader, applicationName)
  }

//  /**
//    * starts the authentication process for a user. By default, this just sends the user off to the OAuth provider for auth
//    * but if you want to show welcome page with a button on it then override.
//    */
//  def sendForAuth(implicit request: RequestHeader, email: Option[String] = None) = {
//    val antiForgeryToken = OAuth.generateAntiForgeryToken()
//    OAuth.redirectToOAuthProvider(antiForgeryToken, email)(ec) map { res =>
//      val originUrl = request.uri
//      res.withCookies(cookie(ANTI_FORGERY_KEY, antiForgeryToken), cookie(LOGIN_ORIGIN_KEY, originUrl))
//    }
//  }

  def checkMultifactor(authedUser: AuthenticatedUser) = multifactorChecker.exists(_.checkMultifactor(authedUser))

  /**
    * invoked when the user is not logged in a can't be authed - this may be when the user is not valid in yur system
    * or when they have exoplicitly logged out.
    *
    * Override this to add a logged out screen and display maeesages for your app. The default implementation is
    * to ust return a 403 response
    *
    * @param message
    * @param request
    * @return
    */
  def showUnauthedMessage(message: String): Result = {
    logger.info(message)
    Forbidden
  }

  /**
    * Generates the message shown to the user when user validation fails. override this to add a custom error message
    *
    * @param claimedAuth
    * @return
    */
  def invalidUserMessage(user: com.gu.pandomainauth.model.User) = s"user ${user.email} not valid for $system"

  private def decodeCookie(name: String)(implicit request: RequestHeader) =
    request.cookies.get(name).map(cookie => URLDecoder.decode(cookie.value, "UTF-8"))

  def processOAuthCallback()(implicit request: RequestHeader): Future[Result] = {
    val pageRequestAdapter: WebFrameworkAdapter.PageRequestAdapter[RequestHeader] = ???
    val pageRequest = pageRequestAdapter.transform(request)
    for {
      pandaResponse <- pageStrat.processOAuthCallback(pageRequest)
    } yield {
      pageResponseHandlerFoo.handle(pandaResponse)
    }
  }

  def processLogout(implicit request: RequestHeader) = {
    flushCookie(showUnauthedMessage("logged out"))
  }

  def readAuthenticatedUser(request: RequestHeader): Option[AuthenticatedUser] = readCookie(request) flatMap { cookie =>
    CookieUtils.parseCookieData(cookie.cookie.value, settings.signingAndVerification).toOption
  }

  def readCookie(request: RequestHeader): Option[PandomainCookie] = {
    request.cookies.get(settings.cookieSettings.cookieName).map { cookie =>
      val forceExpiry = request.cookies.get(PageRequestHandlingStrategy.FORCE_EXPIRY_KEY).exists(_.value != "0")
      PandomainCookie(cookie, forceExpiry)
    }
  }

  def generateCookie(authedUser: AuthenticatedUser): Cookie = Cookie(
    name = settings.cookieSettings.cookieName,
    value = CookieUtils.generateCookieData(authedUser, settings.signingAndVerification),
    domain = Some(domain),
    secure = true,
    httpOnly = true
  )

  def flushCookie(result: Result): Result = {
    val clearCookie = DiscardingCookie(
      name = settings.cookieSettings.cookieName,
      domain = Some(domain),
      secure = true
    )
    result.discardingCookies(clearCookie)
  }

  /**
    * Extract the authentication status from the request.
    */
  def extractAuth(request: RequestHeader): AuthenticationStatus = {
    readCookie(request).map { cookie =>
      PanDomain.authStatus(cookie.cookie.value, settings.signingAndVerification, validateUser, system, cacheValidation, cookie.forceExpiry)
    } getOrElse NotAuthenticated
  }

  /**
    * Action that ensures the user is logged in and validated.
    *
    * This action is for page load type requests where it is possible to send the user for auth
    * and for them to interact with the auth provider. For API / XHR type requests use the APIAuthAction
    *
    * if the user is not authed or the auth has expired they are sent for authentication
    */
  object AuthAction extends AuthenticationAction {

    override def parser: BodyParser[AnyContent]               = AuthActions.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext = AuthActions.this.controllerComponents.executionContext

    def authenticateRequest(request: RequestHeader)(produceResultGivenAuthedUser: User => Future[Result]): Future[Result] = {
      val authStatus: AuthenticationStatus = ???
      val pageRequest = PageRequest(
        URI.create(request.uri),
        request.cookies.map(c => c.name -> c.value).toMap
      )
      val pandaResponse = pageStrat.authenticateRequest(authStatus)
      pageResponseHandlerFoo.handle(pandaResponse)(produceResultGivenAuthedUser)
    }
  }

  /**
    * Action that ensures the user is logged in and validated.
    *
    * This action is for API / XHR type requests where the user can't be sent to the auth provider for auth. In the
    * cases where the auth is not valid response codes are sent to the requesting app and the javascript that initiated
    * the request should handle these appropriately
    *
    * If the user is not authed then a 401 response is sent, if the auth has expired then a 419 response is sent, if
    * the user is authed but not allowed to perform the action a 403 is sent
    *
    * If the user is authed or has an expiry extension, a 200 is sent
    *
    */
  object APIAuthAction extends AbstractApiAuthAction

  /**
    * Abstraction for API auth actions allowing to mix in custom results for each of the different error scenarios.
    */
  trait AbstractApiAuthAction extends AuthenticationAction {

    override def parser: BodyParser[AnyContent]               = AuthActions.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext = AuthActions.this.controllerComponents.executionContext

    val apiResponseHandlerFoo = ApiResponseHandlerFoo[Result](PlayFrameworkAdapter)

    def authenticateRequest(request: RequestHeader)(produceResultGivenAuthedUser: User => Future[Result]): Future[Result] = {
      val authStatus: AuthenticationStatus = ???
      val pandaResponse = apiStrat.authenticateRequest(authStatus)
      apiResponseHandlerFoo.handle(pandaResponse)(produceResultGivenAuthedUser)
    }
  }
}
