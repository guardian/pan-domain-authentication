package com.gu.pandomainauth

import com.gu.pandomainauth.internal.planning.{AuthPersistenceStatus, AuthStatusFromRequest}
import com.gu.pandomainauth.model.AuthenticationStatus
import org.apache.http.client.utils.URLEncodedUtils

import java.net.{URI, URLDecoder}
import java.nio.charset.StandardCharsets.UTF_8
import scala.jdk.CollectionConverters.*

/**
 * The literal values of an HTTP request that are relevant to Panda's processing of the request.
 */
case class PageRequest(requestUrl: URI, cookies: Map[String, String]) {
  val queryParams: Map[String, String] = {
    URLEncodedUtils.parse(requestUrl.getQuery, UTF_8).asScala.map(nvp => nvp.getName -> nvp.getValue).toMap
  }
  
  def getUrlDecodedCookie(cookieName: String): Option[String] =
    cookies.get(cookieName).map(value => URLDecoder.decode(value, UTF_8))

  def authenticationStatus()(implicit authStatusFromRequest: AuthStatusFromRequest): AuthPersistenceStatus =
    authStatusFromRequest.authStatusFor(this)
}
