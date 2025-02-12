# Pan Domain Authentication 

[![pan-domain-auth-core Scala version support](https://index.scala-lang.org/guardian/pan-domain-authentication/pan-domain-auth-core/latest-by-scala-version.svg?platform=jvm)](https://index.scala-lang.org/guardian/pan-domain-authentication/pan-domain-auth-core)

* This repo - General docs & Scala implementation
* [pan-domain-node](https://github.com/guardian/pan-domain-node) - Typescript implementation

Pan domain authentication provides distributed authentication for multiple webapps running in the same domain. Each
application can authenticate users against an OAuth provider and store the authentication information in a common cookie.
Each application can read this cookie and check if the user is allowed in the specific application and allow access accordingly.

This means that users are only prompted to provide authentication credentials once across the domain and any inter-app
interactions (e.g javascript cross-origin requests) can be easily secured.

## How it works

The library can be used in two ways:

 - **Verify**: read the Panda cookie and check whether the user is valid for the request
 - **Issue**: as above but redirecting the user to the OAuth provider to authenticate if the cookie is not present or expired

Simply verifying the cookie is useful for APIs that cannot provide a user-facing OAuth dance to acquire credentials. It is also
useful to minimise the parts of your application that have to have knowledge of the private key.

To ensure the cookie is not tampered with, public/private key pair encryption is used. An issuing application signs the cookie
using the private key and both verifying and issuing applications verify using the public key.

The cookie contains an expiry time generated at issue after which the user should be redirected to the OAuth provided again.

All OAuth 2.0 compliant providers are supported. There is additional support for verifying that the user has two-factor login
enabled when using Google as the provider.

Each authenticated request that an application receives should be checked to see if there is a auth cookie.

* If the cookie is not present then the user should be sent to the OAuth provider for authentication. Upon their return the user
information should be checked and if the user is allowed in the app then the shared cookie should be set marking the user as valid
in the application.

* If there is a cookie but the cookie does not indicate that the user is valid then the user should be validated for the application.
This is an application-specific concern such as verifying the email address or checking two-factor is enabled. If they are valid then the
cookie should be updated to indicate this or an error page displayed.

* If there is a cookie and it indicates the user is valid in this application then the request should be processed as normal.

* if there is a cookie but it indicates the the authentication is expired then the user should be sent off to the provider to renew their session.
On their return the existing cookie is updated with the new expiry time.

## What's provided

Pan domain auth is split into 6 modules.

The [pan-domain-auth-verification](#to-verify-logins) library provides the basic functionality for signing and verifying login cookies in Scala.
For JVM applications that only need to *VERIFY* an existing login (rather than issue logins themselves) this is the library to use.

The `pan-domain-auth-core` library provides the core utilities to load settings, create and validate the cookie and
check if the user has multi-factor auth turned on when using Google as the provider.

The [pan-domain-auth-play_2-8, 2-9 and 3-0](#if-your-application-needs-to-issue-logins) libraries provide an implementation for play apps. There is an auth action
that can be applied to the endpoints in your application that will do checking and setting of the cookie and will give you the OAuth authentication
mechanism and callback. This is the only framework specific implementation currently (due to play being the framework predominantly used at The
Guardian), this can be used as reference if you need to implement another framework implementation. This library is for applications
that need to be able to issue and verify logins which is likely to include user-facing applications.

The [pan-domain-node](https://github.com/guardian/pan-domain-node) library provides an implementation of *verification only* for node apps.

The `pan-domain-auth-example` provides an example Play 2.9 app with authentication. Additionally the nginx directory provides an example
of how to set up an nginx configuration to allow you to run multiple authenticated apps locally as if they were all on the same domain which
is useful during development.

The [panda-hmac](#to-verify-machines) libraries build on pan-domain-auth-play to also verify machine clients,
who cannot perform OAuth authentication, by using HMAC-SHA-256.

## Requirements

If you are adding a new application to an existing deployment of pan-domain-authentication then you can skip to
[Integrating With Your App](#integrating-with-your-app)

* At least one webapp running on subdomains of a single domain (e.g. app1.example.com and app2.example.com)

* The apps must be using https - the cookie set by pan domain auth are set to secure and http only

* An OAuth provider to use for authentication

## Setting up your domain configuration

Panda is compatible with all OAuth2 providers, automatically discovering endpoints using a
[discovery document](https://tools.ietf.org/html/draft-ietf-oauth-discovery-06).

### AWS Cognito

Follow these steps to create a new [Cognito User Pool](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-identity-pools.html).
This allows you to manage your users entirely within AWS.

You will need:

- The AWS CLI and credentials for your AWS account
- An OAuth callback URL for each application that will be issuing logins

Run the following commands:

- Deploy the [CloudFormation Template](./cognito/cognito.yaml)
- Generate settings and public/private keys
  - `./cognito/generate-settings.sh ${CLOUDFORMATION_STACK} ${REGION}` 
  - This will also upload them to the configuration bucket
- Add users
  - `./cognito/add-user.sh ${CLOUDFORMATION_STACK} ${USER_EMAIL} ${REGION}`
  - They will receive an email invite with a temporary password

### Generic OAuth2 Provider 

You will need:

* An AWS S3 bucket where the configuration for your domain will live
* Both a [Client ID and secret](https://www.oauth.com/oauth2-servers/client-registration/client-id-secret/) are required
  * An OAuth [discovery document](https://tools.ietf.org/html/draft-ietf-oauth-discovery-06) is required
    * Example Google: `https://accounts.google.com/.well-known/openid-configuration`
    * Example AWS Cognito: `https://cognito-idp.eu-west-1.amazonaws.com/eu-west-1_nW3FKqRh0/.well-known/openid-configuration`
  * You must also configure the callback URLs with your provider, one for each application under the domain that will be issuing logins.

The configuration file is named for the domain and is a simple properties style file. A good naming convention to follow
is for apps on the `*.example.com` domain to name the file `example.com.settings`. The contents of the file would look something like this:

``` ini
publicKey=example_key
privateKey=example_key
cookieName=exampleAuth

clientId=example_oauth_client_id
clientSecret=example_oauth_secret
organizationDomain=example.com

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

* **cookieName** - the name of the cookie. This should be unique to each top-level domain.

* **clientId** - this is the OAuth client id for the provider you are authenticating against

* **clientSecret** - this is the OAuth secret for the provider

* **organizationDomain** - OPTIONAL: this is the domain where users are registered, for services which support it. If user's emails are in the format `user@example.com`, then this should be set to `example.com`.
  * Known services with support: 
    * Google

* **googleServiceAccountId, googleServiceAccountCert, google2faUser and multifactorGroupId** - these are optional parameters for using a group based 2 factor auth verification

* **privateKey** - this is the private key used to sign the cookie

* **publicKey** - this is the public key used to verify the cookie

### Rotating Keys

**Guardian Devs**: See the [Panda key-rotation Guide](https://docs.google.com/document/d/1haVnQ9D8zNYUU-fOfkudPC1WpPGrlelLygd8V7xb3eQ/edit?usp=sharing)
for Guardian-specific details of where config details are stored, etc.

To avoid disruption to users, rotating keys requires 3 distinct settings updates, with pauses between each one. First
obtain a copy of the current settings file (eg `current-from-s3.settings`), then use the sbt console to run
the `CryptoConfForRotation` Scala script on that `.settings` file to generate a new RSA 4096 keypair and the new
required config files for each step:

```
pan-domain-auth-verification / Test / runMain com.gu.pandomainauth.CryptoConfForRotation current-from-s3.settings
```

3 new partial `.settings` files will be created, providing _just_ the updated crypto settings - you'll need to
edit them into the existing `current-from-s3.settings` & `current-from-s3.settings.public` files before uploading
those updates:

* 1.rotation-upcoming.settings - give this 2 minutes of settling time 
* 2.rotation-in-progress.settings - give this at least 1 hour of settling time
* 3.rotation-complete.settings

## Integrating with your Scala app

### To verify logins

Add the verification library as an SBT dependency to your project, taking care to use the latest version:

[![pan-domain-auth-verification Scala version support](https://index.scala-lang.org/guardian/pan-domain-authentication/pan-domain-auth-verification/latest-by-scala-version.svg?platform=jvm)](https://index.scala-lang.org/guardian/pan-domain-authentication/pan-domain-auth-verification)

```scala
libraryDependencies += "com.gu" %% "pan-domain-auth-verification" % "<<LATEST_VERSION>>"
```

Follow the example code provided [here](pan-domain-auth-example/app/VerifyExample.scala)

Configuration settings are read from an S3 bucket. Follow the steps below if you do not yet have configuration set up.

### If your application needs to issue logins

Add the core library as an SBT dependency to your project, taking care to use the latest version.
If you are building your application using the Play framework, use the Play integration.
Play versions 2.9 and 3.0 are supported only on Scala 2.13.
Play version 2.8 is supported on Scala 2.12 and 2.13.
Play version 2.7 is supported up until v1.3.0.
Play version 2.6 is supported up until v0.9.2.

[![pan-domain-auth-core Scala version support](https://index.scala-lang.org/guardian/pan-domain-authentication/pan-domain-auth-core/latest-by-scala-version.svg?platform=jvm)](https://index.scala-lang.org/guardian/pan-domain-authentication/pan-domain-auth-core)

```scala
libraryDependencies += "com.gu" %% "pan-domain-auth-core" % "<<LATEST_VERSION>"
```

or

```scala
// pick the version corresponding to your app's version of Play
libraryDependencies += "com.gu" %% "pan-domain-auth-play_2-8" % "<<LATEST_VERSION>"
libraryDependencies += "com.gu" %% "pan-domain-auth-play_2-9" % "<<LATEST_VERSION>"
libraryDependencies += "com.gu" %% "pan-domain-auth-play_3-0" % "<<LATEST_VERSION>"
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

### To verify machines

Add a dependency on the correct version of `pan-domain-auth-play` and configure to allow authentication of users using OAuth 2. Then, adding support should be as simple as adding a dependency on the relevant panda-hmac-play library, and mixing `HMACAuthActions` into your controllers.

Example:

```scala
import com.gu.pandahmac.HMACAuthActions

// ...

@Singleton
class MyController @Inject() (
    override val config: Configuration,
    override val controllerComponents: ControllerComponents,
    override val wsClient: WSClient,
    override val refresher: InjectableRefresher
) extends AbstractController(controllerComponents)
    with PanDomainAuthActions
    with HMACAuthActions {

  override def secretKeys = List("currentSecret") // You're likely to get your secret from configuration or a cloud service like AWS Secrets Manager

  def myApiActionWithBody = APIHMACAuthAction.async(circe.json(2048)) { request => 
    // ... do something with the request
  }

  def myRegularAction = HMACAuthAction {}

  def myRegularAsyncAction = HMACAuthAction.async {}
}
```

#### Setting up a machine client

There are example clients for Scala, Javascript and Python in the `hmac-examples/` directory.

Each client needs a copy of the shared secret, defined as "currentSecret" in the controller example above.
Each request needs a standard (RFC-7231) HTTP Date header, and an authorization digest that is calculated like this:

1. Make a "string to sign" consisting of the HTTP Date and the Path part of the URI you're trying to access, 
seperated by a literal newline (unix-style, not CRLF)
2. Calculate the HMAC digest of the "string to sign" using the shared secret as a key and the HMAC-SHA-256 algorithm
3. Base64 encode the binary output of the HMAC digest to get a random-looking string
4. Add the HTTP date to the request headers with the header name **'X-Gu-Tools-HMAC-Date'**
5. Add another header called **'X-Gu-Tools-HMAC-Token'** and set its value to the literal string **HMAC** followed by a
 space and the digest, like this: `X-Gu-Tools-HMAC-Token: HMAC boXSTNumKWRX3eQk/BBeHYk`
6. Send the request and the server should respond with a success.
7. The default allowable clock skew is 5 minutes, if you have problems then this is the first thing to check.

#### Testing HMAC-authenticated endpoints in isolation

[Postman](https://www.postman.com/) is a common environment for testing HTTP requests. We can add a [pre-request script](https://learning.postman.com/docs/writing-scripts/pre-request-scripts/) that automatically adds HMAC headers when we hit send.

<details>
<summary>Pre-request script</summary>
  
```js
const URL = require("url");

const uri = pm.request.url.toString();
const secret = "Secret goes here :)";

const httpDate = new Date().toUTCString();
const path = new URL.parse(uri).path;
const stringToSign = `${httpDate}\n${path}`;
const stringToSignBytes = CryptoJS.enc.Utf8.parse(stringToSign);
const secretBytes = CryptoJS.enc.Utf8.parse(secret);

const signature = CryptoJS.enc.Base64.stringify(CryptoJS.HmacSHA256(stringToSignBytes, secretBytes));
const authToken = `HMAC ${signature}`;

pm.request.headers.add({ key: 'X-Gu-Tools-HMAC-Date', value: httpDate });
pm.request.headers.add({ key: 'X-Gu-Tools-HMAC-Token', value: authToken });
```

</details>


### Dealing with auth expiry in a single page webapp

In a single page webapp there will typically be an initial page load and then all communication with the server will be initiated by JavaScript.
This causes problems when the auth session expires as you can't redirect the request to the OAuth provider. To work around this
all ajax type requests should return 419 responses on auth session expiry and this should be handled by the JavaScript layer.

See also the helper [panda-session](https://github.com/guardian/panda-session) JavaScript library.
