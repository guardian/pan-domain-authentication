package com.gu.pandomainauth.service

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import com.gu.pandomainauth.PanDomainAuth
import com.gu.pandomainauth.model.{User, AuthenticatedUser}
import org.apache.commons.codec.binary.{Hex, Base64}


object CookieUtils {

  private def serializeAuthenticatedUser(authUser: AuthenticatedUser): String =
    s"firstName=${authUser.user.firstName}" +
      s"&lastName=${authUser.user.lastName}" +
      s"&email=${authUser.user.email}" +
      s"&avatarUrl=${authUser.user.avatarUrl}" +
      s"&system=${authUser.authenticatingSystem}" +
      s"&authedIn=${authUser.authenticatedIn.mkString(",")}" +
      s"&expires=${authUser.expires}" +
      s"&multifactor=${authUser.multiFactor}"

  private def deserializeAuthenticatedUser(serializedForm: String): AuthenticatedUser = {
    val data = serializedForm
      .split("&")
      .map(_.split("=", 2))
      .map{p => p(0) -> p(1)}
      .toMap

    AuthenticatedUser(
      user = User(data("firstName"), data("lastName"), data("email"), data.get("avatarUrl")),
      authenticatingSystem = data("system"),
      authenticatedIn = Set(data("authedIn").split(",") :_*),
      expires = data("expires").toLong,
      multiFactor = data("multifactor").toBoolean
    )
  }

  private def encode(data: String) = new String(Base64.encodeBase64(data.getBytes("UTF-8")))

  private def decode(data: String) = new String(Base64.decodeBase64(data.getBytes("UTF-8")))

  private def generateSignature(message: String, secret: String): String = {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(secret.getBytes, "HmacSHA1"))

    new String(Hex.encodeHex(mac.doFinal(message.getBytes("UTF-8"))))
  }

  def generateCookieData(authUser: AuthenticatedUser, secret: String): String = {
    val data = encode(serializeAuthenticatedUser(authUser))
    val sign = generateSignature(data, secret)

    s"$data>>$sign"
  }

  lazy val CookieRegEx = "^^([\\w\\W]*)>>([\\w\\W]*)$".r

  def parseCookieData(cookieString: String, secret: String): AuthenticatedUser = {

    cookieString match {
      case CookieRegEx(data, sig) => {
        val computedSig = generateSignature(data, secret)
        if (safeEquals(sig, computedSig)) {
          deserializeAuthenticatedUser(decode(data))
        } else {
          throw new CookieSignatureInvalidException
        }
      }
      case _ => throw new CookieParseException
    }
  }


  // Cribbed from the play framework equivalent - seems like a sound idea
  // Do not change this unless you understand the security issues behind timing attacks.
  // This method intentionally runs in constant time if the two strings have the same length.
  // If it didn't, it would be vulnerable to a timing attack.
  private def safeEquals(a: String, b: String) = {
    if (a.length != b.length) {
      false
    } else {
      var equal = 0
      for (i <- Array.range(0, a.length)) {
        equal |= a(i) ^ b(i)
      }
      equal == 0
    }
  }

}

class CookieSignatureInvalidException extends RuntimeException("cookie signature incorrect")
class CookieParseException extends RuntimeException("cookie format incorrect")
