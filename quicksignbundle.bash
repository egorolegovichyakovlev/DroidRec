#!/bin/bash

if [ $# -eq 1 ]; then
    jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore signature.keystore -signedjar app-release-signed.aab app/build/outputs/bundle/release/app-release.aab $1

    echo ""
    echo "Bundle signed."
fi;

if [ $# -ne 1 ]; then
    echo "Please provide your key name."
fi;
