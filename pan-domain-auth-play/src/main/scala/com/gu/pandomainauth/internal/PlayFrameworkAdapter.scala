package com.gu.pandomainauth.internal

import com.gu.pandomainauth.model.User
import com.gu.pandomainauth.webframeworks.WebFrameworkAdapter
import com.gu.pandomainauth.{ApiResponse, ResponseModification}
import play.api.mvc.{Cookie, DiscardingCookie, Result, Results}

import java.net.{URI, URLEncoder}

object PlayFrameworkAdapter extends Results
  with WebFrameworkAdapter.PageResponseAdapter[Result]
  with WebFrameworkAdapter.ApiResponseAdapter[Result] {

  override val responseModifier: WebFrameworkAdapter.ResponseModifier[Result] = (mods: ResponseModification) => { initialResult =>
    val modifiers: Seq[Result => Result] = Seq[Result => Result](
      _.withHeaders(mods.responseHeaders.toSeq: _*)
    ) ++ mods.cookieChanges.map[Result => Result]{ cookieChanges =>
        _.withCookies(cookieChanges.setSessionCookies.toSeq.map {
            case (name, value) => Cookie(
              name,
              value = URLEncoder.encode(value, "UTF-8"),
              domain = Some(cookieChanges.domain),
              secure = true,
              httpOnly = true,
              // Chrome will pass back SameSite=Lax cookies, but Firefox requires
              // SameSite=None, since the cookies are to be returned on a redirect
              // from a 3rd party
              sameSite = Some(Cookie.SameSite.None)
            )
          }: _*)
          .discardingCookies(cookieChanges.wipeCookies.toSeq.map(name => DiscardingCookie(
            name,
            domain = Some(cookieChanges.domain),
            secure = true
          )): _*)
      }
    modifiers.foldLeft(initialResult)((result, f) => f(result))
  }

  override def handleNotAuthorised(user: User): Result = ??? // TODO showUnauthedMessage(invalidUserMessage(user))

  override def handleRedirect(redirect: URI): Result = Redirect(redirect.toString)

  override def handleDisallow(statusCode: ApiResponse.HttpStatusCode): Result = Results.Status(statusCode.code)

}
