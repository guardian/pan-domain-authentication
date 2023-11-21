const crypto = require('crypto');
const reqwest = require('reqwest');

// The secret you share with the remote service.
// Should *NOT* be hard coded, put it somewhere private (S3, Dynamo, properties file, etc.)
const sharedSecret = "Sanguine, my brother.";

// Make a hmac token from the required components. You probably want to copy this :)
function makeHMACToken(secret, date, uri) {
    const hmac = crypto.createHmac('sha256', secret);

    const content = date + '\n' + uri;

    hmac.update(content, 'utf-8');

    return "HMAC " + hmac.digest('base64');
}

// It's important to remember the leading /
const uri = "/api/examples";
const date = (new Date()).toUTCString();
const token = makeHMACToken(sharedSecret, date, uri);

// Make a request to our example API with the generated HMAC
reqwest({
    url: "http://example.com" + uri,
    method: 'GET',
    headers: {
        'X-Gu-Tools-HMAC-Date': date,
        'X-Gu-Tools-HMAC-Token': token,
        'X-Gu-Tools-Service-Name': 'example-service-name'
    }
}).then(function(resp) {
    console.log('We did it!');
}, function(err, msg) {
    console.error('Something went wrong :(');
});
