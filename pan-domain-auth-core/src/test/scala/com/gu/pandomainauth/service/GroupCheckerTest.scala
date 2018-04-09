package com.gu.pandomainauth.service

import java.util.Date

import com.gu.pandomainauth.model.{AuthenticatedUser, User}
import org.scalatest.{EitherValues, FreeSpec, Matchers}

class GroupCheckerTest extends FreeSpec with Matchers with EitherValues {
  val authUser = AuthenticatedUser(User("test", "üsér", "test.user@example.com", None), "testsuite", Set("testsuite", "another"),
    new Date().getTime + 86400, multiFactor = true, emergency = false)

  "Google Group Checker" - {
    "always pass for emergency cookies" in {
      val checker = new GoogleGroupChecker(null)
      val emergencyAuthedUser = authUser.copy(emergency = true)

      val result = checker.checkGroups(emergencyAuthedUser, List("EditorialToolsUsers"))
      result.right.value should be(true)
    }
  }
}
