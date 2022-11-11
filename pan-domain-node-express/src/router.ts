import { Response, Router } from "express";
import type { Handler } from "express";
import { PanDomainAuthenticationIssuer } from ".";
import { AuthenticationStatus } from "@guardian/pan-domain-node";

type ResponsePlan = (res: Response) => void;

type Args = {
  onUnauthenticated: ResponsePlan;
  onExpired: ResponsePlan;
};

type Logger = {
  info: (msg: string) => void;
  warn: (msg: string) => void;
  error: (msg: string) => void;
}

export const build = (panda: PanDomainAuthenticationIssuer, logger: Logger, system: string) => {

  const buildAuthHandler = ({onUnauthenticated, onExpired}: Args): Handler =>
    async (req, res, next) => {
      const authed = await panda.verify(req.headers.cookie ?? '');

      switch (authed.status) {
        case AuthenticationStatus.AUTHORISED:
          req.panda = { user: authed.user };
          return next();
        case AuthenticationStatus.EXPIRED:
          return onExpired(res);
        case AuthenticationStatus.NOT_AUTHENTICATED:
          return onUnauthenticated(res);
        case AuthenticationStatus.NOT_AUTHORISED:
          const message = authed.user
            ? `User ${authed.user.email} is not authorised to use ${system}`
            : `Unknown user is not authorised to use ${system}`;
          return res.status(403).send(message);
        default:
          return next(new Error(""))
      }
    }

  const sendForAuthentication = (res: Response) => {

  };

  const protect: Handler = buildAuthHandler({
    onUnauthenticated: res => sendForAuthentication(res),
    onExpired: res => sendForAuthentication(res),
  });
  const protectApi: Handler = buildAuthHandler({
    onUnauthenticated: res => res.status(401).send(),
    onExpired: res => res.status(419).send(),
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



