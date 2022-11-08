import * as iniparser from 'iniparser';
import * as cookie from 'cookie';

import {base64ToPEM, httpGet, parseCookie, parseUser, sign, verifySignature} from './utils';
import {AuthenticationStatus, User, AuthenticationResult, ValidateUserFn} from './api';

function fetchPublicKey(region: string, bucket: String, keyFile: String): Promise<PublicKey> {
    const path = `https://s3.${region}.amazonaws.com/${bucket}/${keyFile}`;

    return httpGet(path).then(response => {
        const config: { publicKey?: string} = iniparser.parseString(response);

        if(config.publicKey) {
            return {
                key: base64ToPEM(config.publicKey, "PUBLIC"),
            };
        } else {
            throw new Error("Missing publicKey setting from config");
        }
    });
}

export function createCookie(user: User, privateKey: string): string {
    let queryParams: string[] = [];

    queryParams.push("firstName=" + user.firstName);
    queryParams.push("lastName=" + user.lastName);
    queryParams.push("email=" + user.email);
    user.avatarUrl && queryParams.push("avatarUrl=" + user.avatarUrl);
    queryParams.push("system=" + user.authenticatingSystem);
    queryParams.push("authedIn=" + user.authenticatedIn.join(","));
    queryParams.push("expires=" + user.expires.toString());
    queryParams.push("multifactor=" + String(user.multifactor));
    const combined = queryParams.join("&");

    const queryParamsString = Buffer.from(combined).toString('base64');

    const signature = sign(combined, privateKey);

    return queryParamsString + "." + signature
}

export function verifyUser(pandaCookie: string | undefined, publicKey: string, currentTimestamp: number, validateUser: ValidateUserFn): AuthenticationResult {
    if(!pandaCookie) {
        return { status: AuthenticationStatus.NOT_AUTHENTICATED };
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

export abstract class Refreshable<T> {
    value?: Promise<T>;
    updateTimer?: NodeJS.Timeout;

    cacheTime: number;

    constructor(cacheTime: number) {
        // this.refreshFn = () => refreshFn().then(res => ({ value: res, lastUpdated: new Date() }));
        this.cacheTime = cacheTime;

        this.updateTimer = setInterval(() => {
            this.value = this.refresh()
        }, this.cacheTime);
    }

    abstract refresh(): Promise<T>;

    get(): Promise<T> {
        if (this.value)
            return this.value;

        this.value = this.refresh();
        return this.value;
    }
}

type PublicKey = {
    key: string;
}
export class PanDomainAuthentication extends Refreshable<PublicKey> {
    cookieName: string;
    region: string;
    bucket: string;
    keyFile: string;
    validateUser: ValidateUserFn;

    static keyCacheTime: number = 60 * 1000; // 1 minute

    constructor(cookieName: string, region: string, bucket: string, keyFile: string, validateUser: ValidateUserFn) {
        super(PanDomainAuthentication.keyCacheTime)
        this.cookieName = cookieName;
        this.region = region;
        this.bucket = bucket;
        this.keyFile = keyFile;
        this.validateUser = validateUser;
    }

    override refresh(): Promise<PublicKey> {
        return fetchPublicKey(this.region, this.bucket, this.keyFile);
    };
    

    // TODO deprecate
    stop(): void {
        if(this.updateTimer) {
            clearInterval(this.updateTimer);
        }
    }

    getPublicKey(): Promise<string> {
        return this.get().then(held => held.key);
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
