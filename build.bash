#!/bin/bash

set -e

RUNDIR="$PWD"

dirprefix=""

if [ $# -ge 1 ]; then
    if [ $# -eq 2 ]; then
        if ( [ -e "$2/aapt2" ] && [ -e "$2/d8" ] && [ -e "$2/zipalign" ] && [ -e "$2/apksigner" ] ); then
            BUILD_TOOLS_DIR="$2";
            cd $BUILD_TOOLS_DIR;
            dirprefix="./";
        fi;
    fi;
    ANDROID_SDK_PLATFORM="$1"
fi;

echo "Preparing resources."

if [ -d "$RUNDIR/build" ]; then
    rm -rf "$RUNDIR/build";
fi

mkdir "$RUNDIR/build"

mkdir "$RUNDIR/build/gen"

if [ -d "$RUNDIR/build/gen" ]; then
    rm -rf "$RUNDIR/build/gen";
fi

mkdir "$RUNDIR/build/gen"

"$dirprefix"aapt2 compile --dir "$RUNDIR/res" -o "$RUNDIR/build/resources.zip"
"$dirprefix"aapt2 link -I $ANDROID_SDK_PLATFORM/android.jar --manifest "$RUNDIR/AndroidManifest.xml" --java "$RUNDIR/build/gen" -o "$RUNDIR/build/res.apk" "$RUNDIR/build/resources.zip"
rm "$RUNDIR/build/resources.zip"

echo "Compiling sources."

if [ -d "$RUNDIR/build/obj" ]; then
    rm -rf "$RUNDIR/build/obj";
fi

mkdir "$RUNDIR/build/obj"

javac -Xlint:all -source 1.8 -target 1.8 -bootclasspath $ANDROID_SDK_PLATFORM/android.jar -sourcepath java:gen $(find "$RUNDIR/src" "$RUNDIR/build/gen" -type f -name '*.java') -d "$RUNDIR/build/obj"

rm -rf "$RUNDIR/build/gen"

echo "Linking libraries."

"$dirprefix"d8 --release --classpath $ANDROID_SDK_PLATFORM/android.jar --output "$RUNDIR/build" $(find "$RUNDIR/build/obj" -type f)

rm -rf "$RUNDIR/build/obj"

echo "Packing application."

zip -j -u "$RUNDIR/build/res.apk" "$RUNDIR/build/classes.dex"

rm "$RUNDIR/build/classes.dex"

if [ -f "$RUNDIR/build/out-aligned.apk" ]; then
    rm "$RUNDIR/build/out-aligned.apk";
fi


"$dirprefix"zipalign 4 "$RUNDIR/build/res.apk" "$RUNDIR/build/out-aligned.apk"
rm "$RUNDIR/build/res.apk"

if [ ! -f "$RUNDIR/signature.keystore" ]; then
  echo "Unsigned build complete."
  exit
fi

echo "Signing application."
"$dirprefix"apksigner sign --ks "$RUNDIR/signature.keystore" --out "$RUNDIR/build/app-build.apk" "$RUNDIR/build/out-aligned.apk"
rm "$RUNDIR/build/out-aligned.apk"

echo "Build complete."
