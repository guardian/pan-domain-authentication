# Pan Domain Authentication

Pan domain authentication provides distributed authentication for multiple webapps running in the same domain. Each
application can authenticate users (currently by using Google's oauth mechanism, but other mechanisms could be added in the future)
and store the authentication information in a common cookie. Each application can read this cookie and check if the user is allowed
in the specific application and allow access accordingly.

This means that users are only prompted to provide authentication credentials once across the domain and any inter-app
interactions (e.g javascript CORS or jsonp requests) can be easily secured.

## How it works

Each application in the domain is configured with the domain, an application name and an AWS key. The AWS key allows the
application to connect to an S3 bucket (```pan-domain-auth-settings```)and download the domain the settings (in a 
```<domain>.settings``` file). The downloaded settings configure the shared secret used to sign the cookie and the credentials
needed to authenticate with google.

Each authenticated request that an application receives is checked to see if there is a auth cookie. 

* If the cookie is not present then the user is sent to google for authentication. Upon returning from google the use information is
checked and if the user is allowed in the app then the shared cookie is set marking the user as valid in the application.

* If there is a cookie but te cookie does not indicate that the the user is valid in the application then the user is validated for the application.
if they are valid then the cookie is updated to indicate this otherwise an error page is displayed.

* If there is a cookie and it indicates the user is valid in this application then the request is processed as normal.

* if there is a cookie but it indicated the the authentication is expired then the user is sent off to google to renew their session.
On returning from google the existing cookie is updated with the new expiry time.

## What's provided

Pan domain auth is split into 3 modules. 

The ```pan-domain-auth-core``` library provides the core utilities to load the domain settings, create and validate the cookie and
check if the user has mutlifactor auth turned on (see below). Note this does not include the google oath dance code or cookie setting
as these vary based on web framework being used by implementing apps

The ```pan-domain-auth-play``` library provides an implementation for play apps. There is an auth action that can be applied to the
endpoints in you appliciation that will do checking and setting of the cookie and will give you the google authentication mechanism
and callback. This is the only framework specific implementation currently (due to play being the framework predominantly used at the
guardian), this can be used as reference if you need to implement another framework implementation.

The ```pan-domain-auth-example``` provides an example app with authentication. This is implemented in play and is used for testing.
Additionally the nginx directory provides an example of how to set up an nginx configuration to allow you to run multiple authenticated
apps locally as if they were all on the same domain (also useful for testing)

The ```pan-domain-auth-core``` and ```pan-domain-auth-play``` libraries are available on maven central cross compiled for scala
2.10.4 and 2.11.1. to include them via sbt:
 
```
"com.gu" %% "pan-domain-auth-core" % "0.2.1"
```

or

```
"com.gu" %% "pan-domain-auth-play" % "0.2.1"
```


## Requirements

To use pan domain authentication you will need:

* At least one webapp running on subdomains of a single domain (eg. app1.example.com and app2.example.com)

* The apps must be using https - the cookie set by pan domain auth are set to secure and http only

* An AWS S3 bucket names ```pan-domain-auth-settings``` where the configuration for your domain will live

* The AWS login credentials for a user that can read from the said bucket (it is recommended that this is the only thing that the user is allowed to do in your s3 account)

* An app set up in google with access to the google+ api (this is used for the actual authentication):

    * get a set of API credentials for your app from the [Google Developer Console](https://console.developers.google.com)
    * ensure that you have switched on access to the `Google+ API` for your credentials
    * configure all the oath callbacks used by your apps
    
* A configuartion file in the S3 bucket named ```<domain>.settings```


## Setting up your domain configuration

The configuration file is named for the domain and is a simple properties style file. For all apps on the *.example.com
domain the file would be called example.com.settings. The contents of the file would look something like this:

    secret=example_secret
    cookieName=exampleAuth
    
    googleAuthClientId=example_google_client
    googleAuthSecret=example_google_secret
    
    googleServiceAccountId=serviceAccount@developer.gserviceaccount.com
    googleServiceAccountCert=name_of_cert_in_bucket.p12
    google2faUser=an.admin@example.com
    multifactorGroupId=group@2fa_admin_user


* **secret** - this is the shared secret used to sign the cookie

* **cookieName** - this is what the shared cookie is called

* **googleAuthClientId** - this is the client id for the google app you autheniticate with - this is obtained from the google [Google Developer Console](https://console.developers.google.com)

* **googleAuthSecret** - this is the secret for the google app you autheniticate with - this is obtained from the google [Google Developer Console](https://console.developers.google.com)

* **googleServiceAccountId, googleServiceAccountCert, google2faUser and multifactorGroupId** - these are optional parameters for using a group based 2 factor auth verification, see explanation below


## Integrating with your app

### Using the play implementation

If you are using play then use the play library, this provides the actions that allow you to secure your endpoints.

Create a pan domain auth actions trait that extends the ```AuthActions``` trait in the the play lib. This trait will
provide the config needed to connect to the aws bucket and the domain and app you are using. You will also need to
add a method here to ensure that any authenticated user is valid in your specific app (and this could be used to create
users in you app's datastore). You should also provide the full url of the endpoint that will handle the oauth callback
from google 


    package controllers
    
    import com.gu.pandomainauth.action.AuthActions
    import com.gu.pandomainauth.model.AuthenticatedUser
    
    trait PanDomainAuthActions extends AuthActions {
    
      import play.api.Play.current
      lazy val config = play.api.Play.configuration
    
      override def validateUser(authedUser: AuthenticatedUser): Boolean = {
        (authedUser.user.email endsWith ("@guardian.co.uk")) && authedUser.multiFactor
      }
    
      override def authCallbackUrl: String = config.getString("host").get + "/oauthCallback"
    
      override lazy val domain: String = config.getString("pandomain.domain").get
      
      lazy val awsSecretAccessKey: String = config.getString("pandomain.aws.secret")
      lazy val awsKeyId: String = config.getString("pandomain.aws.keyId")
      override lazy val awscredentials = for(key <- awsKeyId; secret <- awsSecretAccessKey) yield {new BasicAWSCredentials(key, secret)}
      
      override lazy val system: String = "workflow"
    }
    

Create a controller that will handle the oauth callback and logout actions, add these actions to the routes file.

    package controllers
    
    import play.api.mvc._
    import scala.concurrent.Future
    import scala.concurrent.ExecutionContext.Implicits.global
    
    object Login extends Controller with PanDomainAuthActions {
    
      def oauthCallback = Action.async { implicit request =>
        processGoogleCallback()
      }
    
      def logout = Action.async { implicit request =>
        Future(processLogout)
      }
    }
    

Add the ```AuthAction``` or ```ApiAuthAction``` to any endpoints you with to require an authenticated user for.

    package controllers
    
    import scala.concurrent.ExecutionContext.Implicits.global
    import lib._
    import play.api.mvc._
    
    
    object Application extends Controller with PanDomainAuthActions {
    
      def loginStatus = AuthAction { request =>
        val user = request.user
        Ok(views.html.loginStatus(user.toJson))
      }
      
      def getItems = APIAuthAction { implicit req =>
        ...
      }
    
      ...
    }


* ```AuthAction``` is used for endpoints that the user requests and will redirect unauthenticated users to google for authentication.
  Use this for standard page loads etc.
  
* ```ApiAuthAction``` is used for api ajax / xhr style requests and will not redirect to google for auth. This action will either process
  the action or return an error code that can be processed by your client javascript (see section on handling expired logins in a single
  page webapp). The response codes are:
  
    * **401** - user not authenticated - probably tricky to get this response as presumably the user has already loaded a page that would have
            logged them in
            
    * **403** - not authorised - occurs then the user is authenticated but not valid in this app, this can happen when making cross app CORS
            requests
            
    * **419** - authorisation expired - occurs when the authorisation with google has expired (after 1 hour), you will need to re auth with
            google to reestablish the session, this can typically be done transparently on the next page load request.

Both the actions add the current user to the request, this is available as ```request.user``` 

### Using pan domain auth with another framework

Other scala frameworks exist as well as play. Full framework libraries are not provided for these yet as we predominantly use play at the guardian.
To use pan domain auth with another framework you will need to provide an equivalent of the user auth checking in the play actions and provide an
implementation of the google oauth integration. Have a look at how this is done in the play library and provide your own implementation for
your framework and http client etc.

More examples and framework clients may be added in the future as they become available.

### configuring access to the S3 bucket

Access to the s3 bucket is controlled by overriding the ```awsCredentials``` and ```awsRegion``` options in the ```PanDomainAuth``` trait (or the 
```AuthActions``` sub trait in the play implementation).
 
* **awsCredentials** defaults to None - this means that the instance profile of your app running in EC2 will be used. You can configure access to the bucket
in your cloud formation script. For apps tha are not running in EC2 (such as developer environments) you can supply ```BasicAWSCredentials``` with a key and secret
for a user that will grant access to the bucket.

* **awsRegion** defaults to eu-west-1 - This is where the guardian runs the majority of it's aws estate so is a useful default for us.



### The user object

The user object is defined as:

    case class User(firstName: String, lastName: String, email: String, avatarUrl: Option[String])
    
Hopefully the fields are clear as to what they are. There is a budget toJson method on it that will give a json string representation of
the user which can be consumed by your javascript, this method does no use any json libraries so should work for any framework and library
choices you've made in you implementing app.

### Validating the user

As different apps may have different requirements on user validity each individual app should provide a user validation mechanism. The validation
method takes in an ```AuthenticatedUser``` object which contains the user object and metadata about the authentication.

    case class AuthenticatedUser(user: User, authenticatingSystem: String, authenticatedIn: Set[String], expires: Long, multiFactor: Boolean)
    
The fields are:

* **user** - the user object
* **authenticatingSystem** - the app name of the app that authenticated the user
* **authenticatedIn** - the set of app names that this user is known to be valid, this prevents revalidation
* **expires** - the authentication session expiry time in milliseconds, after this has passed then the session is invalid and the user will need to 
                be reauthenticated with google. There is a handy method to check if the authentication is expired ```def isExpired = expires < new Date().getTime```
* **multiFactor** - true if the user's authentication used a 2 factor type login. This defaults to false                


## Using Google group based 2 factor authentication validation

Some applications may require that a multifactor authentication is used when authenticating a user. Since it is not possible to tell if this happened
from the standard callback this is checked by asserting that the user is in a google group that enforces 2 factor auth (this was the workaround suggested
by google themselves when we asked about checking 2fa). Since the group is likely set up within an apps for domains setup and not accessible to everyone
checking the 2fa group uses different google credentials from the main auth.

To configure multifactor checking you will need to create a service account that can access the google directory api,
see [directory API docs](https://developers.google.com/admin-sdk/directory/v1/guides/delegation). once this is configured fill in all the following properties
in the domain's property file and upload the service accounts cert to the s3bucket. If you do not wish to use this feature just omit the
properties:

* **googleServiceAccountId** - the service account email that is set up to allow access to the directory API
* **googleServiceAccountCert** - the name within the bucket of the certificate used to validate the service account
* **google2faUser** - the admin user to connect to the directory api as, this is not the service account user but a user in your org who is authorised to access group information
* **multifactorGroupId** - the name of the group that indicates and enforces that 2fa is turned on


## Dealing with auth expiry in a single page webapp

In a single page webapp there will typically be an initial page load and then all communication with the server will be initiated by javascript.
This causes problems when the auth session expires as you can't redirect the request to google to reauthenticate the request. To work around this
all ajax type requests should return 419 responses on auth session expiry and this should be handled by the javascript layer.

*example javascript reauthentication code coming soon*


## A note for guardian developers

At the guardian we are using pan domain auth on our tools domain. To add your tools apps you will need the s3 credentials and the oauth callbacks
set up in google for you app. To get this done come and speak to Swells or the tools team.
