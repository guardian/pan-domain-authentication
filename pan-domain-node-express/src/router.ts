import { Router } from "express";
import type { Handler, Request } from "express";
import { PanDomainAuthenticationIssuer } from ".";
import { AuthenticationStatus } from "@guardian/pan-domain-node";

type Logger = {
    info: (msg: string) => void;
    warn: (msg: string) => void;
    error: (msg: string) => void;
}

export const build = (panda: PanDomainAuthenticationIssuer, logger: Logger, system: string) => {

    const protect: Handler = async (req, res, next) => {
        const authed = await panda.verify(req.headers.cookie ?? '');

        switch (authed.status) {
            case AuthenticationStatus.AUTHORISED:
                return next();
            case AuthenticationStatus.EXPIRED:
            case AuthenticationStatus.NOT_AUTHENTICATED:
                return res.redirect('/auth/status');
            case AuthenticationStatus.NOT_AUTHORISED:
                const message = authed.user 
                    ? `User ${authed.user.email} is not authorised to use ${system}`
                    : `Unknown user is not authorised to use ${system}`;
                return res.status(403).send(message);
            default:
                return next(new Error(""))
        }
    }

    const authEndpoints = Router();

    authEndpoints.get("/auth/status", (req, res) => {
        
    });

    return {
        authEndpoints,
    }
}



