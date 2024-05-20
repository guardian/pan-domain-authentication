import "source-map-support/register";
import {GuRoot} from "@guardian/cdk/lib/constructs/root";
import {PanDomainAuthentication} from "../lib/pan-domain-authentication";

const stack = "pan-domain-authentication";
const env = {region: "eu-west-1"};

const app = new GuRoot();

new PanDomainAuthentication(app, "PanDomainAuthentication-euwest-1-LOCAL", {
  stack,
  env,
  stage: "LOCAL"
});

new PanDomainAuthentication(app, "PanDomainAuthentication-euwest-1-CODE", {
  stack,
  env,
  stage: "CODE"
});

new PanDomainAuthentication(app, "PanDomainAuthentication-euwest-1-PROD", {
  stack,
  env,
  stage: "PROD"
});
