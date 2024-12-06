import * as iniparser from 'iniparser';
import {base64ToPEM, httpGet} from './utils';

export interface PublicKeyHolder {
    key: string,
    lastUpdated: Date
}


export function fetchPublicKey(region: string, bucket: String, keyFile: String): Promise<PublicKeyHolder> {
    const path = `https://s3.${region}.amazonaws.com/${bucket}/${keyFile}`;

    return httpGet(path).then(response => {
        const config: { publicKey?: string} = iniparser.parseString(response);

        if(config.publicKey) {
            return {
                key: base64ToPEM(config.publicKey, "PUBLIC"),
                lastUpdated: new Date()
            };
        } else {
            throw new Error("Missing publicKey setting from config");
        }
    });
}


