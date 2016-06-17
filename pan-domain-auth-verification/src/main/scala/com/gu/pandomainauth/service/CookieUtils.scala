package com.gu.pandomainauth.service

import java.security.SignatureException

import com.gu.pandomainauth.{PrivateKey, PublicKey}
import com.gu.pandomainauth.model.{AuthenticatedUser, CookieParseException, CookieSignatureInvalidException, User}
import org.apache.commons.codec.binary.Base64

object CookieUtils {

  private[service] def serializeAuthenticatedUser(authUser: AuthenticatedUser): String =
      s"firstName=${authUser.user.firstName}" +
      s"&lastName=${authUser.user.lastName}" +
      s"&email=${authUser.user.email}" +
      authUser.user.avatarUrl.map(a => s"&avatarUrl=$a").getOrElse("") +
      s"&system=${authUser.authenticatingSystem}" +
      s"&authedIn=${authUser.authenticatedIn.mkString(",")}" +
      s"&expires=${authUser.expires}" +
      s"&multifactor=${authUser.multiFactor}"

  private[service] def deserializeAuthenticatedUser(serializedForm: String): AuthenticatedUser = {
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

  def generateCookieData(authUser: AuthenticatedUser, prvKey: PrivateKey): String = {
    val data = serializeAuthenticatedUser(authUser)
    val encodedData = new String(Base64.encodeBase64(data.getBytes("UTF-8")))
    val signature = Crypto.signData(data.getBytes("UTF-8"), prvKey)
    val encodedSignature = new String(Base64.encodeBase64(signature))

    s"$encodedData.$encodedSignature"
  }

  lazy val CookieRegEx = "^^([\\w\\W]*)\\.([\\w\\W]*)$".r

  def parseCookieData(cookieString: String, pubKey: PublicKey): AuthenticatedUser = {

    cookieString match {
      case CookieRegEx(data, sig) =>
        try {
          if (Crypto.verifySignature(Base64.decodeBase64(data.getBytes("UTF-8")), Base64.decodeBase64(sig.getBytes("UTF-8")), pubKey)) {
            deserializeAuthenticatedUser(new String(Base64.decodeBase64(data.getBytes("UTF-8"))))
          } else {
            throw new CookieSignatureInvalidException
          }
        } catch {
          case e: SignatureException =>
            throw new CookieSignatureInvalidException
        }
      case _ => throw new CookieParseException
    }
  }
}
