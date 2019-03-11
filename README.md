# Pan Domain Authentication 

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gu/pan-domain-auth-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.gu/pan-domain-auth-core_2.12)

Pan domain authentication provides distributed authentication for multiple webapps running in the same domain. Each
application can authenticate users against an OAuth provider and store the authentication information in a common cookie.
Each application can read this cookie and check if the user is allowed in the specific application and allow access accordingly.

This means that users are only prompted to provide authentication credentials once across the domain and any inter-app
interactions (e.g javascript cross-origin requests) can be easily secured.

## How it works

The library can be used in two ways:

 - **Verify**: read the Panda cookie and check whether the user is valid for the request
 - **Issue**: as above but redirecting the user to the OAuth provider to authenticate if the cookie is not present or expired

Simply verifying the cookie is useful for APIs that cannot provided a user-facing OAuth dance to acquire credentials. It is also
useful to minimise the parts of your application that have to have knowledge of the private key.

To ensure the cookie is not tampered with, public/private key pair encryption is use. An issuing application signs the cookie
using the private key and both verifying and issuing applications verify using the public key.

The cookie contains an expiry time generated at issue after which the user should be redirected to the OAuth provided again.

All OAuth 2.0 compliant providers are supported. There is additional support for verifying that the user has two-factor login
enabled when using Google as the provider.

Each authenticated request that an application receives should be checked to see if there is a auth cookie.

* If the cookie is not present then the user should be sent to the OAuth provider for authentication. Upon their return the user
information should be checked and if the user is allowed in the app then the shared cookie should be set marking the user as valid
in the application.

* If there is a cookie but the cookie does not indicate that the the user is valid then the user should be validated for the application.
This is an application-specific concern such as verifying the email address or checking two-factor is enabled. If they are valid then the
cookie should be updated to indicate this or an error page displayed.

* If there is a cookie and it indicates the user is valid in this application then the request should be processed as normal.

* if there is a cookie but it indicates the the authentication is expired then the user should be sent off to the provider to renew their session.
On their return the existing cookie is updated with the new expiry time.

## What's provided

Pan domain auth is split into 5 modules.

