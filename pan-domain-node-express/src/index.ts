import { Request, Response, Router } from "express";
import type { Handler } from "express";
import { PanDomainAuthenticationIssuer } from "@guardian/pan-domain-node";
import { AuthenticationStatus, User } from "@guardian/pan-domain-node";
import crypto from 'crypto';

import fetch from 'node-fetch';
import cookieParser from 'cookie-parser';

import * as jose from 'jose';
import { DiscoveryDocument, Logger } from "../types/express-panda";
import { validateUserIdentity } from "./utils";

type ResponsePlan = (req: Request, res: Response) => void;

type Args = {
  onUnauthenticated: ResponsePlan;
  onExpired: ResponsePlan;
};

const LOGIN_ORIGIN_KEY = "panda-loginOriginUrl"
const ANTI_FORGERY_KEY = "panda-antiForgeryToken"

const cookieOpts = {
  secure: true,
  httpOnly: true,
  sameSite: 'none'
} as const;

export const pandaExpress = (panda: PanDomainAuthenticationIssuer, logger: Logger) => {

  const discoveryDocument: Promise<DiscoveryDocument> = panda.get()
    .then(pandaSettings => fetch(pandaSettings.discoveryDocumentUrl))
    .then(dd => dd.json() as Promise<DiscoveryDocument>);

  const jwks = discoveryDocument.then(dd =>
    jose.createRemoteJWKSet(new URL(dd.jwks_uri))
  );


  const buildAuthHandler = ({ onUnauthenticated, onExpired }: Args): Handler => {
    return async (req, res, next) => {
      const authed = await panda.verify(req.headers.cookie ?? '');

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
            ? `User ${authed.user.email} is not authorised to use ${panda.system}`
            : `Unknown user is not authorised to use ${panda.system}`;
          return res.status(403).send(message);
        default:
          return next(new Error(`Authenticating user failed with an unexpected cause. Authentication status was ${authed.status}`));
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

  const auth: Handler = buildAuthHandler({
    onUnauthenticated: (req, res) => sendForAuthentication(req, res),
    onExpired: (req, res) => sendForAuthentication(req, res),
  });
  const authApi: Handler = buildAuthHandler({
    onUnauthenticated: (_, res) => res.status(401).send(),
    onExpired: (_, res) => res.status(419).send(),
  });

  const authEndpoints = Router();

  authEndpoints.get("/auth/status", auth, (req, res) => {
    logger.info(`User ${req.panda?.user ?? 'unknown'} is successfully authenticated`);
    res.status(200).send("You are logged in.");
  });


  authEndpoints.get("/oauthCallback", cookieParser(), async (req, res) => {
    const pandaSettings = await panda.get();
    const antiForgeryToken = req.cookies[ANTI_FORGERY_KEY];
    const originalUrl = req.cookies[LOGIN_ORIGIN_KEY];

    const oldCookie = req.cookies[pandaSettings.cookieName];

    const authenticatedUser = await validateUserIdentity({
      expectedAntiForgeryToken: antiForgeryToken,
      state: req.query.state as string,
      authorizationCode: req.query.code as string,
      pandaSettings,
      discoveryDocument: await discoveryDocument,
      redirectUrl: panda.redirectUrl,
      jwks: await jwks,
      system: panda.system,
      logger
    });

    const existingAuth = await readOldAuthData(oldCookie);

    const existingAuthHasSystem = existingAuth?.authenticatedIn.includes(panda.system);

    if (existingAuth) {
      authenticatedUser.authenticatedIn = existingAuth.authenticatedIn;
      if (!existingAuthHasSystem) {
        authenticatedUser.authenticatedIn.push(panda.system);
      }

      if (existingAuth.multifactor) {
        authenticatedUser.multifactor = true;
      }
    }

    // FIXME check 2fa here
    if (!authenticatedUser.multifactor) {
      authenticatedUser.multifactor = true;
    }

    if (panda.validateUser(authenticatedUser)) {
      res.cookie(panda.cookieName, await panda.generateCookie(authenticatedUser), {
        domain: panda.domain,
        secure: true,
        httpOnly: true,
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
    auth,
    authApi
  }
}
