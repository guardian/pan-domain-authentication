package com.gu.pandomainauth

import org.apache.http.client.utils.URLEncodedUtils

import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import scala.jdk.CollectionConverters._

case class PageRequest(requestUrl: URI, cookies: Map[String, String]) {
  val queryParams: Map[String, String] =
    URLEncodedUtils.parse(requestUrl.getQuery, UTF_8).asScala.map(nvp => nvp.getName -> nvp.getValue).toMap
}
