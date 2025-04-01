package com.gu.pandomainauth.oauth

import org.apache.commons.codec.binary.Base64
import upickle.default.*

case class JwtClaims(iss: String, sub: String, azp: Option[String], email: Option[String], at_hash: String,
                     email_verified: Option[Boolean], aud: String, hd: Option[String], iat: Long, exp: Long)
object JwtClaims {
  implicit val claimsRW: ReadWriter[JwtClaims] = macroRW[JwtClaims]
}

object JsonWebToken {
  
  def claimsFrom(jwt: String): JwtClaims = {
    val jwtParts = jwt.split('.')
    read[JwtClaims](Base64.decodeBase64(jwtParts(1)))
  }
}

case class ErrorInfo(domain: String, reason: String, message: String)
object ErrorInfo {
  implicit val errorInfoRW: ReadWriter[ErrorInfo] = macroRW[ErrorInfo]
}

case class Error(errors: Seq[ErrorInfo], code: Int, message: String)
object Error {
  implicit val errorRW: ReadWriter[Error] = macroRW[Error]
}
