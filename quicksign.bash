#!/bin/bash

set -e

RUNDIR="$PWD"

dirprefix=""

if [ $# -eq 1 ]; then
    if ( [ -e "$1/aapt2" ] && [ -e "$1/d8" ] && [ -e "$1/zipalign" ] && [ -e "$1/apksigner" ] ); then
        BUILD_TOOLS_DIR="$1";
        cd $BUILD_TOOLS_DIR;
        dirprefix="./";
    fi;
fi;

"$dirprefix"apksigner sign --ks "$RUNDIR/signature.keystore" --out "$RUNDIR/app-release-signed.apk" "$RUNDIR/app/build/outputs/apk/release/app-release-unsigned.apk"

echo ""
echo "Application signed."
