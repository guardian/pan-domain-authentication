import qs from 'qs';
import { parseCookie, decodeBase64 } from '../src/utils';
import { sampleCookie } from './fixtures';

test("decode a cookie", () => {
    const { data, signature } = parseCookie(sampleCookie);
    expect(signature.length).toBe(684);

    const params = qs.parse(data);
    
    expect(params["firstName"]).toBe("Test");
    expect(params["lastName"]).toBe("User");
});