import { Request, Response, Router } from "express";
import type { Handler } from "express";
import { PanDomainAuthenticationIssuer, PanDomainSettings } from ".";
import { AuthenticationStatus, User } from "@guardian/pan-domain-node";
import crypto from 'crypto';

import fetch from 'node-fetch';
import cookieParser from 'cookie-parser';

import * as jose from 'jose';

type ResponsePlan = (req: Request, res: Response) => void;

type Args = {
  onUnauthenticated: ResponsePlan;
  onExpired: ResponsePlan;
};

type Logger = {
  info: (msg: string) => void;
  warn: (msg: string) => void;
  error: (msg: string) => void;
}

type DiscoveryDocument = {
  authorization_endpoint: string;
  token_endpoint: string;
  userinfo_endpoint: string;
  jwks_uri: string;
  issuer: string;
};

type UserInfo = {
  name: string;
  given_name: string;
  family_name: string;
  picture?: string;
  email: string;
};

type TokenResponse = {
  access_token: string;
  token_type: string;
  expires_in: number;
  id_token: string;
};

const LOGIN_ORIGIN_KEY = "panda-loginOriginUrl"
const ANTI_FORGERY_KEY = "panda-antiForgeryToken"

const cookieOpts = {
  secure: true,
  httpOnly: true,
  sameSite: 'none'
} as const;

