#!/usr/bin/python

import hashlib
import hmac
from optparse import OptionParser
from datetime import datetime
import base64
from email.utils import formatdate
import requests
from time import mktime
from urllib.parse import urlparse
from pprint import pprint

def get_token(uri, secret):
    httpdate = formatdate(timeval=mktime(datetime.now().timetuple()),localtime=False,usegmt=True)
    url_parts = urlparse(uri)

    string_to_sign = "{0}\n{1}".format(httpdate, url_parts.path)
    print("string_to_sign: " + string_to_sign)
    hm_digest = hmac.new(secret.encode('utf-8'), string_to_sign.encode('utf-8'), hashlib.sha256).digest()
    base64encodedDigest = base64.b64encode(hm_digest).decode('utf-8')
    return "HMAC {0}".format(base64encodedDigest), httpdate

#START MAIN
parser = OptionParser()
parser.add_option("-u", "--uri", dest="uri", help="URI to access")
parser.add_option("-s", "--secret", dest="secret", help="shared secret to use")
(options, args) = parser.parse_args()

if options.uri is None:
    print("You must supply the uri in --uri")
    exit(1)
if options.secret is None:
    print("You must supply the password in --secret")
    exit(1)

uri = options.uri
print("uri is " + uri)
authtoken, httpdate = get_token(uri, options.secret)
print(authtoken)

headers = {
        'X-Gu-Tools-HMAC-Date': httpdate,
        'X-Gu-Tools-HMAC-Token': authtoken
}

print(headers)
response = requests.get(uri,headers=headers)
print("Server returned {0}".format(response.status_code))
pprint(response.headers)
if response.status_code==200:
    pprint(response.text)
else:
    print(response.text)
