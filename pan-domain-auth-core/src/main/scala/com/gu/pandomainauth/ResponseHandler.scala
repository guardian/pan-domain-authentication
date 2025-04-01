package com.gu.pandomainauth

import com.gu.pandomainauth.model.User
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter.ResponseAdapter

import scala.concurrent.{ExecutionContext, Future}

/**
 * @tparam PandaResponseType
 * @tparam Resp the native response type of the web framework - eg, for Play, 'Result'.
 */
trait ResponseHandler[PandaResponseType, Resp] {

  val responseAdapter: ResponseAdapter[Resp]
  
  /**
   * Receives a fully-realised Panda response - then either:
   * - if authorised, does the authorised action
   * - if unauthorised, do exactly what Panda said to do with its PageResponse model
   */
  def handle(pandaResponse: Plan[PandaResponseType])(allowAccess: User => Future[Resp])(implicit ec: ExecutionContext): Future[Resp]

  def modifyResponseWith(responseModification: ResponseModification): Resp => Resp = responseAdapter.responseModifier.apply(responseModification)

}

case class PageResponseHandler[R](
  responseAdapter: WebFrameworkAdapter.PageResponseAdapter[R]
) extends ResponseHandler[PageResponse, R] {
  import PageResponse.*


  def handle(plan: Plan[PageResponse])(allowAccess: User => Future[R])(implicit ec: ExecutionContext): Future[R] = (plan.typ match {
    case AllowAccess(user) => allowAccess(user)
    case NotAuthorized(user) => Future.successful(responseAdapter.handleNotAuthorised(user))
    case Redirect(uri) => Future.successful(responseAdapter.handleRedirect(uri))
  }).map(modifyResponseWith(plan.responseModification))

  def handle(plan: Plan[OAuthCallbackResponse]): R =
    modifyResponseWith(plan.responseModification)(plan.typ match {
      case NotAuthorized(user) => responseAdapter.handleNotAuthorised(user)
      case Redirect(uri) => responseAdapter.handleRedirect(uri)
    })
}

case class ApiResponseHandler[Resp](responseAdapter: WebFrameworkAdapter.ApiResponseAdapter[Resp]) extends ResponseHandler[ApiResponse, Resp] {
  import ApiResponse.*

  def handle(pandaResponse: Plan[ApiResponse])(allowAccess: User => Future[Resp])(implicit ec: ExecutionContext): Future[Resp] = (pandaResponse.typ match {
    case AllowAccess(user) => allowAccess(user)
    case disallow: DisallowApiAccess => Future.successful(responseAdapter.handleDisallow(disallow.statusCode))
  }).map(modifyResponseWith(pandaResponse.responseModification))
}
