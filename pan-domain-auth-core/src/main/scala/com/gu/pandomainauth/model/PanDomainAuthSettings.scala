package com.gu.pandomainauth.model

import com.gu.pandomainauth.{PrivateKey, PublicKey, Secret}

case class PanDomainAuthSettings(
  secret: Secret,
  publicKey: PublicKey,
  privateKey: PrivateKey,
  cookieSettings: CookieSettings,
  googleAuthSettings: GoogleAuthSettings,
  google2FAGroupSettings: Option[Google2FAGroupSettings]
)

case class CookieSettings(
  cookieName: String,
  assymCookieName: String
)

case class GoogleAuthSettings(
  googleAuthClient: String,
  googleAuthSecret: String
)

case class Google2FAGroupSettings(
  serviceAccountId: String,
  serviceAccountCert: String,
  adminUserEmail: String,
  multifactorGroupId: String
)

object PanDomainAuthSettings{

  def apply(settingMap: Map[String, String]): PanDomainAuthSettings = {
    val cookieSettings = CookieSettings(
      cookieName = settingMap("cookieName"),
      assymCookieName = settingMap("assymCookieName")
    )

    val googleAuthSettings = GoogleAuthSettings(settingMap("googleAuthClientId"), settingMap("googleAuthSecret"))

    val google2faSettings = for(
      serviceAccountId   <- settingMap.get("googleServiceAccountId");
      serviceAccountCert <- settingMap.get("googleServiceAccountCert");
      adminUser          <- settingMap.get("google2faUser");
      group              <- settingMap.get("multifactorGroupId")
    ) yield {
      Google2FAGroupSettings(serviceAccountId, serviceAccountCert, adminUser, group)
    }

    PanDomainAuthSettings(
      Secret(settingMap("secret")),
      PublicKey(settingMap("publicKey")),
      PrivateKey(settingMap("privateKey")),
      cookieSettings,
      googleAuthSettings,
      google2faSettings
    )
  }
}