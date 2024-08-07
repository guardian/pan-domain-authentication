package com.gu.pandomainauth.service

import com.gu.pandomainauth.service.CookiePayload.encodeBase64
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Base64.isBase64

import java.nio.charset.StandardCharsets.UTF_8
import java.security.{PrivateKey, PublicKey}
import scala.util.matching.Regex

/**
 * A representation of the underlying binary data (both payload & signature) in a Panda cookie.
 *
 * If an instance has been parsed from a cookie's text value, the existence of the instance
 * *does not* imply that the signature has been verified. It only means that the cookie text was
 * correctly formatted (two Base64 strings separated by '.').
 *
 * `CookiePayload` is designed to be the optimal representation of cookie data for checking
 * signature-validity against *multiple* possible accepted public keys. It's a bridge between
 * these two contexts:
 *
 * * cookie text: the raw cookie value - two Base64-encoded strings (payload & signature), separated by '.'
 * * payload text: in Panda, a string representation of `AuthenticatedUser`
 *
 * To make those transformations, you need either a public or private key:
 *
 * * payload text -> cookie text: uses a *private* key to generate the signature
 * * cookie text -> payload text: uses a *public* key to verify the signature
 */
case class CookiePayload(payloadBytes: Array[Byte], sig: Array[Byte]) {
  def payloadTextVerifiedSignedWith(publicKey: PublicKey): Option[String] =
    if (Crypto.verifySignature(payloadBytes, sig, publicKey)) Some(new String(payloadBytes, UTF_8)) else None

  lazy val asCookieText: String = s"${encodeBase64(payloadBytes)}.${encodeBase64(sig)}"
}

object CookiePayload {
  private val CookieRegEx: Regex = "^([\\w\\W]*)\\.([\\w\\W]*)$".r

  private def encodeBase64(data: Array[Byte]): String = new String(Base64.encodeBase64(data), UTF_8)
  private def decodeBase64(text: String): Array[Byte] = Base64.decodeBase64(text.getBytes(UTF_8))

  /**
   * @return `None` if the cookie text is incorrectly formatted (ie not "abc.xyz", with a '.' separator)
   */
  def parse(cookieText: String): Option[CookiePayload] = cookieText match {
    case CookieRegEx(data, sig) if isBase64(data) && isBase64(sig) =>
      Some(CookiePayload(decodeBase64(data), decodeBase64(sig)))
    case _ => None
  }

  def generateForPayloadText(payloadText: String, privateKey: PrivateKey): CookiePayload = {
    val data = payloadText.getBytes(UTF_8)
    CookiePayload(data, Crypto.signData(data, privateKey))
  }
}
