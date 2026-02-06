package com.gu.pandomainauth.webframeworks

import com.gu.pandomainauth.internal.planning.ApiEndpoint.HttpStatusCode
import com.gu.pandomainauth.model.AuthenticatedUser
import com.gu.pandomainauth.{PageRequest, ResponseModification}

import java.net.URI

/**
 * Traits required for translating web-framework requests/responses to Panda representations of those same things.
 *
 * Contrast with the [[com.gu.pandomainauth.EndpointAuth]] interfaces which are high-level entry points into Panda's
 * functionality.
 */
object WebFrameworkAdapter {

  trait RequestAdapter[Req] {
    def convert(req: Req): PageRequest
  }

  implicit class RichRequest[Req: RequestAdapter](req: Req) {
    def asPandaRequest: PageRequest = implicitly[RequestAdapter[Req]].convert(req)
  }

  trait ResponseModifier[Resp] {
    def apply(modifications: ResponseModification): Resp => Resp
  }
  
  trait ResponseAdapter[Resp] {
    val responseModifier: ResponseModifier[Resp]
  }


  trait PageResponseAdapter[Resp] extends ResponseAdapter[Resp] {
    def handleNotAuthorised(user: AuthenticatedUser): Resp //  'User' would be better than 'AuthenticatedUser'

    def handleRedirect(redirect: URI): Resp
  }

  trait ApiResponseAdapter[Resp] extends ResponseAdapter[Resp] {
    def handleDisallow(httpStatusCode: HttpStatusCode): Resp
  }
}
