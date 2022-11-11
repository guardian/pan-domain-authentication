import type { User } from '@guardian/pan-domain-node';

declare module 'express-serve-static-core' {
  interface Request {
    panda?: { user?: User };
  }
}
