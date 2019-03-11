export { PanDomainAuthentication } from './panda';

export enum AuthenticationStatus {
    INVALID_COOKIE = 'Invalid Cookie',
    EXPIRED = 'Expired',
    NOT_AUTHORISED = 'Not Authorised',
    AUTHORISED = 'Authorised'
}

export interface User {
    firstName: string,
    lastName: string,
    email: string,
    avatarUrl?: string,
    authenticatingSystem: string,
    authenticatedIn: string[],
    expires: number,
    multifactor: boolean
}

export interface AuthenticationResult {
    status: AuthenticationStatus,
    user?: User 
}

export type ValidateUserFn = (user: User) => boolean;

export function guardianValidation(user: User): boolean {
    const isGuardianUser = user.email.indexOf('guardian.co.uk') !== -1;
    return isGuardianUser && user.multifactor;
}