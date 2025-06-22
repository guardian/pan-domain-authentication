package com.gu.pandomainauth

import cats.Id
import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI

class PageRequestHandlingStrategyTest  extends AnyFreeSpec with Matchers with OptionValues {
  
  val dummyUri = URI.create("https://example.com/")
  
  "authorisation" - {
    "update cookie with new authorised system if it's not in the cookie already" in {

      
      val prhs: PagePlanners[Id] = ???

      val pageRequest: PageRequest = PageRequest(dummyUri, Map(prhs.cookieResponses.cookieSettings.cookieName -> ""))
      val cookieChanges = prhs.auth.planFor(pageRequest).responseModification.cookieChanges.value
      val newCookie = cookieChanges.setSessionCookies(prhs.cookieResponses.cookieSettings.cookieName)
      
      
      prhs.planForAuthStatus()
    }
  }
}