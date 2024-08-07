package com.gu.pandomainauth.model

import com.gu.pandomainauth.service.Crypto

import java.security.KeyPair

case class PanDomainAuthSettings(
  signingKeyPair: KeyPair,
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
  serviceAccountCert: String,
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
      serviceAccountCert <- settingMap.get("googleServiceAccountCert");
      adminUser          <- settingMap.get("google2faUser");
      group              <- settingMap.get("multifactorGroupId")
    ) yield {
      Google2FAGroupSettings(serviceAccountId, serviceAccountCert, adminUser, group)
    }

    PanDomainAuthSettings(
      Crypto.keyPairFrom(settingMap),
      cookieSettings,
      oAuthSettings,
      google2faSettings
    )
  }
}
