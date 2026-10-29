package com.gu.pandomainauth.oauth

import cats.*
import com.gu.pandomainauth.SystemAuthorisation
import com.gu.pandomainauth.internal.planning
import com.gu.pandomainauth.internal.planning.{AuthPersistenceStatus, NotAuthorized, Redirect}
import com.gu.pandomainauth.model.{AuthenticatedUser, NotAuthenticated, User}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Inside, OptionValues}

import java.net.URI
import java.time.Instant

class NewlyOAuthedUserHandlerTest extends AnyFreeSpec with Matchers with OptionValues with Inside {
  val initialAuthedUserWithoutMultifactorStatus =
    AuthenticatedUser(User("John", "Smith", "", None), "composer", Set.empty, Instant.now().plusSeconds(1), false)

  val examplePageRequestUrl: URI = URI.create("https://example.tool.co.uk/admin")

  "NewlyAuthenticatedUserHandler" - {
    "ensure that 'authenticatedIn' is updated once the user has been authorised (not just authenticated) by that system" in {
      val handler = new NewlyOAuthedUserHandler[Id](
        SystemAuthorisation("composer", _ => true, false),
        twoFactorAuthChecker = None
      )

      initialAuthedUserWithoutMultifactorStatus.authenticatedIn shouldBe empty // authorisation has not happened yet

      val plan =
        handler.planFor(initialAuthedUserWithoutMultifactorStatus, priorAuth = AuthPersistenceStatus(NotAuthenticated, Set.empty), returnUrl = examplePageRequestUrl)

      inside (plan.respMod.value) {
        case planning.PersistAuth(persistedUser, _) => persistedUser.authenticatedIn shouldBe Set("composer")
      }
    }

    "ensure multi-factor status is available to authorisation checks" in {
      def responseWhen2FACheckReturns(status: Boolean) = new NewlyOAuthedUserHandler[Id](
          SystemAuthorisation("composer", _.multiFactor, false),
          twoFactorAuthChecker = Some(_ => status)
        ).planFor(
          initialAuthedUserWithoutMultifactorStatus,
          priorAuth = AuthPersistenceStatus(NotAuthenticated, Set.empty),
          returnUrl = examplePageRequestUrl
        ).respType

      responseWhen2FACheckReturns(true) shouldBe Redirect(examplePageRequestUrl)
      responseWhen2FACheckReturns(false) shouldBe NotAuthorized(initialAuthedUserWithoutMultifactorStatus)
    }
  }
}