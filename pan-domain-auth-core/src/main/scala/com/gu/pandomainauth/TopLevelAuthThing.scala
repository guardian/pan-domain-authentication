package com.gu.pandomainauth

import cats._
import cats.data._
import cats.syntax.all._
import com.gu.pandomainauth.internal.planning.{AllowAccess, AuthPlanner, WithholdAccess}
import com.gu.pandomainauth.model.User
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter.{RequestAdapter, RichRequest}

abstract class TopLevelAuthThing[
  Req: RequestAdapter,
  RespType, RespMod,
  Resp,
  F[_] : Monad
](
  respModReifier: RespMod => ResponseModification,
  authPlanner: AuthPlanner[RespType, RespMod],
  responseAdapter: WebFrameworkAdapter.ResponseAdapter[Resp],
) {
  val F: Monad[F] = Monad[F]

  protected def modify(responseModification: ResponseModification): Endo[Resp] =
    responseAdapter.responseModifier(responseModification)
  
  def modifyResponseWith(responseMod: RespMod): Endo[Resp] = modify(respModReifier(responseMod))

  def authenticateRequest(request: Req)(produceResultGivenAuthedUser: User => F[Resp]): F[Resp] = {
    val plan = authPlanner.planFor(request.asPandaRequest)

    val respF = plan.respType match {
      case AllowAccess(user) => produceResultGivenAuthedUser(user)
      case withholdAccess: RespType with WithholdAccess => F.pure(handleWithholdAccess(withholdAccess))
    }

    plan.respMod.fold(respF)(respMod => respF.map(modifyResponseWith(respMod)))
  }

  def handleWithholdAccess(pandaResp: RespType with WithholdAccess): Resp
}
