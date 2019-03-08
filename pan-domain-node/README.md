# pan-domain-node
NodeJs implementation of pan-domain auth verification

## Installation

`npm install --save git://github.com/guardian/pan-domain-node.git`

## Dependencies

`pan-domain-node` requires access to cookies, if your application is not using them already you should install `cookie-parser`.

## Usage

Setup:

```JavaScript
// app.js

var PanDomainNode = new require('pan-domain-node')('MY_APPLICATION_DOMAIN');

PanDomainNode.setLogLevel('info'); // Defaults to 'error'

```

As [express.js](http://expressjs.com/) / connect style middleware:

```JavaScript
// index.js

var pandaAuthMiddleware = PanDomainNode.Middleware;

router.get('/', pandaAuthMiddleware, function (req, res, next) {
    var guardianUser = req.guUser;

    // Do stuff with a verified user logged in
});

```

Error handling (see [express.js docs](http://expressjs.com/guide/error-handling.html)):

```JavaScript
// app.js

var PANDA_AUTH_ERROR_MESSAGE = PanDomainNode.PandaAuthFailedErrorMessage;

app.use(function(err, req, res, next) {
    if (err === PANDA_AUTH_ERROR_MESSAGE) {
        // redirect to sign in
        res.status(401).send('Please sign in to a gutools.co.uk application.');
    } else {
        next(err);
    };
});
```

## Logging

By default `pan-domain-node` logs to `console`. You can configure a custom logger

```js
function customLogger (level, message) {
	// level is either 'error',  'info', 'debug'
}

PanDomainNode.setLogger(customLogger);
```

This allow for instance to send logs to [winston](https://github.com/winstonjs/winston) or [bunyan](https://github.com/trentm/node-bunyan).
