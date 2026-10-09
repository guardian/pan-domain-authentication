package com.gu.pandomainauth.oauth

import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import upickle.default.*


class UserInfoTest extends AnyFreeSpec with Matchers with EitherValues {

  "UserInfo" - {
    "parse" in {
      val userInfoJson = """{
                           |  "sub": "12345678901234567890",
                           |  "name": "Roberto Bloggs",
                           |  "given_name": "Roberto",
                           |  "family_name": "Bloggs",
                           |  "picture": "https://example.com/example.jpg",
                           |  "email": "roberto.bloggs@example.com",
                           |  "email_verified": true,
                           |  "hd": "example.com"
                           |}""".stripMargin
      val userInfo = read[UserInfo](userInfoJson)
      userInfo.family_name shouldBe "Bloggs"
      userInfo.email shouldBe "roberto.bloggs@example.com"
    }
  }

}
