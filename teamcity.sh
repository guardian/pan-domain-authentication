#!/usr/bin/env bash

set -e

setupNvm() {
    pushd pan-domain-node
    
    export NVM_DIR="$HOME/.nvm"
    [[ -s "$NVM_DIR/nvm.sh" ]] && . "$NVM_DIR/nvm.sh"  # This loads nvm

    nvm install
    nvm use

    popd
}

node() {
    pushd pan-domain-node

    npm install
    npm run build
    npm test

    popd
}

riffRaffUpload() {
    sbt clean compile test riffRaffUpload
}

main() {
    setupNvm
    node
    riffRaffUpload
}

main
