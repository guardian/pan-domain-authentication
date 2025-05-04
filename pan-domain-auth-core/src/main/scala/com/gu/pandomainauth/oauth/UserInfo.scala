package com.gu.pandomainauth.oauth

/*
 * https://www.oauth.com/oauth2-servers/signing-in-with-google/verifying-the-user-info/
 */
case class UserInfo(sub: Option[String], name: String, given_name: String, family_name: String, profile: Option[String],
  picture: Option[String], email: String, locale: Option[String], hd: Option[String])
