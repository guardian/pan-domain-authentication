package com.gu.pandomainauth.oauth

import upickle.default.*
import upickle.implicits.key

import java.net.URI
import java.net.http.HttpClient.Version.HTTP_2
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import java.time.Duration.ofSeconds
import java.util.concurrent.atomic.AtomicReference
import scala.util.{Success, Try}

case class DiscoveryDocument(
  @key("authorization_endpoint") authorizationEndpoint: URI,
  @key("token_endpoint") tokenEndpoint: URI,
  @key("userinfo_endpoint") userinfoEndpoint: URI
)

object DiscoveryDocument {
  val uri: URI = URI.create("https://accounts.google.com/.well-known/openid-configuration")

  implicit val discoveryDocumentRW: ReadWriter[DiscoveryDocument] = {
    implicit val uriRW: ReadWriter[URI] = readwriter[String].bimap[URI](_.toString, URI.create)

    macroRW[DiscoveryDocument]
  }

  /**
   * Google's documentation suggests that the Discovery Document should be fetched and cached
   * with a cache-duration respecting the HTTP response headers:
   *
   * "Standard HTTP caching headers are used and should be respected."
   * https://developers.google.com/identity/openid-connect/openid-connect#discovery
   *
   * In truth, there's probably no harm in caching the Discovery Document for the duration of
   * the app server's lifetime, given that we at the Guardian frequently redeploy our services.
   */
  class Cache() {
    private val client: HttpClient = HttpClient.newBuilder.version(HTTP_2).connectTimeout(ofSeconds(20)).build

    private val request: HttpRequest = HttpRequest.newBuilder(uri).GET().build()

    private def fetchAndParse(): Try[DiscoveryDocument] = for {
      response <- Try(client.send(request, BodyHandlers.ofString)) if response.statusCode() == 200
      doc <- Try(read[DiscoveryDocument](response.body()))
    } yield doc

    private val holder: AtomicReference[Try[DiscoveryDocument]] = new AtomicReference(fetchAndParse())

    def get(): DiscoveryDocument = holder.updateAndGet(docTry => docTry.fold(_ => fetchAndParse(), Success(_))).get
  }
}