import { Request, Response, Router } from "express";
import type { Handler } from "express";
import { PanDomainAuthenticationIssuer } from ".";
import { AuthenticationStatus } from "@guardian/pan-domain-node";
import crypto from 'crypto';

import fetch from 'node-fetch';

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
};

const LOGIN_ORIGIN_KEY = "panda-loginOriginUrl"
const ANTI_FORGERY_KEY = "panda-antiForgeryToken"

const cookieOpts = {
  secure: true,
  httpOnly: true,
  sameSite: 'none'
} as const;

export const build = (panda: PanDomainAuthenticationIssuer, logger: Logger, system: string) => {

  const discoveryDocument: Promise<DiscoveryDocument> = panda.get().then(pandaSettings =>
    fetch(pandaSettings.discoveryDocumentUrl)
      .then(dd => dd.json() as Promise<DiscoveryDocument>)
  );


  const buildAuthHandler = ({ onUnauthenticated, onExpired }: Args): Handler =>
    async (req, res, next) => {
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
            ? `User ${authed.user.email} is not authorised to use ${system}`
            : `Unknown user is not authorised to use ${system}`;
          return res.status(403).send(message);
        default:
          return next(new Error(""))
      }
    }

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
      redirect_uri: 'TODO',
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

  authEndpoints.get("/oauthCallback", (req, res) => {

  });

  return {
    authEndpoints,
    protect,
    protectApi
  }
}



