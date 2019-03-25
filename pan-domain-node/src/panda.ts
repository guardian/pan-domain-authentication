import * as iniparser from 'iniparser';
import * as cookie from 'cookie';

import { httpGet, base64ToPEM, parseCookie, verifySignature, parseUser } from './utils';
import { ValidateUserFn, AuthenticationStatus, User, AuthenticationResult } from './api';

interface PublicKeyHolder {
    key: string,
    lastUpdated: Date
}

function fetchPublicKey(region: string, bucket: String, keyFile: String): Promise<PublicKeyHolder> {
    const path = `https://s3.${region}.amazonaws.com/${bucket}/${keyFile}`;

    return httpGet(path).then(response => {
        const config: { publicKey?: string} = iniparser.parseString(response);
        
        if(config.publicKey) {
            return {
                key: base64ToPEM(config.publicKey),
                lastUpdated: new Date()
            };
        } else {
            throw new Error("Missing publicKey setting from config");
        }
    });
}

export function verifyUser(pandaCookie: string | undefined, publicKey: string, currentTimestamp: number, validateUser: ValidateUserFn): AuthenticationResult {
    if(!pandaCookie) {
        return { status: AuthenticationStatus.INVALID_COOKIE };
    }

    const { data, signature } = parseCookie(pandaCookie);

    if(!verifySignature(data, signature, publicKey)) {
        return { status: AuthenticationStatus.INVALID_COOKIE };
    }

    try {
        const user: User = parseUser(data);
        const isExpired = user.expires < currentTimestamp;

        if(isExpired) {
            return { status: AuthenticationStatus.EXPIRED, user };
        }

        if(!validateUser(user)) {
            return { status: AuthenticationStatus.NOT_AUTHORISED, user };
        }

        return { status: AuthenticationStatus.AUTHORISED, user };
    } catch(error) {
        console.error(error);
        return { status: AuthenticationStatus.INVALID_COOKIE };
    }
}

export class PanDomainAuthentication {
    cookieName: string;
    region: string;
    bucket: string;
    keyFile: string;
    validateUser: ValidateUserFn;

    publicKey: Promise<PublicKeyHolder>;
    keyCacheTime: number = 60 * 1000; // 1 minute
    keyUpdateTimer?: NodeJS.Timeout

    constructor(cookieName: string, region: string, bucket: string, keyFile: string, validateUser: ValidateUserFn) {
        this.cookieName = cookieName;
        this.region = region;
        this.bucket = bucket;
        this.keyFile = keyFile;
        this.validateUser = validateUser;

        this.publicKey = fetchPublicKey(region, bucket, keyFile);
        
        this.keyUpdateTimer = setInterval(() => this.getPublicKey(), this.keyCacheTime);
    }

    stop(): void {
        if(this.keyUpdateTimer) {
            clearInterval(this.keyUpdateTimer);
        }
    }

    getPublicKey(): Promise<string> {
        return this.publicKey.then(({ key, lastUpdated }) => {
            const now = new Date();
            const diff = now.getMilliseconds() - lastUpdated.getMilliseconds();

            if(diff > this.keyCacheTime) {
                this.publicKey = fetchPublicKey(this.region, this.bucket, this.keyFile);
                return this.publicKey.then(({ key }) => key);
            } else {
                return key;
            }
        });
    }

    verify(requestCookies: string): Promise<AuthenticationResult> {
        return this.getPublicKey().then(publicKey => {
            const cookies = cookie.parse(requestCookies);
            const pandaCookie = cookies[this.cookieName];

            const now = new Date().getMilliseconds();
            return verifyUser(pandaCookie, publicKey, now, this.validateUser);
        });
    }
}