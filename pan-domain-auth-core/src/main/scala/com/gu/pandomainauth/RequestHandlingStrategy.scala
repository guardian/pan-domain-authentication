package com.gu.pandomainauth

import com.gu.pandomainauth.ApiResponse.{DisallowApiAccess, HttpStatusCode}
import com.gu.pandomainauth.PageRequestHandlingStrategy.{ANTI_FORGERY_KEY, LOGIN_ORIGIN_KEY, TemporaryCookiesUsedForOAuth}
import com.gu.pandomainauth.PageResponse.AllowAccess
import com.gu.pandomainauth.ResponseModification.NoResponseModification
import com.gu.pandomainauth.model._
import com.gu.pandomainauth.oauth.OAuthValidator
import com.gu.pandomainauth.service.CookieUtils.generateCookieData
import com.gu.pandomainauth.service.CryptoConf.Signing
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URIBuilder
import org.apache.http.message.BasicNameValuePair

import java.net.{URI, URLDecoder}
import java.nio.charset.StandardCharsets.UTF_8
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

case class CookieChanges(
  domain: String,
  setSessionCookies: Map[String, String] = Map.empty,
  wipeCookies: Set[String] = Set.empty
)

case class ResponseModification(responseHeaders: Map[String, String] = Map.empty, cookieChanges: Option[CookieChanges] = None)

object ResponseModification {
  val NoResponseModification: ResponseModification = ResponseModification()
}

case class Response[+T](typ: T, responseModification: ResponseModification = NoResponseModification)

sealed trait PageResponse
sealed trait OAuthCallbackResponse

object PageResponse {
  case class AllowAccess(user: User) extends PageResponse // May add system
  case class NotAuthorized(user: User) extends PageResponse with OAuthCallbackResponse
  case class Redirect(uri: URI) extends PageResponse with OAuthCallbackResponse
}

sealed trait ApiResponse

object ApiResponse {
  case class HttpStatusCode(code: Int, message: String)

  trait DisallowApiAccess extends ApiResponse {
    val statusCode: HttpStatusCode
  }

  case class AllowAccess(user: User, suggestCredsRefresh: Boolean = false) extends ApiResponse {
    val header: (String, String) = "X-Panda-Credential-Refresh-Suggested" -> suggestCredsRefresh.toString
  }

  case object NotAuthorized extends DisallowApiAccess {
    val statusCode: HttpStatusCode = HttpStatusCode(403, "User is not authorized to use this tool")
  }

  case object NoAuthentication extends DisallowApiAccess {
    val statusCode: HttpStatusCode = HttpStatusCode(401, "Missing or expired auth cookie, or cookie with an integrity failure") // or 419, if they are expired & not acceptable?
  }
}

object PageRequestHandlingStrategy {
  /**
   * A cookie key that stores the target URL that was being accessed when redirected for authentication
   */
  val LOGIN_ORIGIN_KEY = "panda-loginOriginUrl"
  /*
   * Cookie key containing an anti-forgery token; helps to validate that the oauth callback arrived in response to the correct oauth request
   */
  val ANTI_FORGERY_KEY = "panda-antiForgeryToken"
  /*
   * Cookie that will make panda behave as if the cookie has expired.
   * NOTE: This cookie is for debugging only! It should _not_ be set by any application code to expire the cookie!! Use the `processLogout` action instead!!
   */
  val FORCE_EXPIRY_KEY = "panda-forceExpiry"

  val TemporaryCookiesUsedForOAuth = Set(LOGIN_ORIGIN_KEY, ANTI_FORGERY_KEY, FORCE_EXPIRY_KEY)
}

/*
 * Panda should specify a set of cookie values that it wants to be given.
 *
 * Then it should accept a *map of cookie name to value* (which calling code should have constrained to just
 * the requested ones), along with the origin-url of the request (really, only useful if we're going to redirect)
 */
class PageRequestHandlingStrategy(system: String, domain: String, cookieSettings: CookieSettings, signing: Signing) {

  private def updateCookieToAddSystemIfNecessary(authedUser: AuthenticatedUser): ResponseModification =
    authedUser.requiringAdditional(system).fold(NoResponseModification) { updatedUser => cookieResponseFor(updatedUser) }

  private def cookieResponseFor(user: AuthenticatedUser, wipeTemporaryCookiesUsedForOAuth: Boolean = false) =
    ResponseModification(cookieChanges = Some(CookieChanges(
      domain,
      setSessionCookies = Map(cookieSettings.cookieName -> generateCookieData(user, signing)),
      wipeCookies = if (wipeTemporaryCookiesUsedForOAuth) TemporaryCookiesUsedForOAuth else Set.empty
    )))

  val oAuthUrl: OAuthUrl = ???

  def redirectForAuth(email: Option[String] = None, wipeAuthCookie: Boolean = false): Response[PageResponse] = {
    val antiForgeryToken: String = ???
    Response(PageResponse.Redirect(oAuthUrl.redirectToOAuthProvider(antiForgeryToken, email)),
      ResponseModification(cookieChanges = Some(CookieChanges(
        domain, // Should only the main auth cookie be on the shared domain, while temp OAuth cookies be on the app-specific domain, to avoid clashes?
        setSessionCookies = Map(ANTI_FORGERY_KEY -> antiForgeryToken),
        wipeCookies = if (wipeAuthCookie) Set(cookieSettings.cookieName) else Set.empty
      )))
    )
  }

