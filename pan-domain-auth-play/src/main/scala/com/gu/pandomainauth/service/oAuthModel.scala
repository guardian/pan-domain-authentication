package com.gu.pandomainauth.service

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.apache.commons.codec.binary.Base64

case class DiscoveryDocument(authorization_endpoint: String, token_endpoint: String, userinfo_endpoint: String)
object DiscoveryDocument {
  implicit val discoveryDocumentReads: Reads[DiscoveryDocument] = Json.reads[DiscoveryDocument]
  def fromJson(json: JsValue) = Json.fromJson[DiscoveryDocument](json).getOrElse(
    throw new IllegalArgumentException("Invalid discovery document")
  )
}

case class Token(access_token:String, token_type:String, expires_in:Long, id_token:String) {
  val jwt = JsonWebToken(id_token)
}
object Token {

  implicit val tokenReads: Reads[Token] = (
    (JsPath \ "access_token").read[String] and
    (JsPath \ "token_type").read[String] and
    (JsPath \ "expires_in").read[Long].orElse((JsPath \ "expires_in").read[String].map(_.toLong)) and
    (JsPath \ "id_token").read[String]
  )(Token.apply _)

  def fromJson(json:JsValue):Token = {
    Json.fromJson[Token](json).get
  }
}

case class JwtClaims(iss: String, sub: String, azp: Option[String], email: Option[String], at_hash: String,
                     email_verified: Option[Boolean], aud: String, hd: Option[String], iat: Long, exp: Long)
object JwtClaims {
  implicit val claimsReads: Reads[JwtClaims] = Json.reads[JwtClaims]
}

case class UserInfo(sub: Option[String], name: String, given_name: String, family_name: String, profile: Option[String],
                    picture: Option[String], email: String, locale: Option[String], hd: Option[String])
object UserInfo {
  implicit val userInfoReads: Reads[UserInfo] = Json.reads[UserInfo]

  def fromJson(json:JsValue):UserInfo = {
    json.as[UserInfo]
  }
}

case class JsonWebToken(jwt: String) {
  val jwtParts: Array[String] = jwt.split('.')
  val Array(headerJson, claimsJson) = jwtParts.take(2).map(Base64.decodeBase64).map(Json.parse)
  val claims = claimsJson.as[JwtClaims]
}

case class ErrorInfo(domain: String, reason: String, message: String)
object ErrorInfo {
  implicit val errorInfoReads: Reads[ErrorInfo] = Json.reads[ErrorInfo]
}
case class Error(errors: Seq[ErrorInfo], code: Int, message: String)
object Error {
  implicit val errorReads: Reads[Error] = Json.reads[Error]
}
