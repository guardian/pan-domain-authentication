package com.gu.pandomainauth.action

import com.gu.pandomainauth.*
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter.*
import com.gu.pandomainauth.internal.PlayFrameworkAdapter
import com.gu.pandomainauth.model.*
import com.gu.pandomainauth.oauth.{DiscoveryDocument, OAuthCallbackPlanner, OAuthCodeToUser, OAuthInteractions, OAuthUrl}
import com.gu.pandomainauth.oauth.OAuthCodeToUser.TokenRequestParamsGenerator
import com.gu.pandomainauth.service.*
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter
import org.slf4j.LoggerFactory
import play.api.libs.ws.WSClient
import play.api.mvc.*
import play.api.mvc.Results.*

import java.net.URI
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

  implicit val pageRequestAdapter: WebFrameworkAdapter.PageRequestAdapter[RequestHeader] = (req: RequestHeader) => PageRequest(
    URI.create(req.uri),
    req.cookies.map(c => c.name -> c.value).toMap
  )

  private lazy val system: String = panDomainSettings.system
  private lazy val domain: String = panDomainSettings.domain

  private def settings: PanDomainAuthSettings = panDomainSettings.settings

  implicit val authStatusFromRequest: AuthStatusFromRequest = AuthStatusFromRequest(
    panDomainSettings,
    validateUser = ???,
    cacheValidation = ???
  )

  val pagePlanners: PagePlanners[Future] = PagePlanners(
    panDomainSettings,
    OAuthInteractions.AppSpecifics(new PlayImplOfOAuthHttpClient(wsClient), URI.create(authCallbackUrl))
  )

  val topLevelPageThing: TopLevelPageThing[RequestHeader, Result, Future] =
    new TopLevelPageThing(pagePlanners, PlayFrameworkAdapter, showUnauthedMessage("logged out"))

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

  /**
    * Application name used for initialising Google API clients for directory group checking
    */
  lazy val applicationName: String = s"pan-domain-authentication-$system"

  lazy val multifactorChecker: Option[Google2FAGroupChecker] = settings.google2FAGroupSettings.map {
    new Google2FAGroupChecker(_, panDomainSettings.s3BucketLoader, applicationName)
  }

  def checkMultifactor(authedUser: AuthenticatedUser) = multifactorChecker.exists(_.checkMultifactor(authedUser))

  /**
    * invoked when the user is not logged in a can't be authed - this may be when the user is not valid in yur system
    * or when they have explicitly logged out.
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

  def processOAuthCallback()(implicit request: RequestHeader): Future[Result] =
    topLevelPageThing.processOAuthCallback(request)

  def processLogout(implicit request: RequestHeader) = topLevelPageThing.processLogout()

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

    def authenticateRequest(request: RequestHeader)(produceResultGivenAuthedUser: User => Future[Result]): Future[Result] =
      topLevelPageThing.authenticateRequest(request)(produceResultGivenAuthedUser)
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

    val topLevelApiThing: TopLevelApiThing[RequestHeader, Result, Future] = 
      new TopLevelApiThing[RequestHeader, Result, Future](
        new AuthPlanner[ApiResponse](ApiRequestHandlingStrategy),
        PlayFrameworkAdapter
      )

    def authenticateRequest(request: RequestHeader)(produceResultGivenAuthedUser: User => Future[Result]): Future[Result] =
      topLevelApiThing.authenticateRequest(request)(produceResultGivenAuthedUser)
  }
}
