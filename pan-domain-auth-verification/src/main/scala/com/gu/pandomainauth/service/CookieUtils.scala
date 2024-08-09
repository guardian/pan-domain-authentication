package com.gu.pandomainauth.service

import com.gu.pandomainauth.model.{AuthenticatedUser, User}
import com.gu.pandomainauth.service.CookieUtils.CookieIntegrityFailure.{MalformedCookieText, MissingOrMalformedUserData, SignatureNotValid}

import java.security.{PrivateKey, PublicKey}

object CookieUtils {
  sealed trait CookieIntegrityFailure
  object CookieIntegrityFailure {
    case object MalformedCookieText extends CookieIntegrityFailure
    case object SignatureNotValid extends CookieIntegrityFailure
    case object MissingOrMalformedUserData extends CookieIntegrityFailure
  }

  type CookieResult[A] = Either[CookieIntegrityFailure, A]

  private[service] def serializeAuthenticatedUser(authUser: AuthenticatedUser): String =
      s"firstName=${authUser.user.firstName}" +
      s"&lastName=${authUser.user.lastName}" +
      s"&email=${authUser.user.email}" +
      authUser.user.avatarUrl.map(a => s"&avatarUrl=$a").getOrElse("") +
      s"&system=${authUser.authenticatingSystem}" +
      s"&authedIn=${authUser.authenticatedIn.mkString(",")}" +
      s"&expires=${authUser.expires}" +
      s"&multifactor=${authUser.multiFactor}"

  private[service] def deserializeAuthenticatedUser(serializedForm: String): Option[AuthenticatedUser] = {
    val data = serializedForm
      .split("&")
      .map(_.split("=", 2))
      .map{p => p(0) -> p(1)}
      .toMap

    for {
      firstName <- data.get("firstName")
      lastName <- data.get("lastName")
      email <- data.get("email")
      system <- data.get("system")
      authedIn <- data.get("authedIn")
      expires <- data.get("expires").flatMap(_.toLongOption)
      multiFactor <- data.get("multifactor").flatMap(_.toBooleanOption)
    } yield AuthenticatedUser(
      user = User(firstName, lastName, email, data.get("avatarUrl")),
      authenticatingSystem = system,
      authenticatedIn = Set(authedIn.split(",").toSeq :_*),
      expires = expires,
      multiFactor = multiFactor
    )
  }

  def generateCookieData(authUser: AuthenticatedUser, prvKey: PrivateKey): String =
    CookiePayload.generateForPayloadText(serializeAuthenticatedUser(authUser), prvKey).asCookieText

  def parseCookieData(cookieString: String, publicKey: PublicKey): CookieResult[AuthenticatedUser] = for {
    cookiePayload <- CookiePayload.parse(cookieString).toRight(MalformedCookieText)
    cookiePayloadText <- cookiePayload.payloadTextVerifiedSignedWith(publicKey).toRight(SignatureNotValid)
    authUser <- deserializeAuthenticatedUser(cookiePayloadText).toRight(MissingOrMalformedUserData)
  } yield authUser
}

