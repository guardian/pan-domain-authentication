package com.gu.pandomainauth

import com.gu.pandomainauth.model.AuthenticationStatus
import org.apache.http.client.utils.URLEncodedUtils

import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import scala.jdk.CollectionConverters.*

/**
 *
 * @param requestUrl may be relative, eg "/foo", or absolute
 * @param cookies
 */
case class PageRequest(requestUrl: URI, cookies: Map[String, String]) {
  val queryParams: Map[String, String] = {
    URLEncodedUtils.parse(requestUrl.getQuery, UTF_8).asScala.map(nvp => nvp.getName -> nvp.getValue).toMap
  }
    
  def authenticationStatus()(implicit authStatusFromRequest: AuthStatusFromRequest): AuthenticationStatus =
    authStatusFromRequest.authStatusFor(this)
}
