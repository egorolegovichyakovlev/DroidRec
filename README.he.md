![DroidRec תמונה](https://raw.githubusercontent.com/yakovlevegor/DroidRec/main/app_icon_big.png "תמונה מיישום")

![DroidRec צילם](metadata/he/images/phoneScreenshots/1.jpg "צילם מיישום")

[English](https://github.com/yakovlevegor/DroidRec/blob/main/README.md) | עברית

## סביב
רשום אנדרואיד מסך.
לרשום מצליל אתה/אתך צריך/צריכה אנדרואיד 10 או יותר מאוחר. לא רוט הכרחי.

[רכש היישום](https://github.com/yakovlevegor/DroidRec/releases)

## בנוה
ברא מפתח

`keytool -genkeypair -keystore mykey.keystore -validity 365000 -keysize 4096 -keyalg RSA`

ושים בשם

`signature.keystore`.

(**עיון**: היי אחד נוסח מג'אווה)

בנוה היישום פתח **build.bash** וטענות:

א. נתיב אל אנדראיד

"platforms/android-*version*"

ב. (*לא חשוב*) אלו אין תוכנות בבנוה מאנדראיד שקע הנתיב

"build-tools/*version*"

למשל:

`./build.bash path/to/android/sdk/platforms/android-33 path/to/android/sdk/build-tools/33.0.0`

## התעודה

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