  def authenticateRequest(authStatus: AuthenticationStatus): Response[PageResponse] = authStatus match {
    case NotAuthenticated =>
      redirectForAuth()
    case InvalidCookie(e) =>
      redirectForAuth(wipeAuthCookie = true)
    case Expired(authedUser) =>
      redirectForAuth(email = Some(authedUser.user.email))
    case GracePeriod(authedUser) =>
      redirectForAuth(email = Some(authedUser.user.email))
    case NotAuthorized(authedUser) =>
      Response(PageResponse.NotAuthorized(authedUser.user))
    case Authenticated(authedUser) =>
      Response(AllowAccess(authedUser.user), updateCookieToAddSystemIfNecessary(authedUser))
  }

  def processOAuthCallback(pageRequest: PageRequest)(implicit ec: ExecutionContext): Future[Response[OAuthCallbackResponse]] = {
    def decodeCookie(name: String): Option[String] =
      pageRequest.cookies.get(name).map(value => URLDecoder.decode(value, UTF_8))

    val oAuthValidator: OAuthValidator = ???
    (for {
      expectedAntiForgeryToken <- decodeCookie(ANTI_FORGERY_KEY)
      returnUrl <- decodeCookie(LOGIN_ORIGIN_KEY)
    } yield {
      if (!pageRequest.queryParams.get("state").contains(expectedAntiForgeryToken)) {
        throw new IllegalArgumentException("The anti forgery token did not match")
      }
      val code = pageRequest.queryParams("code")

      oAuthValidator.validate(code).map { claimedAuth =>
        val priorAndProbablyExpiredAuth: Option[AuthenticatedUser] = ??? // should accept GracePeriod, but not expired?
        val authedUserData = claimedAuth.copy(
          authenticatingSystem = system,
          authenticatedIn = priorAndProbablyExpiredAuth.toSet.flatMap[String](_.authenticatedIn) + system,
          multiFactor = ??? // checkMultifactor(claimedAuth)
        )

        if (???) { // validateUser(authedUserData)
          Response(PageResponse.Redirect(URI.create(returnUrl)), cookieResponseFor(authedUserData, wipeTemporaryCookiesUsedForOAuth = true))
        } else Response(PageResponse.NotAuthorized(claimedAuth.user))
      }
    }) getOrElse {
      Future.successful(Response(???, ???)) // Future.successful(BadRequest("Missing cookies"))
    }
  }


//  def doIt(originUri: String, cookieValues: Map[String, String]): Response[PageResponse] = {
//
//  }
}

class ApiRequestHandlingStrategy() {
  def authenticateRequest(authStatus: AuthenticationStatus): Response[ApiResponse] = {
    authStatus match {
      case NotAuthenticated =>
        Response(ApiResponse.NoAuthentication)
      case InvalidCookie(e) =>
        Response(ApiResponse.NoAuthentication) // don't cookie wipe; might wipe out a good cookie coming from Page load
      case Expired(authedUser) =>
        Response(ApiResponse.NoAuthentication)
      case GracePeriod(authedUser) =>
        Response(ApiResponse.AllowAccess(authedUser.user, suggestCredsRefresh = true))
      case NotAuthorized(authedUser) =>
        Response(ApiResponse.NotAuthorized)
      case Authenticated(authedUser) =>
        Response(ApiResponse.AllowAccess(authedUser.user))
      // if required, add a CookieChanges that includes the system update
      // AuthorizedAllowAccess(addSystem = !authedUser.authenticatedIn(system))
    }
  }
}
object WebFrameworkAdapter {

  trait PageRequestAdapter[Req] {
    def transform(req: Req): PageRequest
  }

  trait ResponseModifier[Resp] {
    def apply(modifications: ResponseModification): Resp => Resp
  }

  trait PageResponseAdapter[Resp] {
    def handleNotAuthorised(user: User): Resp

    def handleRedirect(redirect: URI): Resp

    val responseModifier: ResponseModifier[Resp]
  }

  trait ApiResponseAdapter[Resp] {

    def handleDisallow(httpStatusCode: HttpStatusCode): Resp

    val responseModifier: ResponseModifier[Resp]
  }
}

/**
 * Should get handled a fully-realised Panda object - then either:
 * * if authorised, do the authorised action
 * * if unauthorised, do exactly what Panda said to do with its PageResponse model
 */
case class PageResponseHandlerFoo[R](
  webFramework: WebFrameworkAdapter.PageResponseAdapter[R]
) {
  def handle(pageResponse: Response[PageResponse])(allowAccess: User => Future[R])(implicit ec: ExecutionContext): Future[R] = (pageResponse.typ match {
    case PageResponse.AllowAccess(user) => allowAccess(user)
    case PageResponse.NotAuthorized(user) => Future.successful(webFramework.handleNotAuthorised(user))
    case PageResponse.Redirect(uri) => Future.successful(webFramework.handleRedirect(uri))
  }).map(webFramework.responseModifier.apply(pageResponse.responseModification))

  def handle(pageResponse: Response[OAuthCallbackResponse])(implicit ec: ExecutionContext): R =
    webFramework.responseModifier.apply(pageResponse.responseModification)(pageResponse.typ match {
    case PageResponse.NotAuthorized(user) => webFramework.handleNotAuthorised(user)
    case PageResponse.Redirect(uri) => webFramework.handleRedirect(uri)
  })
}

case class ApiResponseHandlerFoo[R](webFramework: WebFrameworkAdapter.ApiResponseAdapter[R]){
  def handle(apiResponse: Response[ApiResponse])(allowAccess: User => Future[R])(implicit ec: ExecutionContext): Future[R] = (apiResponse.typ match {
    case ApiResponse.AllowAccess(user, suggestCredsRefresh) => allowAccess(user) // suggestCredsRefresh
    case disallow: DisallowApiAccess => Future.successful(webFramework.handleDisallow(disallow.statusCode))
  }).map(webFramework.responseModifier.apply(apiResponse.responseModification))
}