export const buildRouter = (panda: PanDomainAuthenticationIssuer, logger: Logger, system: string) => {

  const discoveryDocument: Promise<DiscoveryDocument> = panda.get().then(pandaSettings =>
    fetch(pandaSettings.discoveryDocumentUrl)
      .then(dd => dd.json() as Promise<DiscoveryDocument>)
  );

  const jwks = discoveryDocument.then(dd =>
    jose.createRemoteJWKSet(new URL(dd.jwks_uri))
  );


  const buildAuthHandler = ({ onUnauthenticated, onExpired }: Args): Handler => {
    return async (req, res, next) => {
      const authed = await panda.verify(req.headers.cookie ?? '');

      console.log(authed.status);
      console.log(authed.user);

      switch (authed.status) {
        case AuthenticationStatus.AUTHORISED:
          req.panda = { user: authed.user };
          return next();
        case AuthenticationStatus.EXPIRED:
          req.panda = { user: authed.user };
          return onExpired(req, res);
        case AuthenticationStatus.NOT_AUTHENTICATED:
          return onUnauthenticated(req, res);
        case AuthenticationStatus.NOT_AUTHORISED:
          const message = authed.user
            ? `User ${authed.user.email} is not authorised to use ${system}`
            : `Unknown user is not authorised to use ${system}`;
          return res.status(403).send(message);
        default:
          return next(new Error("TODO"))
      }
    }
  };

  // https://developers.google.com/identity/openid-connect/openid-connect#createxsrftoken
  const generateAntiforgeryToken = (): string =>
    crypto.randomBytes(30).toString('base64url')

  const sendForAuthentication = async (req: Request, res: Response) => {
    const antiforgeryToken = generateAntiforgeryToken();
    const pandaSettings = await panda.get();

    const queryParams: Record<string, string> = {
      client_id: pandaSettings.clientId,
      response_type: 'code',
      scope: 'openid email profile',
      redirect_uri: panda.redirectUrl,
      state: antiforgeryToken,
    }
    if (req?.panda?.user) {
      queryParams.login_hint = req.panda.user.email;
    }
    if (pandaSettings.organizationDomain) {
      queryParams.hd = pandaSettings.organizationDomain;
    }

    const query = new URLSearchParams(queryParams).toString();

    const loginUri = (await discoveryDocument).authorization_endpoint + '?' + query;

    res.cookie(LOGIN_ORIGIN_KEY, req.originalUrl, cookieOpts)
      .cookie(ANTI_FORGERY_KEY, antiforgeryToken, cookieOpts)
      .redirect(loginUri);
  };

  const protect: Handler = buildAuthHandler({
    onUnauthenticated: (req, res) => sendForAuthentication(req, res),
    onExpired: (req, res) => sendForAuthentication(req, res),
  });
  const protectApi: Handler = buildAuthHandler({
    onUnauthenticated: (_, res) => res.status(401).send(),
    onExpired: (_, res) => res.status(419).send(),
  });

  const authEndpoints = Router();

  authEndpoints.get("/auth/status", protect, (req, res) => {
    logger.info(`User ${req.panda?.user ?? 'unknown'} is successfully authenticated`);
    res.status(200).send("You are logged in.");
  });

  const makeAuthorizationRequest = async (code: string, pandaSettings: PanDomainSettings, discoveryDoc: DiscoveryDocument) => {
    const bodyParams = new URLSearchParams({
      code: code,
      client_id: pandaSettings.clientId,
      client_secret: pandaSettings.clientSecret,
      redirect_uri: panda.redirectUrl,
      grant_type: 'authorization_code',
    });

    const url = discoveryDoc.token_endpoint;// + '?' + new URLSearchParams(queryParams).toString();

    const authorization_resp = await fetch(url, { method: 'POST', body: bodyParams });

    let authorization;
    console.log(authorization_resp.status);
    console.log(url);
    try {
      authorization = await authorization_resp.json() as TokenResponse;
    } catch (e) {
      console.log(await authorization_resp.text());
      throw e;
    }

    const { payload } = await jose.jwtVerify(authorization.id_token, await jwks, {
      issuer: discoveryDoc.issuer,
      audience: pandaSettings.clientId
    });

    return { authorization, jwtPayload: payload };
  };

  const makeUserInfoRequest = async (discoveryDoc: DiscoveryDocument, accessToken: string): Promise<UserInfo> => {
    const headers = { 'Authorization': `Bearer ${accessToken}` };

    const userInfo = await (await fetch(discoveryDoc.userinfo_endpoint, { headers })).json() as UserInfo;

    return userInfo;
  };


  const validateUserIdentity = async (expectedAntiForgeryToken: string, req: Request, panda: PanDomainSettings): Promise<User> => {
    if (req.query.state !== expectedAntiForgeryToken) {
      throw new Error('Anti forgery token did not match');
    }

    const dd = await discoveryDocument;

    const { authorization, jwtPayload } = await makeAuthorizationRequest(req.query.code as string, panda, dd);

    const userInfo = await makeUserInfoRequest(dd, authorization.access_token);

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

  authEndpoints.get("/oauthCallback", cookieParser(), async (req, res) => {
    const pandaSettings = await panda.get();
    const token = req.cookies[ANTI_FORGERY_KEY];
    const originalUrl = req.cookies[LOGIN_ORIGIN_KEY];

    const oldCookie = req.cookies[pandaSettings.cookieName];

    const authenticatedUser = await validateUserIdentity(token, req, pandaSettings);

    const existingAuth = await readOldAuthData(oldCookie);

    const existingAuthHasSystem = existingAuth?.authenticatedIn.includes(system);

    if (existingAuth) {
      authenticatedUser.authenticatedIn = existingAuth.authenticatedIn;
      if (!existingAuthHasSystem) {
        authenticatedUser.authenticatedIn.push(system);
      }

      if (existingAuth.multifactor) {
        authenticatedUser.multifactor = true;
      }
    }

    // FIXME check 2fa here
    //
    if (!authenticatedUser.multifactor) {
      authenticatedUser.multifactor = true;
    }

    if (panda.validateUser(authenticatedUser)) {
      res.cookie(panda.cookieName, await panda.generateCookie(authenticatedUser), {
        domain: panda.domain,
        secure: true,
        httpOnly: true,
        encode: s => s
      })
        .clearCookie(ANTI_FORGERY_KEY)
        .clearCookie(LOGIN_ORIGIN_KEY)
        .redirect(originalUrl);
    } else {
      res.status(403).send()
    }


  });

  const readOldAuthData = async (oldCookie: string | undefined): Promise<User | undefined> => {
    if (!oldCookie) return;
    try {
      return (await panda.verify(oldCookie)).user;
    } catch {
      return;
    }
  };

  return {
    authEndpoints,
    protect,
    protectApi
  }
}



