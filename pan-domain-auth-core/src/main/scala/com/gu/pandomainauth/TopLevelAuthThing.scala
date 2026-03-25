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
  responseModifier: WebFrameworkAdapter.ResponseModifier[Resp],
  authPlanner: AuthPlanner[RespType, RespMod]
) {
  val F: Monad[F] = Monad[F]

  def modifyResponseWith(responseMod: RespMod): Endo[Resp] = responseModifier(respModReifier(responseMod))

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
