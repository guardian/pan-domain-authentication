import { parseCookie, decodeBase64 } from '../src/utils';
import { sampleCookie } from './fixtures';
import { URLSearchParams } from 'url';

test("decode a cookie", () => {
    const { data, signature } = parseCookie(sampleCookie);
    expect(signature.length).toBe(684);

    const params = new URLSearchParams(data);
    
    expect(params.get("firstName")).toBe("Test");
    expect(params.get("lastName")).toBe("User");
});