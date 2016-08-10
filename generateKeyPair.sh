#!/bin/bash
set -e

PRIVATE_KEY_FILE=$(mktemp /tmp/private-key.XXXXXX)
PUBLIC_KEY_FILE=$(mktemp /tmp/public-key.XXXXXX)

function on_exit() {
	rm -f ${PRIVATE_KEY_FILE}
	rm -f ${PUBLIC_KEY_FILE}
}

trap on_exit EXIT

openssl genrsa -out ${PRIVATE_KEY_FILE} 4096
openssl rsa -pubout -in ${PRIVATE_KEY_FILE} -out ${PUBLIC_KEY_FILE}

TRIMMED_PRIVATE_KEY=`cat ${PRIVATE_KEY_FILE} | sed -e '1d' -e '$d' | tr -d '\n'`
TRIMMED_PUBLIC_KEY=`cat ${PUBLIC_KEY_FILE} | sed -e '1d' -e '$d' | tr -d '\n'`

echo privateKey=${TRIMMED_PRIVATE_KEY}
echo publicKey=${TRIMMED_PUBLIC_KEY}
