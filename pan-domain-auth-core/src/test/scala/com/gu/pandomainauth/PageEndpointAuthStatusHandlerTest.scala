package com.gu.pandomainauth

import com.gu.pandomainauth.internal.planning
import com.gu.pandomainauth.internal.planning.PageEndpoint.PrepareForOAuth
import com.gu.pandomainauth.internal.planning.{AuthPersistenceStatus, PersistAuth, Plan}
import com.gu.pandomainauth.model.{Authenticated, AuthenticatedUser, NotAuthenticated, User}
import com.gu.pandomainauth.oauth.OAuthUrl
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Inside, OptionValues}

import java.net.URI
import java.time.Instant

class PageEndpointAuthStatusHandlerTest extends AnyFreeSpec with Matchers with OptionValues with Inside {
  def authenticatedStatus(authenticatedIn: Set[String]) = Authenticated(
    AuthenticatedUser(User("John", "Smith", "", None), "composer", authenticatedIn, Instant.now().plusSeconds(1), true)
  )

  val examplePageRequestUrl: URI = URI.create("https://example.tool.co.uk/admin")

  val authStatusHandler = new PageEndpointAuthStatusHandler((antiForgeryToken, _) =>
    URI.create(s"example.com/?aft=$antiForgeryToken"))

  "authorisation" - {
    "persist new auth cookie containing additional authorised system if it's not in the cookie already" in {
      val plan = authStatusHandler.planForAuthStatus(examplePageRequestUrl, AuthPersistenceStatus(
        authenticatedStatus(authenticatedIn = Set("workflow", "composer")),
        systemsAuthorisationsCurrentlyPersistedWithUser = Set("composer")
      ))

      inside (plan.respMod.value) {
        case planning.PersistAuth(persistedUser, _) => persistedUser.authenticatedIn shouldBe Set("workflow", "composer")
      }
    }

    "redirect for OAuth if the user is not authenticated" in {
      val plan = authStatusHandler.planForAuthStatus(examplePageRequestUrl, AuthPersistenceStatus(NotAuthenticated, Set.empty))

      inside(plan) {
        case Plan(planning.Redirect(redirectUri), Some(prepareForOAuth: PrepareForOAuth)) =>
          prepareForOAuth.returnUrl shouldBe examplePageRequestUrl
          redirectUri.getQuery should include (prepareForOAuth.antiForgeryToken)
      }
    }
  }
}