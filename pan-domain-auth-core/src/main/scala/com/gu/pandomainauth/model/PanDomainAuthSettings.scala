package com.gu.pandomainauth.model

import com.gu.pandomainauth.{PrivateKey, PublicKey, Secret}

case class PanDomainAuthSettings(
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
  discoveryDocumentUrl: String,
  organizationDomain: Option[String]
)

case class Google2FAGroupSettings(
  serviceAccountId: String,
  // The S3 key that contains the Google Service Account JSON credentials.
  serviceAccountJson: String,
  adminUserEmail: String,
  multifactorGroupId: String
)

object PanDomainAuthSettings{
  private val legacyCookieNameSetting = "assymCookieName"

  def apply(settingMap: Map[String, String]): PanDomainAuthSettings = {
    val cookieSettings = CookieSettings(
      cookieName = settingMap.getOrElse(legacyCookieNameSetting, settingMap("cookieName"))
    )

    val oAuthSettings = OAuthSettings(
      settingMap("clientId"),
      settingMap("clientSecret"),
      settingMap("discoveryDocumentUrl"),
      settingMap.get("organizationDomain")
    )

    val google2faSettings = for(
      serviceAccountId   <- settingMap.get("googleServiceAccountId");
      serviceAccountJson <- settingMap.get("googleServiceAccountJson");
      adminUser          <- settingMap.get("google2faUser");
      group              <- settingMap.get("multifactorGroupId")
    ) yield {
      Google2FAGroupSettings(serviceAccountId, serviceAccountJson, adminUser, group)
    }

    PanDomainAuthSettings(
      PublicKey(settingMap("publicKey")),
      PrivateKey(settingMap("privateKey")),
      cookieSettings,
      oAuthSettings,
      google2faSettings
    )
  }
}
