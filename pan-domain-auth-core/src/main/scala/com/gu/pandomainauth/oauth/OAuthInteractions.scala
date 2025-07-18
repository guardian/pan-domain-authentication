package com.gu.pandomainauth.oauth

import cats.Monad
import com.gu.pandomainauth.model.OAuthSettings
import com.gu.pandomainauth.oauth.OAuthCodeToUser.TokenRequestParamsGenerator

import java.net.URI

case class OAuthInteractions[F[_] : Monad](
  providerUrl: OAuthUrl,
  codeToUser: OAuthCodeToUser[F],
)

object OAuthInteractions {
  case class AppSpecifics[F[_] : Monad](httpClient: OAuthHttpClient[F], authCallbackUrl: URI)

  def apply[F[_] : Monad](
    system: String,
    oAuthSettings: OAuthSettings,
    appSpecifics: AppSpecifics[F]
  ): OAuthInteractions[F] = {
    val ddCache = DiscoveryDocument.Cache

    new OAuthInteractions(
      new OAuthUrl(
        oAuthSettings.clientId,
        appSpecifics.authCallbackUrl,
        organizationDomain = Some("guardian.co.uk"), // TODO configure this via oAuthSettings, ie the .settings file
        authorizationEndpoint = () => ddCache.get().authorizationEndpoint
      ),
      new OAuthCodeToUser(
        TokenRequestParamsGenerator(oAuthSettings, appSpecifics.authCallbackUrl),
        system,
        appSpecifics.httpClient,
        () => ddCache.get()
      )
    )

  }
}