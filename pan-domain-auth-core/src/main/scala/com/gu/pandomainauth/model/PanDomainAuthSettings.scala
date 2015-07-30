package com.gu.pandomainauth.model

import com.gu.pandomainauth.PublicSettings

case class PanDomainAuthSettings(
  secret: String,
  publicKey: String,
  privateKey: String,
  googleAuthSettings: GoogleAuthSettings,
  google2FAGroupSettings: Option[Google2FAGroupSettings]
) {
  @deprecated("Use com.gu.pandomainauth.PublicSettings.cookieName", "0.2.7")
  val cookieName = PublicSettings.cookieName

  @deprecated("Use com.gu.pandomainauth.PublicSettings.assymCookieName", "0.2.7")
  val assymCookieName = PublicSettings.assymCookieName
}

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

    val googleAuthSettings = GoogleAuthSettings(settingMap("googleAuthClientId"), settingMap("googleAuthSecret"))

    val google2faSettings = for(
      serviceAccountId   <- settingMap.get("googleServiceAccountId");
      serviceAccountCert <- settingMap.get("googleServiceAccountCert");
      adminUser          <- settingMap.get("google2faUser");
      group              <- settingMap.get("multifactorGroupId")
    ) yield {
      Google2FAGroupSettings(serviceAccountId, serviceAccountCert, adminUser, group)
    }

    PanDomainAuthSettings(settingMap("secret"), settingMap("publicKey"), settingMap("privateKey"), googleAuthSettings, google2faSettings)
  }
}