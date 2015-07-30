package com.gu.pandomainauth.model

case class User(firstName: String, lastName: String, email: String, avatarUrl: Option[String]) {
  def toJson = {
    s"""{"firstName": "$firstName", "lastName": "$lastName", "email": "$email" ${avatarUrl.map( u => s""", "avatarUrl": "$u" """).getOrElse("")} }"""
  }

  def emailDomain = email.split("@").last
  def username = email.split("@").head
}
