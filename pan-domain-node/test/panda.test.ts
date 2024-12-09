import {guardianValidation, AuthenticationStatus, User} from '../src/api';
import { verifyUser, createCookie } from '../src/panda';

import {
    sampleCookie,
    sampleCookieWithoutMultifactor,
    sampleNonGuardianCookie,
    publicKey,
    privateKey
} from './fixtures';
import {decodeBase64} from "../src/utils";

describe('verifyUser', function () {

    test("return invalid cookie if missing", () => {
        expect(verifyUser(undefined, "", new Date(0), guardianValidation).status).toBe(AuthenticationStatus.INVALID_COOKIE);
    });

    test("return invalid cookie for a malformed signature", () => {
        const [data, signature] = sampleCookie.split(".");
        const testCookie = data + ".1234";

        expect(verifyUser(testCookie, publicKey, new Date(0), guardianValidation).status).toBe(AuthenticationStatus.INVALID_COOKIE);
    });

    test("return expired", () => {
        const someTimeInTheFuture = new Date(5678);
        expect(someTimeInTheFuture.getTime()).toBe(5678);
        expect(verifyUser(sampleCookie, publicKey, someTimeInTheFuture, guardianValidation).status).toBe(AuthenticationStatus.EXPIRED);
    });

    test("return not authenticated if user fails validation function", () => {
        expect(verifyUser(sampleCookieWithoutMultifactor, publicKey, new Date(0), guardianValidation).status).toBe(AuthenticationStatus.NOT_AUTHORISED);
        expect(verifyUser(sampleNonGuardianCookie, publicKey, new Date(0), guardianValidation).status).toBe(AuthenticationStatus.NOT_AUTHORISED);
    });

    test("return authenticated", () => {
        expect(verifyUser(sampleCookie, publicKey, new Date(0), guardianValidation).status).toBe(AuthenticationStatus.AUTHORISED);
    });
});

describe('createCookie', function () {
    it('should return the same cookie based on the user details being provided', function () {
        const user: User = {
            firstName: "Test",
            lastName: "User",
            email: "test.user@guardian.co.uk",
            authenticatingSystem: "test",
            authenticatedIn: ["test"],
            expires: 1234,
            multifactor: true
        };

        const cookie = createCookie(user, privateKey);

        expect(decodeBase64(cookie)).toEqual(decodeBase64(sampleCookie));
        expect(cookie).toEqual(sampleCookie)
    });
});
