package com.gu.pandomainauth.webframeworks

import cats.Endo
import com.gu.pandomainauth.internal.planning.ApiEndpoint.HttpStatusCode
import com.gu.pandomainauth.model.User
import com.gu.pandomainauth.oauth.OAuthCallbackPlanner.PayloadFailure
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
    def apply(modifications: ResponseModification): Endo[Resp]
  }

  trait PageResponseAdapter[Resp] {
    def handleRedirect(redirect: URI): Resp

    def handleNotAuthorised(user: User): Resp //  'User' is better than 'AuthenticatedUser' here

    def handleBadOAuthCallback(payloadFailure: PayloadFailure): Resp
  }

  trait ApiResponseAdapter[Resp] {
    def handleDisallow(httpStatusCode: HttpStatusCode): Resp
  }
}
