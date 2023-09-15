import type { User } from '@guardian/pan-domain-node';

declare module 'express-serve-static-core' {
  interface Request {
    panda?: { user?: User };
  }
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

type Logger = {
  info: (msg: string) => void;
  warn: (msg: string) => void;
  error: (msg: string) => void;
}
