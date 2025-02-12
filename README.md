Pan Domain Authentication
pan-domain-auth-core Scala version support

This repo - General docs & Scala implementation
pan-domain-node - Typescript implementation
Pan domain authentication provides distributed authentication for multiple webapps running in the same domain. Each application can authenticate users against an OAuth provider and store the authentication information in a common cookie. Each application can read this cookie and check if the user is allowed in the specific application and allow access accordingly.

This means that users are only prompted to provide authentication credentials once across the domain and any inter-app interactions (e.g javascript cross-origin requests) can be easily secured.

How it works
The library can be used in two ways:

Verify: read the Panda cookie and check whether the user is valid for the request
Issue: as above but redirecting the user to the OAuth provider to authenticate if the cookie is not present or expired
Simply verifying the cookie is useful for APIs that cannot provide a user-facing OAuth dance to acquire credentials. It is also useful to minimise the parts of your application that have to have knowledge of the private key.

To ensure the cookie is not tampered with, public/private key pair encryption is use. An issuing application signs the cookie using the private key and both verifying and issuing applications verify using the public key.

The cookie contains an expiry time generated at issue after which the user should be redirected to the OAuth provided again.

All OAuth 2.0 compliant providers are supported. There is additional support for verifying that the user has two-factor login enabled when using Google as the provider.

Each authenticated request that an application receives should be checked to see if there is a auth cookie.

If the cookie is not present then the user should be sent to the OAuth provider for authentication. Upon their return the user information should be checked and if the user is allowed in the app then the shared cookie should be set marking the user as valid in the application.

If there is a cookie but the cookie does not indicate that the the user is valid then the user should be validated for the application. This is an application-specific concern such as verifying the email address or checking two-factor is enabled. If they are valid then the cookie should be updated to indicate this or an error page displayed.

If there is a cookie and it indicates the user is valid in this application then the request should be processed as normal.

if there is a cookie but it indicates the the authentication is expired then the user should be sent off to the provider to renew their session. On their return the existing cookie is updated with the new expiry time.

What's provided
Pan domain auth is split into 6 modules.

The pan-domain-auth-verification library provides the basic functionality for signing and verifying login cookies in Scala. For JVM applications that only need to VERIFY an existing login (rather than issue logins themselves) this is the library to use.

The pan-domain-auth-core library provides the core utilities to load settings, create and validate the cookie and check if the user has mutli-factor auth turned on when using Google as the provider.

The pan-domain-auth-play_2-8, 2-9 and 3-0 libraries provide an implementation for play apps. There is an auth action that can be applied to the endpoints in your application that will do checking and setting of the cookie and will give you the OAuth authentication mechanism and callback. This is the only framework specific implementation currently (due to play being the framework predominantly used at The Guardian), this can be used as reference if you need to implement another framework implementation. This library is for applications that need to be able to issue and verify logins which is likely to include user-facing applications.

The pan-domain-node library provides an implementation of verification only for node apps.

The pan-domain-auth-example provides an example Play 2.9 app with authentication. Additionally the nginx directory provides an example of how to set up an nginx configuration to allow you to run multiple authenticated apps locally as if they were all on the same domain which is useful during development.

The panda-hmac libraries build on pan-domain-auth-play to also verify machine clients, who cannot perform OAuth authentication, by using HMAC-SHA-256.

Requirements
If you are adding a new application to an existing deployment of pan-domain-authentication then you can skip to Integrating With Your App

At least one webapp running on subdomains of a single domain (e.g. app1.example.com and app2.example.com)

The apps must be using https - the cookie set by pan domain auth are set to secure and http only

An OAuth provider to use for authentication

Setting up your domain configuration
Panda is compatible with all OAuth2 providers, automatically discovering endpoints using a discovery document.

AWS Cognito
Follow these steps to create a new Cognito User Pool. This allows you to manage your users entirely within AWS.

You will need:

The AWS CLI and credentials for your AWS account
An OAuth callback URL for each application that will be issuing logins
Run the following commands:

Deploy the CloudFormation Template
Generate settings and public/private keys
./cognito/generate-settings.sh ${CLOUDFORMATION_STACK} ${REGION}
This will also upload them to the configuration bucket
Add users
./cognito/add-user.sh ${CLOUDFORMATION_STACK} ${USER_EMAIL} ${REGION}
They will receive an email invite with a temporary password
Generic OAuth2 Provider
You will need:

An AWS S3 bucket where the configuration for your domain will live
Both a Client ID and secret are required
An OAuth discovery document is required
Example Google: https://accounts.google.com/.well-known/openid-configuration
Example AWS Cognito: https://cognito-idp.eu-west-1.amazonaws.com/eu-west-1_nW3FKqRh0/.well-known/openid-configuration
You must also configure the callback URLs with your provider, one for each application under the domain that will be issuing logins.
