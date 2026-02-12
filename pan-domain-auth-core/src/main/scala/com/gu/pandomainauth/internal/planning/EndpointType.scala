package com.gu.pandomainauth.internal.planning

import com.gu.pandomainauth.*
import com.gu.pandomainauth.internal.*
import com.gu.pandomainauth.model.{AuthenticatedUser, User}

import java.net.URI

trait AuthedEndpointResponse

sealed trait PageResponse extends AuthedEndpointResponse
sealed trait WithholdAccess
sealed trait OAuthCallbackResponse extends PageResponse

sealed trait EndpointType {
  type RT <: AuthedEndpointResponse
  type RM
}

case class AllowAccess(user: User) extends PageEndpoint.RespType with ApiEndpoint.RespType

object PageEndpoint extends EndpointType {
  sealed trait RespType extends AuthedEndpointResponse
  sealed trait RespMod
  
  type RT = RespType
  type RM = RespMod

  case object Logout extends RespMod
  case class PrepareForOAuth(
    returnUrl: URI,
    antiForgeryToken: String, 
    wipeAuthCookie: Boolean = false
  ) extends RespMod
}

trait PageOrOAuthCallbackWithholdAccess extends PageEndpoint.RespType with OAuthCallbackEndpoint.RespType with WithholdAccess

case class NotAuthorized(user: AuthenticatedUser) extends PageOrOAuthCallbackWithholdAccess
case class Redirect(uri: URI) extends PageOrOAuthCallbackWithholdAccess

case class PersistAuth(authenticatedUser: AuthenticatedUser, wipeTemporaryCookiesUsedForOAuth: Boolean = false) 
  extends PageEndpoint.RespMod with OAuthCallbackEndpoint.RespMod

object OAuthCallbackEndpoint extends EndpointType {
  sealed trait RespType extends AuthedEndpointResponse
  sealed trait RespMod

  type RT = RespType
  type RM = RespMod
  
  case object BadRequest extends OAuthCallbackEndpoint.RespType with WithholdAccess
}

object ApiEndpoint extends EndpointType {
  sealed trait RespType extends AuthedEndpointResponse
  sealed trait RespMod

  type RT = RespType
  type RM = RespMod

  case class HttpStatusCode(code: Int, message: String)
  trait DisallowApiAccess extends RespType with WithholdAccess {
    val statusCode: HttpStatusCode
  }
  case object NotAuthorized extends DisallowApiAccess {
    val statusCode: HttpStatusCode =
      HttpStatusCode(403, "User is not authorized to use this tool")
  }
  case object NoAuthentication extends DisallowApiAccess {
    val statusCode: HttpStatusCode = 
      HttpStatusCode(401, "Missing or expired auth cookie, or cookie with an integrity failure") // or 419, if they are expired & not acceptable?
  }

  case class CredentialRefreshing(suggest: Boolean) extends RespMod
  
  val respModReifier: RespMod => ResponseModification = {
    case CredentialRefreshing(suggest) =>
      ResponseModification(responseHeaders = Map("X-Panda-Should-Refresh-Credentials" -> suggest.toString))
  }
}
