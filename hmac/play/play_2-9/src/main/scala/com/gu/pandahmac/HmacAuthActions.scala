package com.gu.pandahmac
import com.gu.pandomainauth.action.{AuthActions, UserRequest}
import com.gu.pandomainauth.model.User
import play.api.libs.ws.WSClient
import play.api.mvc.*
import play.api.mvc.Results.*

import java.net.URI
import scala.concurrent.{ExecutionContext, Future}

trait HMACAuthActions extends AuthActions with HMACSecrets {
  /**
    * Play application
    * Play application components that you must provide in order to use AuthActions
    */
  def wsClient: WSClient
  def controllerComponents: ControllerComponents

  private implicit val ec: ExecutionContext = controllerComponents.executionContext

  private def authByKeyOrPanda[A](request: Request[A], block: RequestHandler[A], useApiAuth: Boolean): Future[Result] = {
    val oHmac: Option[String] = request.headers.get(HMACHeaderNames.hmacKey)
    val oDate: Option[String] = request.headers.get(HMACHeaderNames.dateKey)
    val oServiceName: Option[String] = request.headers.get(HMACHeaderNames.serviceNameKey)
    val uri = new URI(request.uri)

    (oHmac, oDate) match {
      case (Some(hmac), Some(date)) => {
        if (validateHMACHeaders(date, hmac, uri)) {
          val user = User(oServiceName.getOrElse("hmac-authed-service"), "", "", None)
          block(new UserRequest(user, request))
        } else {
          Future.successful(Unauthorized)
        }
      }
      case _ => if(useApiAuth) apiAuthByPanda(request, block) else authByPanda(request, block)
    }
  }

  type RequestHandler[A] = UserRequest[A] => Future[Result]

  def authByPanda[A](request: Request[A], block: RequestHandler[A]): Future[Result] =
    AuthAction.invokeBlock(request, (request: UserRequest[A]) => {
      block(new UserRequest(request.user, request))
    })

  def apiAuthByPanda[A](request: Request[A], block: RequestHandler[A]): Future[Result] =
    APIAuthAction.invokeBlock(request, (request: UserRequest[A]) => {
      block(new UserRequest(request.user, request))
    })


  /* as per https://www.playframework.com/documentation/2.6.x/ScalaActionsComposition */
  object HMACAuthAction extends ActionBuilder[UserRequest, AnyContent] {
    override def parser: BodyParser[AnyContent] = HMACAuthActions.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext = HMACAuthActions.this.controllerComponents.executionContext

    override def invokeBlock[A](request: Request[A], block: RequestHandler[A]): Future[Result] = {
      authByKeyOrPanda(request, block, useApiAuth = false)
    }
  }


  object APIHMACAuthAction extends ActionBuilder[UserRequest, AnyContent] {
    override def parser: BodyParser[AnyContent] = HMACAuthActions.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext = HMACAuthActions.this.controllerComponents.executionContext

    override def invokeBlock[A](request: Request[A], block: RequestHandler[A]): Future[Result] =
      authByKeyOrPanda(request, block, useApiAuth = true)
  }
}
