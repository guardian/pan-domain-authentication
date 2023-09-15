import express from "express";

import { S3Client } from '@aws-sdk/client-s3'
import { fromIni } from '@aws-sdk/credential-providers';

import { pandaExpress } from '@guardian/pan-domain-node-express';
import { guardianValidation, PanDomainAuthenticationIssuer } from "@guardian/pan-domain-node";

const app = express();

const s3 = new S3Client({
  region: 'eu-west-1',
  credentials: fromIni({ profile: 'media-service' }),
});

const panda = new PanDomainAuthenticationIssuer({
  cookieName: 'gutoolsAuth-assym',
  region: 'eu-west-1',
  bucket: 'pan-domain-auth-settings',
  keyFile: 'local.dev-gutools.co.uk.settings',
  validateUser: guardianValidation,
  s3: s3,
  domain: '.local.dev-gutools.co.uk',
  redirectUrl: 'https://example.local.dev-gutools.co.uk/oauthCallback',
  system: "panda-node-example"
});

const logger = {
  info: console.log,
  warn: console.warn,
  error: console.error,
};

const pandaMiddleware = pandaExpress(panda, logger);

app.use(pandaMiddleware.authEndpoints);

app.get('/', (req, res) => {
  res.send('hello world');
});

app.get('/authed/api', pandaMiddleware.authApi, (req, res) => {
  res.send('authed on an api endpoint! yay! you are ' + req.panda?.user?.email);
});
app.get('/authed', pandaMiddleware.auth, (req, res) => {
  res.send(`<p>
    authed on an html endpoint! wheeee! you are ${req.panda?.user?.email} <img src="${req.panda?.user?.avatarUrl}" />
  </p>`);
});

app.listen(7734, () => {
  console.log('listening on 7734, https://example.local.dev-gutools.co.uk');
});
