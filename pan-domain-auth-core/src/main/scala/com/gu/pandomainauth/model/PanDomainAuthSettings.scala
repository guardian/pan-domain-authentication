package com.gu.pandomainauth.model

case class PanDomainAuthSettings(
  secret: String,
  cookieName: String,
  googleAuthSettings: GoogleAuthSettings,
  google2FAGroupSettings: Option[Google2FAGroupSettings]
) {

}

case class GoogleAuthSettings(
  googleAuthClient: String,
  googleAuthSecret: String
)

case class Google2FAGroupSettings(
  googleUser: String,
  googlePassword: String,
  googleAppDomain: String,
  multifactorGroupId: String
)

object PanDomainAuthSettings{

  def apply(settingMap: Map[String, String]): PanDomainAuthSettings = {

    val googleAuthSettings = GoogleAuthSettings(settingMap("googleAuthClientId"), settingMap("googleAuthSecret"))

    val google2faSettings = for(
      clientId <- settingMap.get("google2faUser");
      secret   <- settingMap.get("google2faPassword");
      domain   <- settingMap.get("google2faAppDomain");
      group    <- settingMap.get("multifactorGroupId")
    ) yield {
      Google2FAGroupSettings(clientId, secret, domain, group)
    }

    PanDomainAuthSettings(settingMap("secret"), settingMap("cookieName"), googleAuthSettings, google2faSettings)
  }
}