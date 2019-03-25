import { guardianValidation, AuthenticationStatus } from '../src/api';
import { verifyUser } from '../src/panda';

import { sampleCookie, sampleCookieWithoutMultifactor, sampleNonGuardianCookie, publicKey } from './fixtures';

test("return invalid cookie if missing", () => {
    expect(verifyUser(undefined, "", 0, guardianValidation).status).toBe(AuthenticationStatus.INVALID_COOKIE);
});

test("return invalid cookie for a malformed signature", () => {
    const [data, signature] = sampleCookie.split(".");
    const testCookie = data + ".1234";

    expect(verifyUser(testCookie, publicKey, 0, guardianValidation).status).toBe(AuthenticationStatus.INVALID_COOKIE);
});

test("return expired", () => {
    const someTimeInTheFuture = 5678;
    expect(verifyUser(sampleCookie, publicKey, someTimeInTheFuture, guardianValidation).status).toBe(AuthenticationStatus.EXPIRED);
});

test("return not authenticated if user fails validation function", () => {
    expect(verifyUser(sampleCookieWithoutMultifactor, publicKey, 0, guardianValidation).status).toBe(AuthenticationStatus.NOT_AUTHORISED);
    expect(verifyUser(sampleNonGuardianCookie, publicKey, 0, guardianValidation).status).toBe(AuthenticationStatus.NOT_AUTHORISED);
});

test("return authenticated", () => {
    expect(verifyUser(sampleCookie, publicKey, 0, guardianValidation).status).toBe(AuthenticationStatus.AUTHORISED);
});