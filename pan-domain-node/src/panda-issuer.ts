import { ValidateUserFn, Refreshable, AuthenticationResult, verifyUser, serialiseUser, User, base64ToPEM } from "./api";
import { GetObjectCommand, GetObjectCommandOutput, S3Client } from "@aws-sdk/client-s3"
import { text } from "node:stream/consumers";
import * as iniparser from 'iniparser';
import * as cookie from 'cookie';
import assert from "node:assert";
import crypto from 'node:crypto';


type Google2FAGroupSettings = {
  googleServiceAccountId: string;
  googleServiceAccountCert: string;
  google2faUser: string;
  multifactorGroupId: string;
}
export type PanDomainSettings = {
  cookieName: string;
  clientId: string;
  clientSecret: string;
  discoveryDocumentUrl: string;
  organizationDomain?: string;
  google2FAGroupSettings?: Google2FAGroupSettings;
  publicKey: string;
  privateKey: string;
}
export class PanDomainAuthenticationIssuer extends Refreshable<PanDomainSettings> {
  cookieName: string;
  region: string;
  bucket: string;
  keyFile: string;
  validateUser: ValidateUserFn;
  s3: S3Client;
  domain: string;
  redirectUrl: string;
  system: string;

  static settingsCacheTime: number = 60 * 1000;

  constructor(
    cookieName: string,
    region: string,
    bucket: string,
    keyFile: string,
    validateUser: ValidateUserFn,
    s3: S3Client,
    domain: string,
    redirectUrl: string,
    system: string
  ) {
    super(PanDomainAuthenticationIssuer.settingsCacheTime);

    this.cookieName = cookieName;
    this.region = region;
    this.bucket = bucket;
    this.keyFile = keyFile;
    this.validateUser = validateUser;
    this.s3 = s3;
    this.domain = domain;
    this.redirectUrl = redirectUrl;
    this.system = system;
  }

  private validateSettingsFile(settings: Partial<PanDomainSettings>): PanDomainSettings {
    assert(settings.cookieName !== undefined, 'Failed to parse cookieName from panda settings file!');
    assert(settings.clientId !== undefined, 'Failed to parse clientId from panda settings file!');
    assert(settings.clientSecret !== undefined, 'Failed to parse clientSecret from panda settings file!');
    assert(settings.discoveryDocumentUrl !== undefined, 'Failed to parse discoveryDocumentUrl from panda settings file!');
    assert(settings.publicKey !== undefined, 'Failed to parse publicKey from panda settings file!');
    assert(settings.privateKey !== undefined, 'Failed to parse privateKey from panda settings file!');

    settings.publicKey = base64ToPEM(settings.publicKey, 'PUBLIC');
    settings.privateKey = base64ToPEM(settings.privateKey, 'RSA PRIVATE');

    if (settings.google2FAGroupSettings !== undefined) {
      assert(settings.google2FAGroupSettings.google2faUser !== undefined, 'Failed to parse google2faUser from panda settings file!');
      assert(settings.google2FAGroupSettings.googleServiceAccountCert !== undefined, 'Failed to parse googleServiceAccountCert from panda settings file!');
      assert(settings.google2FAGroupSettings.googleServiceAccountId !== undefined, 'Failed to parse googleServiceAccountId from panda settings file!');
      assert(settings.google2FAGroupSettings.multifactorGroupId !== undefined, 'Failed to parse multifactorGroupId from panda settings file!');
    }

    return settings as PanDomainSettings;
  }

  override async refresh(): Promise<PanDomainSettings> {
    const getObj = new GetObjectCommand({
      Bucket: this.bucket,
      Key: this.keyFile,
    });
    const obj: GetObjectCommandOutput = await this.s3.send(getObj);
    if (!obj.Body) throw new Error('no body on s3 response!');
    const textBody = await text(obj.Body as NodeJS.ReadableStream);
    const settings: Partial<PanDomainSettings> = iniparser.parseString(textBody);

    return this.validateSettingsFile(settings);
  }

  async verify(requestCookies: string): Promise<AuthenticationResult> {
    const settings = await this.get();
    const publicKey = settings.publicKey;
    const cookies = cookie.parse(requestCookies);
    const pandaCookie = cookies[this.cookieName];
    const now = new Date().getTime();
    return verifyUser(pandaCookie, publicKey, now, this.validateUser);
  }

  async generateCookie(user: User): Promise<string> {
    const data = Buffer.from(serialiseUser(user), 'utf8');
    const encodedData = data.toString('base64');

    const signedData = crypto.sign('sha256WithRSAEncryption', data, (await this.get()).privateKey);
    const encodedSignature = signedData.toString('base64');

    return encodedData + '.' + encodedSignature;
  };

}
