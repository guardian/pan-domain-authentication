package com.gu.pandahmac
import com.gu.hmac.{HMACHeaders, ValidateHMACHeader}
import com.gu.pandomainauth.action.{AuthActions, UserRequest}
import com.gu.pandomainauth.model.User
import org.slf4j.LoggerFactory
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
import play.api.mvc._

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

  private def authByKeyOrPanda[A](request: Request[A], block: RequestHandler[A], useApiAuth: Boolean, hmacOnly: Boolean): Future[Result] = {
    (for {
      hmac <- request.headers.get(HMACHeaderNames.hmacKey)
      date <- request.headers.get(HMACHeaderNames.dateKey)
      serviceName = request.headers.get(
        HMACHeaderNames.serviceNameKey
      ) getOrElse "hmac-authed-service"
      uri = new URI(request.uri)
    } yield
      if (validateHMACHeaders(date, hmac, uri))
        block(new UserRequest(User(serviceName, "", "", None), request))
      else
        Future.successful[Result](Unauthorized)
    ).getOrElse {
      if (hmacOnly) Future.successful(Unauthorized)
      else if (useApiAuth) apiAuthByPanda(request, block)
      else authByPanda(request, block)
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
      authByKeyOrPanda(request, block, useApiAuth = false, hmacOnly = false)
    }
  }


  object APIHMACAuthAction extends ActionBuilder[UserRequest, AnyContent] {
    override def parser: BodyParser[AnyContent] = HMACAuthActions.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext = HMACAuthActions.this.controllerComponents.executionContext

    override def invokeBlock[A](request: Request[A], block: RequestHandler[A]): Future[Result] =
      authByKeyOrPanda(request, block, useApiAuth = true, hmacOnly = false)
  }


  object HMACOnlyAuthAction extends ActionBuilder[UserRequest, AnyContent] {
    override def parser: BodyParser[AnyContent] = HMACAuthActions.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext = HMACAuthActions.this.controllerComponents.executionContext

    override def invokeBlock[A](request: Request[A], block: RequestHandler[A]): Future[Result] =
      authByKeyOrPanda(request, block, useApiAuth = false, hmacOnly = true)
  }
}
