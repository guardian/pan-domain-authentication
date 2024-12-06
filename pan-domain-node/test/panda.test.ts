import {guardianValidation, AuthenticationStatus, User} from '../src/api';
import { verifyUser, createCookie, PanDomainAuthentication } from '../src/panda';
import { fetchPublicKey } from '../src/fetch-public-key';

import {
    sampleCookie,
    sampleCookieWithoutMultifactor,
    sampleNonGuardianCookie,
    publicKey,
    privateKey
} from './fixtures';
import {decodeBase64} from "../src/utils";

jest.mock('../src/fetch-public-key');
jest.useFakeTimers('modern');

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

describe('panda class', function () {
  beforeEach(() => {
    (fetchPublicKey as jest.MockedFunction<typeof fetchPublicKey>).mockResolvedValue({ key: 'PUBLIC KEY', lastUpdated: new Date() });
  });

  describe('stop', () => {

    it('stops auto refresh', () => {
      const panda = new PanDomainAuthentication('cookiename', 'region', 'bucket', 'keyfile', (u)=> true);
      expect(panda.keyUpdateTimer).not.toBeUndefined();
      panda.stop();
      expect(panda.keyUpdateTimer).toBeUndefined();
    });

  });

  describe('getPublicKey', () => {

    it('getsPublicKey immediately when last fetch is within the cache time', async () => {

      const panda = new PanDomainAuthentication('cookiename', 'region', 'bucket', 'keyfile', (u)=> true);
      const fetchesBeforeGet = (fetchPublicKey as jest.MockedFunction<typeof fetchPublicKey>).mock.calls.length;

      await expect(panda.getPublicKey()).resolves.toEqual('PUBLIC KEY');
      const fetchesAfterGet = (fetchPublicKey as jest.MockedFunction<typeof fetchPublicKey>).mock.calls.length;

      expect(fetchesAfterGet).toEqual(fetchesBeforeGet);

    });

    it('getsPublicKey after refetching when last fetch is outside the cache time', async () => {
      // cache time is 1 min
      const fiveMinsAgo = new Date();
      fiveMinsAgo.setMinutes(fiveMinsAgo.getMinutes() - 5);

      (fetchPublicKey as jest.MockedFunction<typeof fetchPublicKey>).mockResolvedValue({ key: 'PUBLIC KEY', lastUpdated: fiveMinsAgo });

      const panda = new PanDomainAuthentication('cookiename', 'region', 'bucket', 'keyfile', (u)=> true);

      const fetchesBefore = (fetchPublicKey as jest.MockedFunction<typeof fetchPublicKey>).mock.calls.length;

      await expect(panda.getPublicKey()).resolves.toEqual('PUBLIC KEY');

      (fetchPublicKey as jest.MockedFunction<typeof fetchPublicKey>).mockResolvedValue({ key: 'PUBLIC KEY 2', lastUpdated: fiveMinsAgo });

      const fetchesAfter = (fetchPublicKey as jest.MockedFunction<typeof fetchPublicKey>).mock.calls.length;

      await expect(panda.getPublicKey()).resolves.toEqual('PUBLIC KEY 2');

      expect(fetchesAfter).toEqual(fetchesBefore + 1);
    });

  });

  describe('verify', () => {

    beforeEach(() => {
      (fetchPublicKey as jest.MockedFunction<typeof fetchPublicKey>).mockResolvedValue({ key: publicKey, lastUpdated: new Date() });
    });

    it('should return authenticated if valid', async () => {
      jest.setSystemTime(100);
      const panda = new PanDomainAuthentication('cookiename', 'region', 'bucket', 'keyfile', (u)=> true);
      const { status } = await panda.verify(`cookiename=${sampleCookie}`);

      expect(status).toBe(AuthenticationStatus.AUTHORISED);
    });

    it('should return expired if expired', async () => {
      jest.setSystemTime(10_000);

      const panda = new PanDomainAuthentication('cookiename', 'region', 'bucket', 'keyfile', (u)=> true);
      const { status } = await panda.verify(`cookiename=${sampleCookie}`);

      expect(status).toBe(AuthenticationStatus.EXPIRED);
    });

    it('should return not authenticated if validation fails', async () => {
      jest.setSystemTime(100);

      const panda = new PanDomainAuthentication('cookiename', 'region', 'bucket', 'keyfile', guardianValidation);
      const { status } = await panda.verify(`cookiename=${sampleNonGuardianCookie}`);

      expect(status).toBe(AuthenticationStatus.NOT_AUTHORISED);
    });

  });

});
