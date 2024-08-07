package com.gu.pandomainauth.model

import com.gu.pandomainauth.SettingsFailure.SettingsResult
import com.gu.pandomainauth.service.CryptoConf
import com.gu.pandomainauth.service.CryptoConf.SigningAndVerification

case class PanDomainAuthSettings(
  signingAndVerification: SigningAndVerification,
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

  def apply(settingMap: Map[String, String]): SettingsResult[PanDomainAuthSettings] = {
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
    ) yield Google2FAGroupSettings(serviceAccountId, serviceAccountCert, adminUser, group)

    for {
      cryptoConf <- CryptoConf.SettingsReader(settingMap).signingAndVerificationConf
    } yield PanDomainAuthSettings(
      cryptoConf,
      cookieSettings,
      oAuthSettings,
      google2faSettings
    )
  }
}
