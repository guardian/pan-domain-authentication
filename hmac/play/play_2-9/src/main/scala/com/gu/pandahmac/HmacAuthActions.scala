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

  type RequestHandler[A] = UserRequest[A] => Future[Result]

  // None if the request has no HMAC headers, letting the caller decide what to fall back to
  private def authByKey[A](request: Request[A], block: RequestHandler[A]): Option[Future[Result]] =
    for {
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
        Future.successful(Unauthorized)

  def authByPanda[A](request: Request[A], block: RequestHandler[A]): Future[Result] =
    AuthAction.invokeBlock(request, (request: UserRequest[A]) => {
      block(new UserRequest(request.user, request))
    })

  def apiAuthByPanda[A](request: Request[A], block: RequestHandler[A]): Future[Result] =
    APIAuthAction.invokeBlock(request, (request: UserRequest[A]) => {
      block(new UserRequest(request.user, request))
    })

  // New actions are defs, not objects: adding an object to a trait breaks binary compatibility
  def HMACOnlyAuthAction: ActionBuilder[UserRequest, AnyContent] = new ActionBuilder[UserRequest, AnyContent] {
    override def parser: BodyParser[AnyContent] = HMACAuthActions.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext = HMACAuthActions.this.controllerComponents.executionContext

    override def invokeBlock[A](request: Request[A], block: RequestHandler[A]): Future[Result] =
      authByKey(request, block).getOrElse(Future.successful(Unauthorized))
  }

  /* as per https://www.playframework.com/documentation/2.6.x/ScalaActionsComposition */
  def HMACOrPandaAuthAction: ActionBuilder[UserRequest, AnyContent] = new ActionBuilder[UserRequest, AnyContent] {
    override def parser: BodyParser[AnyContent] = HMACAuthActions.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext = HMACAuthActions.this.controllerComponents.executionContext

    override def invokeBlock[A](request: Request[A], block: RequestHandler[A]): Future[Result] =
      authByKey(request, block).getOrElse(authByPanda(request, block))
  }

  def HMACOrApiPandaAuthAction: ActionBuilder[UserRequest, AnyContent] = new ActionBuilder[UserRequest, AnyContent] {
    override def parser: BodyParser[AnyContent] = HMACAuthActions.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext = HMACAuthActions.this.controllerComponents.executionContext

    override def invokeBlock[A](request: Request[A], block: RequestHandler[A]): Future[Result] =
      authByKey(request, block).getOrElse(apiAuthByPanda(request, block))
  }


  @deprecated(
    "HMACAuthAction lets in Panda users (redirecting to login) when HMAC headers are missing. " +
      "Use HMACOrPandaAuthAction for the same behaviour, or HMACOnlyAuthAction if Panda users shouldn't have access.")
  object HMACAuthAction extends ActionBuilder[UserRequest, AnyContent] {
    override def parser: BodyParser[AnyContent] = HMACAuthActions.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext = HMACAuthActions.this.controllerComponents.executionContext

    override def invokeBlock[A](request: Request[A], block: RequestHandler[A]): Future[Result] =
      HMACOrPandaAuthAction.invokeBlock(request, block)
  }

  @deprecated(
    "APIHMACAuthAction lets in Panda users (returning 401/419 rather than redirecting) when HMAC headers are missing. " +
      "Use HMACOrApiPandaAuthAction for the same behaviour, or HMACOnlyAuthAction if Panda users shouldn't have access.")
  object APIHMACAuthAction extends ActionBuilder[UserRequest, AnyContent] {
    override def parser: BodyParser[AnyContent] = HMACAuthActions.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext = HMACAuthActions.this.controllerComponents.executionContext

    override def invokeBlock[A](request: Request[A], block: RequestHandler[A]): Future[Result] =
      HMACOrApiPandaAuthAction.invokeBlock(request, block)
  }
}
