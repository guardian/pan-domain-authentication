import express from "express";

import { S3Client } from '@aws-sdk/client-s3'
import { fromIni } from '@aws-sdk/credential-providers';

import { buildRouter } from '@guardian/pan-domain-node-express';
import { guardianValidation, PanDomainAuthenticationIssuer } from "@guardian/pan-domain-node";

const app = express();

const s3 = new S3Client({
  region: 'eu-west-1',
  credentials: fromIni({ profile: 'media-service' }),
});

const panda = new PanDomainAuthenticationIssuer(
  'gutoolsAuth-assym',
  'eu-west-1',
  'pan-domain-auth-settings',
  'local.dev-gutools.co.uk.settings',
  guardianValidation,
  s3,
  '.local.dev-gutools.co.uk',
  'https://example.local.dev-gutools.co.uk/oauthCallback',
  "panda-node-example"
)

const logger = {
  info: console.log,
  warn: console.warn,
  error: console.error,
};

const authRouter = buildRouter(panda, logger);

app.use(authRouter.authEndpoints);

app.get('/', (req, res) => {
  res.send('hello world');
});

app.get('/authed/api', authRouter.authApi, (req, res) => {
  res.send('authed on an api endpoint! yay! you are ' + req.panda?.user?.email);
});
app.get('/authed', authRouter.auth, (req, res) => {
  res.send('authed an html endpoint! yayer! you are ' + req.panda?.user?.avatarUrl);
});

app.listen(7734, () => {
  console.log('listening on 7734, https://example.local.dev-gutools.co.uk');
});
