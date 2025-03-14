#!/usr/bin/python

import hashlib
import hmac
from optparse import OptionParser
from datetime import datetime
import base64
from email.utils import formatdate
import requests
from time import mktime
from urlparse import urlparse
from pprint import pprint

def get_token(uri, secret):
    httpdate = formatdate(timeval=mktime(datetime.now().timetuple()),localtime=False,usegmt=True)
    url_parts = urlparse(uri)

    string_to_sign = "{0}\n{1}".format(httpdate, url_parts.path)
    print "string_to_sign: " + string_to_sign
    hm = hmac.new(secret, string_to_sign,hashlib.sha256)
    return "HMAC {0}".format(base64.b64encode(hm.digest())), httpdate

#START MAIN
parser = OptionParser()
parser.add_option("--host", dest="host", help="host to access", default="video.local.dev-gutools.co.uk")
parser.add_option("-a", "--atom", dest="atom", help="uuid of the atom to request")
parser.add_option("-s", "--secret", dest="secret", help="shared secret to use")
(options, args) = parser.parse_args()

if options.secret is None:
    print "You must supply the password in --secret"
    exit(1)

uri = "https://{host}/pluto/resend/{id}".format(host=options.host, id=options.atom)
print "uri is " + uri
authtoken, httpdate = get_token(uri, options.secret)
print authtoken

headers = {
        'X-Gu-Tools-HMAC-Date': httpdate,
        'X-Gu-Tools-HMAC-Token': authtoken
}

print headers
response = requests.post(uri,headers=headers)
print "Server returned {0}".format(response.status_code)
pprint(response.headers)
if response.status_code==200:
    pprint(response.json())
else:
    print response.text
