<img src="icons_vector/app_icon.svg" alt="DroidRec Icon" width="200"/>

English | [עברית](README.he.md)

## About
DroidRec is an open-source Android screen recorder.

Audio Playback recording requires Android 10 or later. No Root needed.

<img src="metadata/en-US/images/phoneScreenshots/1.jpg" alt="DroidRec Screenshot" width="400"/>

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.yakovlevegor.DroidRec/)

Or download the apk from [the release section](https://github.com/yakovlevegor/DroidRec/releases).

## Building
Generate your keystore with `keytool -genkeypair -keystore mykey.keystore -validity 365000 -keysize 4096 -keyalg RSA` and place it in this folder under `signature.keystore` name.
(**Note**: Run keytool with the same or older Java version on which you are going to build the application)

To build this app, run **build.bash** with arguments:
1. Path to your Android SDK "platforms/android-*version*"
2. (*Optional*) If you don't have Android SDK build tools installed on your system, specify path to your Android SDK "build-tools/*version*"

Example: `./build.bash path/to/android/sdk/platforms/android-33 path/to/android/sdk/build-tools/33.0.0`

## License

#### The Unlicense
```
This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.

In jurisdictions that recognize copyright laws, the author or authors
of this software dedicate any and all copyright interest in the
software to the public domain. We make this dedication for the benefit
of the public at large and to the detriment of our heirs and
successors. We intend this dedication to be an overt act of
relinquishment in perpetuity of all present and future rights to this
software under copyright law.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

For more information, please refer to <http://unlicense.org/>
```
