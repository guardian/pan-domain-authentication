import { App } from "aws-cdk-lib";
import { Template } from "aws-cdk-lib/assertions";
import { PanDomainAuthentication } from "./pan-domain-authentication";

describe("The PanDomainAuthentication stack", () => {
  it("matches the snapshot", () => {
    const app = new App();
    const stack = new PanDomainAuthentication(app, "PanDomainAuthentication", { stack: "pan-domain-authentication", stage: "TEST" });
    const template = Template.fromStack(stack);
    expect(template.toJSON()).toMatchSnapshot();
  });
});
