package com.gu.pandomainauth.model

case class PanDomainAuthSettings(
  publicKey: String,
  privateKey: String,
  cookieName: String,
  googleAuthSettings: GoogleAuthSettings,
  google2FAGroupSettings: Option[Google2FAGroupSettings]
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

    val googleAuthSettings = GoogleAuthSettings(settingMap("googleAuthClientId"), settingMap("googleAuthSecret"))

    val google2faSettings = for(
      serviceAccountId   <- settingMap.get("googleServiceAccountId");
      serviceAccountCert <- settingMap.get("googleServiceAccountCert");
      adminUser          <- settingMap.get("google2faUser");
      group              <- settingMap.get("multifactorGroupId")
    ) yield {
      Google2FAGroupSettings(serviceAccountId, serviceAccountCert, adminUser, group)
    }

    PanDomainAuthSettings(settingMap("publicKey"), settingMap("privateKey"), settingMap("cookieName"), googleAuthSettings, google2faSettings)
  }
}