import * as crypto from 'crypto';
import * as https from 'https';

import {User} from './api';
import {URLSearchParams} from 'url';

export function decodeBase64(data: string): string {
    return Buffer.from(data, 'base64').toString('utf8');
}

/**
 * Parse a pan-domain user cookie in to data and signature
 */
export function parseCookie(cookie: string): { data: string, signature: string} {
    const splitCookie = cookie.split('\.');
    return {
        data: decodeBase64(splitCookie[0]),
        signature: splitCookie[1]
    };
}

/**
 * Verify signed data using nodeJs crypto library
 */
export function verifySignature(message: string, signature: string, pandaPublicKey: string): boolean {
    return crypto.createVerify('sha256WithRSAEncryption')
        .update(message, 'utf8')
        .verify(pandaPublicKey, signature, 'base64');
}

export function sign(message: string, privateKey: string): string {
    const sign = crypto.createSign("sha256WithRSAEncryption");
    sign.write(message);
    sign.end();

    return sign.sign(privateKey, 'base64');
}

const ASCII_NEW_LINE = String.fromCharCode(10);

export function base64ToPEM (key: string, headerFooter: string): string {
    const PEM_HEADER = `-----BEGIN ${headerFooter} KEY-----`;
    const PEM_FOOTER = `-----END ${headerFooter} KEY-----`;

	let tmp = [];
    const ret = [Buffer.from(PEM_HEADER).toString('ascii')];

    for (let i = 0, len = key.length; i < len; i++) {

        if (i>0 && i%64 === 0) {
            ret.push(tmp.join(''));
            tmp = [];
        }

        tmp.push(key[i]);
    }

    ret.push(tmp.join(''));
    ret.push(Buffer.from(PEM_FOOTER).toString('ascii'));

    return ret.join(ASCII_NEW_LINE);
}

export function httpGet(path: string): Promise<string> {
    return new Promise((resolve, reject) => {
        const data: string[] = [];

        https.get(path, res => {
            res.on('data', chunk => data.push(chunk.toString('utf8')));
            res.on('error', err => reject(err));

            res.on('end', () => {
                const body = data.join('');

                if(res.statusCode == 200) {
                    resolve(body);
                } else {
                    // Response might be XML
                    const match = body.match(/<message>(.*)<\/message>/i);
                    const error = new Error(match ? match[1] : 'Invalid public key response');

                    reject(error);
                }
            });
        });
    });
}

export function parseUser(data: string): User {
    const params = new URLSearchParams(data);

    function stringField(name: string): string {
        const value = params.get(name);
        if(!value ) { throw new Error(`Missing ${name}`) }

        return value;
    }

    function numberField(name: string): number {
        const value = params.get(name);
        if(!value) { throw new Error(`Missing ${name}`) }

        return parseInt(value);
    }

    function booleanField(name: string): boolean {
        return params.get(name) === 'true';
    }

    function stringListField(name: string): string[] {
        const value = params.get(name);
        if(!value) { throw new Error(`Missing ${name}`) }

        return value.split(",");
    }

    const avatarUrl = params.get("avatarUrl");

    return {
        firstName: stringField("firstName"),
        lastName: stringField("lastName"),
        email: stringField("email"),
        avatarUrl: avatarUrl ? avatarUrl : undefined,
        authenticatingSystem: stringField("system"),
        authenticatedIn: stringListField("authedIn"),
        expires: numberField("expires"),
        multifactor: booleanField("multifactor")
    };
}