The [pan-domain-auth-verification](###-to-verify-logins) library provides the basic functionality for sigining and verifying login cookies in Scala.
For JVM applications that only need to *VERIFY* an existing login (rather than issue logins themselves) this is the library to use.

The `pan-domain-auth-core` library provides the core utilities to load settings, create and validate the cookie and
check if the user has mutli-factor auth turned on when usng Google as the provider.

The [pan-domain-auth-play_2-6](###if-your-application-needs-to-issue-logins) library provide an implementation for play apps. There is an auth action
that can be applied to the endpoints in your application that will do checking and setting of the cookie and will give you the OAuth authentication
mechanism and callback. This is the only framework specific implementation currently (due to play being the framework predominantly used at The
Guardian), this can be used as reference if you need to implement another framework implementation. This library is for applications
that need to be able to issue and verify logins which is likely to include user-facing applications.

The [pan-domain-node](###to-verify-login-in-nodejs) library provides an implementation of *verification only* for node apps.

The `pan-domain-auth-example` provides an example Play 2.6 app with authentication. Additionally the nginx directory provides an example
of how to set up an nginx configuration to allow you to run multiple authenticated apps locally as if they were all on the same domain which
is useful during development.

## Requirements

If you are adding a new application to an existing deployment of pan-domain-authentication then you can skip to
[Integrating With Your App](#integrating-with-your-app)

* At least one webapp running on subdomains of a single domain (eg. app1.example.com and app2.example.com)

* The apps must be using https - the cookie set by pan domain auth are set to secure and http only

* An AWS S3 bucket where the configuration for your domain will live

* Configuration files in the S3 bucket. A good naming convention to follow is `<domain>.settings` and `<domain>.settings.public`.

* An OAuth provider to use for authentication

  * Both a [Client ID and secret](https://www.oauth.com/oauth2-servers/client-registration/client-id-secret/) are required
  * An OAuth [discovery document](https://tools.ietf.org/html/draft-ietf-oauth-discovery-06) is required
    * Example Google: `https://accounts.google.com/.well-known/openid-configuration`
    * Example AWS Cognito: `https://cognito-idp.eu-west-1.amazonaws.com/eu-west-1_nW3FKqRh0/.well-known/openid-configuration`
  * You must also configure the callback URLs with your provider, one for each application under the domain that will be issuing logins.

* Optionally a Google Service Account to check multi-factor has been enabled when using Google as a provider

  * This can be configured from the [Google Developer Console](https://console.developers.google.com)
  * Ensure that you have switched on access to the `Google+ API` for your credentials


## Setting up your domain configuration

The configuration file is named for the domain and is a simple properties style file. A good naming convention to follow
is for apps on the `*.example.com` domain to name the file `example.com.settings`. The contents of the file would look something like this:

``` ini
publicKey=example_key
privateKey=example_key
cookieName=exampleAuth

clientId=example_oauth_client_id
clientSecret=example_oauth_secret

googleServiceAccountId=serviceAccount@developer.gserviceaccount.com
googleServiceAccountCert=name_of_cert_in_bucket.p12
google2faUser=an.admin@example.com
multifactorGroupId=group@2fa_admin_user
```
  
There is a corresponding (publically available) file called example.com.settings.public. 
The contents of the file looks like:
 
``` ini
publicKey=example_key
```

* **secret** - this is the shared secret used to sign the cookie

* **cookieName** - the name of the cookie. This should be unique to each top-level domain.

* **clientId** - this is the OAuth client id for the provider you are authenticating against

* **googleAuthSecret** - this is the OAuth secret for the provider

* **googleServiceAccountId, googleServiceAccountCert, google2faUser and multifactorGroupId** - these are optional parameters for using a group based 2 factor auth verification

* **privateKey** - this is the private key used to sign the cookie

* **publicKey** - this is the public key used to verify the cookie

### Generating Keys

You can generate an rsa key pair as follows:

    openssl genrsa -out private_key.pem 4096
    openssl rsa -pubout -in private_key.pem -out public_key.pem

There is a helper script in the root of this project that uses the commands above and outputs a new keypair in the format used by the panda settings file:

    ./generateKeyPair.sh

Note: you only need to pass the key ie the blob of base64 between the start and end markers in the pem file.

## Integrating with your Scala app

### To verify logins

Add the verification library as an SBT dependency to your project, taking care to use the latest version:

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gu/pan-domain-auth-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.gu/pan-domain-auth-core_2.12)

```
libraryDependencies += "com.gu" %% "pan-domain-auth-verification" % "<<LATEST_VERSION>>"
```

Follow the example code provided [here](pan-domain-auth-example/app/VerifyExample.scala)

Configuration settings are read from an S3 bucket. Follow the steps below if you do not yet have configuration set up.

### If your application needs to issue logins

Add the core library as an SBT dependency to your project, taking care to use the latest version.
If you are building your application using the Play framework, use the Play integration.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gu/pan-domain-auth-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.gu/pan-domain-auth-core_2.12)

```
libraryDependencies += "com.gu" %% "pan-domain-auth-core" % "<<LATEST_VERSION>"
```

or

```
libraryDependencies += "com.gu" %% "pan-domain-auth-play_2-6" % "<<LATEST_VERSION>"
```

Example code for the Play Framework is provided [here](./pan-domain-auth-example). In particular:

- [di.scala](./pan-domain-auth-example/di.scala) shows how to construct the settings refresher
- [ExampleAuthActions](./pan-domain-auth-example/controllers/ExampleAuthActions.scala) shows how to implement user validation
- [AdminController](./pan-domain-auth-example/controllers/AdminController.scala) shows how to built authenticated request handlers

Ensure you pick the correct request handler for your needs:

* `AuthAction` is used for endpoints that the user requests and will redirect unauthenticated users to the OAuth provider for authentication.
  Use this for standard page loads etc.

* `ApiAuthAction` is used for api ajax / xhr style requests and will not redirect to the OAuth provider. This action will either process
  the action or return an error code that can be processed by your client javascript (see section on handling expired logins in a single
  page webapp).

  A grace period on expiry can be set by adding a `apiGracePeriod`. This is useful for when browsers have third party cookies disabled
  and reauthenticaiton solutions like [pandular](https://github.com/guardian/pandular) break due to cookies being blocked on `window.open`
  or `iframe` requests. During this period we are hopeful of the user refreshing or revisiting the application through a standard browser
  request thus triggering off a reauthentication.

  The response codes are:

    * **401** - user not authenticated - probably tricky to get this response as presumably the user has already loaded a page that would have
            logged them in

    * **403** - not authorised - occurs then the user is authenticated but not valid in this app, this can happen when making cross app CORS
            requests

    * **419** - authorisation expired - occurs when the authorisation with the OAuth provider has expired (eg 1 hour for Google).
                You will need to re auth with the provider. This can typically be done transparently on the next page load request.

  See also [Customising error responses for an authenticated API]().

Example Scala code is not yet provided for web frameworks other than Play.


### Customising error responses for an authenticated API

The default `ApiAuthAction` error responses returns sensible status codes but no body.

To customise the responses (code and body) of an authenticated API,
you can provide your own implementation of the `AbstractApiAuthAction`
trait that provides the various abstract result properties:

``` scala
object VerboseAPIAuthAction extends AbstractApiAuthAction {
  val notAuthenticatedResult: Result = Unauthorized(errorResponse("Not authenticated"))
  val invalidCookieResult: Result    = notAuthenticatedResult
  val expiredResult: Result          = Forbidden(errorResponse("Session expired"))
  val notAuthorizedResult: Result    = Forbidden(errorResponse("Not authorized"))

  private def errorResponse(msg: String) = Json.obj("error" -> msg)
}
```


### Using Google group based 2-factor authentication validation

Some applications may require that a multifactor authentication is used when authenticating a user. Since it is not possible to tell if this happened
from the standard callback this is checked by asserting that the user is in a Google group that enforces 2 factor auth (this was the workaround suggested
by Google themselves when we asked about checking 2fa). Since the group is likely set up within an apps for domains setup and not accessible to everyone
checking the 2fa group uses different Google credentials from the main auth.

To configure multifactor checking you will need to create a service account that can access the Google directory api,
see [directory API docs](https://developers.google.com/admin-sdk/directory/v1/guides/delegation). once this is configured fill in all the following properties
in the domain's property file and upload the service accounts cert to the s3bucket. If you do not wish to use this feature just omit the
properties:

* **googleServiceAccountId** - the service account email that is set up to allow access to the directory API
* **googleServiceAccountCert** - the name within the bucket of the certificate used to validate the service account
* **google2faUser** - the admin user to connect to the directory api as, this is not the service account user but a user in your org who is authorised to access group information
* **multifactorGroupId** - the name of the group that indicates and enforces that 2fa is turned on


### To verify login in NodeJS

[![npm version](https://badge.fury.io/js/pan-domain-node.svg)](https://badge.fury.io/js/pan-domain-node)

```
npm install --save-dev pan-domain-node
```

```typescript
import { PanDomainAuthentication, AuthenticationStatus, User, guardianValidation } from 'pan-domain-node';

const panda = new PanDomainAuthentication(
  "gutoolsAuth-assym", // cookie name
  "eu-west-1", // AWS region
  "pan-domain-auth-settings", // Settings bucket
  "local.dev-gutools.co.uk.public.settings", // Settings file
  guardianValidation // customisable user validation function
);

// alternatively customise the validation function and pass at construction
function customValidation(user: User): boolean {
  const isInCorrectDomain = user.email.indexOf('test.com') !== -1;
  return isInCorrectDomain && user.multifactor;
}

// when handling a request
function(request) {
  // Pass the raw unparsed cookies
  return panda.verify(request.headers['Cookie']).then(( { status, user }) => {
    switch(status) {
      case AuthenticationStatus.Authorised:
        // Good user, handle the request!

      default:
        // Bad user. Return 4XX
    }
  });
}

```


### Dealing with auth expiry in a single page webapp

In a single page webapp there will typically be an initial page load and then all communication with the server will be initiated by JavaScript.
This causes problems when the auth session expires as you can't redirect the request to the OAuth provider. To work around this
all ajax type requests should return 419 responses on auth session expiry and this should be handled by the JavaScript layer.

See also the helper [panda-session](https://github.com/guardian/panda-session) JavaScript library.
