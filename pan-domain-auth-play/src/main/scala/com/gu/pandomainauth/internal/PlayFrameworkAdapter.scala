package com.gu.pandomainauth.internal

import cats.Endo
import com.gu.pandomainauth.internal.planning.ApiEndpoint.HttpStatusCode
import com.gu.pandomainauth.model.{AuthenticatedUser, User}
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter
import com.gu.pandomainauth.ResponseModification
import play.api.mvc.{Cookie, DiscardingCookie, Result, Results}

import java.net.{URI, URLEncoder}

object PlayFrameworkAdapter {
  val responseModifier: WebFrameworkAdapter.ResponseModifier[Result] = (mods: ResponseModification) => { initialResult =>
    val modifiers = Seq[Endo[Result]](
      _.withHeaders(mods.responseHeaders.toSeq: _*)
    ) ++ mods.cookies.map[Endo[Result]] { cookieChanges =>
      _.withCookies(cookieChanges.setSessionCookies.toSeq.map {
          case (cookieNameAndDomain, value) => Cookie(
            cookieNameAndDomain.name,
            value = URLEncoder.encode(value, "UTF-8"),
            domain = cookieNameAndDomain.domain,
            secure = true,
            httpOnly = true,
            // Chrome will pass back SameSite=Lax cookies, but Firefox requires
            // SameSite=None, since the cookies are to be returned on a redirect
            // from a 3rd party
            sameSite = Some(Cookie.SameSite.None)
          )
        }: _*)
        .discardingCookies(cookieChanges.wipeCookies.toSeq.map(cookieNameAndDomain => DiscardingCookie(
          cookieNameAndDomain.name,
          domain = cookieNameAndDomain.domain,
          secure = true
        )): _*)
    }
    modifiers.foldLeft(initialResult)((result, f) => f(result))
  }
  
  object ApiResponses extends WebFrameworkAdapter.ApiResponseAdapter[Result] {
    override def handleDisallow(statusCode: HttpStatusCode): Result = Results.Status(statusCode.code)
  }
  
  trait PageResponses extends WebFrameworkAdapter.PageResponseAdapter[Result] with Results {
    override def handleRedirect(redirect: URI): Result = Redirect(redirect.toString)
  }
}
