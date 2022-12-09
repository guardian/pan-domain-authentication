import { PanDomainSettings, User } from "@guardian/pan-domain-node";
import { jwtVerify, JWTVerifyGetKey } from "jose";
import type { DiscoveryDocument, Logger, TokenResponse, UserInfo } from "../types/express-panda";

import fetch from 'node-fetch';

type ValidateUserIdentityParams = {
  expectedAntiForgeryToken: string,
  state: string,
  system: string,
} & AuthorizationRequestParams;
export const validateUserIdentity = async ({
  expectedAntiForgeryToken,
  state,
  authorizationCode,
  pandaSettings,
  discoveryDocument,
  redirectUrl,
  jwks,
  system,
  logger
}: ValidateUserIdentityParams): Promise<User> => {
  if (state !== expectedAntiForgeryToken) {
    throw new Error('Anti forgery token did not match');
  }

  const { authorization, jwtPayload } = await makeAuthorizationRequest({
    authorizationCode, pandaSettings, discoveryDocument, redirectUrl, jwks, logger
  });

  const userInfo = await makeUserInfoRequest(discoveryDocument, authorization.access_token);

  return {
    firstName: userInfo.given_name,
    lastName: userInfo.family_name,
    email: jwtPayload.email as (string | undefined) || userInfo.email,
    avatarUrl: userInfo.picture,
    authenticatingSystem: system,
    authenticatedIn: [system],
    expires: jwtPayload.exp as number * 1000,
    multifactor: false
  };
};

type AuthorizationRequestParams = {
  authorizationCode: string,
  pandaSettings: PanDomainSettings,
  discoveryDocument: DiscoveryDocument,
  redirectUrl: string,
  jwks: JWTVerifyGetKey,
  logger: Logger,
};
const makeAuthorizationRequest = async ({
  authorizationCode,
  pandaSettings,
  discoveryDocument,
  redirectUrl,
  jwks,
  logger
}: AuthorizationRequestParams) => {
  const bodyParams = new URLSearchParams({
    code: authorizationCode,
    client_id: pandaSettings.clientId,
    client_secret: pandaSettings.clientSecret,
    redirect_uri: redirectUrl,
    grant_type: 'authorization_code',
  });

  const authorizationResponse = await fetch(discoveryDocument.token_endpoint, { method: 'POST', body: bodyParams });

  let authorization;
  try {
    if (!authorizationResponse.ok) {
      throw new Error("Fetching authorization from token endpoint failed: " + authorizationResponse.status);
    }
    authorization = await authorizationResponse.json() as TokenResponse;
  } catch (e) {
    logger.error(await authorizationResponse.text());
    throw e;
  }

  const { payload } = await jwtVerify(authorization.id_token, jwks, {
    issuer: discoveryDocument.issuer,
    audience: pandaSettings.clientId
  });

  return { authorization, jwtPayload: payload };
};

const makeUserInfoRequest = async (discoveryDoc: DiscoveryDocument, accessToken: string): Promise<UserInfo> => {
  const headers = { 'Authorization': `Bearer ${accessToken}` };

  const userInfo = await (await fetch(discoveryDoc.userinfo_endpoint, { headers })).json() as UserInfo;

  return userInfo;
};
