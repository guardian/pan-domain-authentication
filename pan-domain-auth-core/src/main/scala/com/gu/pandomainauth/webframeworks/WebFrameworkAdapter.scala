package com.gu.pandomainauth.webframeworks

import com.gu.pandomainauth.ApiResponse.HttpStatusCode
import com.gu.pandomainauth.model.User
import com.gu.pandomainauth.{PageRequest, ResponseModification}

import java.net.URI

object WebFrameworkAdapter {

  trait PageRequestAdapter[Req] {
    def convert(req: Req): PageRequest
  }

  implicit class RichRequest[Req: PageRequestAdapter](req: Req) {
    def asPandaRequest: PageRequest = implicitly[PageRequestAdapter[Req]].convert(req)
  }

  trait ResponseModifier[Resp] {
    def apply(modifications: ResponseModification): Resp => Resp
  }
  
  trait ResponseAdapter[Resp] {
    val responseModifier: ResponseModifier[Resp]
  }

  trait PageResponseAdapter[Resp] extends ResponseAdapter[Resp] {
    def handleNotAuthorised(user: User): Resp

    def handleRedirect(redirect: URI): Resp
  }

  trait ApiResponseAdapter[Resp] extends ResponseAdapter[Resp] {

    def handleDisallow(httpStatusCode: HttpStatusCode): Resp
  }
}
