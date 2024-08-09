package com.gu.pandomainauth

import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers


class PublicSettingsTest extends AnyFreeSpec with Matchers with EitherValues {

  "extractSettings" - {
    "extracts properties from a valid body" in {
      val body =
        """key=value
          |foo=bar
        """.stripMargin

      Settings.extractSettings(body) shouldEqual Right(Map("key" -> "value", "foo" -> "bar"))
    }
  }
}
