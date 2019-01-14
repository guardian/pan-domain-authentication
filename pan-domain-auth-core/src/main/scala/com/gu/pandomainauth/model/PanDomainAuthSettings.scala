package com.gu.pandomainauth.model

import com.gu.pandomainauth.{PrivateKey, PublicKey, Secret}

case class PanDomainAuthSettings(
  secret: Secret,
  publicKey: PublicKey,
  privateKey: PrivateKey,
  cookieSettings: CookieSettings,
  oAuthSettings: OAuthSettings,
  google2FAGroupSettings: Option[Google2FAGroupSettings]
)

case class CookieSettings(
  cookieName: String
)

case class OAuthSettings(
  clientId: String,
  clientSecret: String,
  discoveryDocumentUrl: String
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
      // Try legacy setting name first for compatibility
      cookieName = settingMap.getOrElse("assymCookieName", settingMap("cookieName"))
    )

    val oAuthSettings = OAuthSettings(
      settingMap("clientId"),
      settingMap("clientSecret"),
      settingMap("discoveryDocumentUrl")
    )

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
      oAuthSettings,
      google2faSettings
    )
  }
}