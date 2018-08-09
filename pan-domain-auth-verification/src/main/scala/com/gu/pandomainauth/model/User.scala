package com.gu.pandomainauth.model

/**
  * 'id' is a stable identifier from the authentication provider that never changes for the lifetime of a user.
  * All other fields may change over time, including email.
  *
  * Calling code should never rely on the presence of an id as it cannot be provided in emergency login situations.
  * There should always be a functional fallback in each application by using 'email' instead.
  */
case class User(firstName: String, lastName: String, email: String, avatarUrl: Option[String], id: Option[String]) {
  def toJson = {
    s"""{"firstName": "$firstName", "lastName": "$lastName", "email": "$email", ${optionalFieldToJson(avatarUrl)} ${optionalFieldToJson(id)} }"""
  }

  def emailDomain = email.split("@").last
  def username = email.split("@").head

  private def optionalFieldToJson(field: Option[String]): String =
    field.map( u => s""", "avatarUrl": "$u" """).getOrElse("")
}
