package com.gu.pandomainauth.service

case class ProxyConfiguration(host: String, port: Int)

object ProxyConfiguration {

  def fromSystemProperties = {
    val proxyHost = Option(System.getProperty("http.proxyHost"))
    val proxyPort = Option(System.getProperty("http.proxyPort")).map(_.toInt)

    configure(proxyHost, proxyPort)
  }

  def configure(host: Option[String], port: Option[Int]) = {
    for(h <- host; p <- port) yield ProxyConfiguration(h, p)
  }
}
