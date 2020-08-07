#!/usr/bin/env bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

brew install guardian/devtools/dev-nginx
dev-nginx setup-app "$DIR"/dev-nginx.yaml
