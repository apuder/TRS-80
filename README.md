
TRS-80 Emulator for Android
===========================

This is the first version of a [TRS-80 Emulator for Android][TRS-80 Emulator for Android].
It is based on [sdltrs][sdltrs] that is itself derived from the popular [xtrs][xtrs]
emulator originally written for X-Windows. This port adds a layer for Android.

Compiling from Source
---------------------

It is recommended to use Android Studio for compiling the sources. You will
need to install the Android SDK and NDK.

Next clone the TRS-80 Emulator sources via git:

``
git clone git://git.code.sf.net/p/trs80/code TRS-80
``

Next import the project into Android Studio via File -> Import Project...
At this point you should be able to compile the sources. Note that running
the TRS-80 emulator inside the Android emulator is very slow and Chromecast
is also not supported by the Android emulator. It is recommended to use a real
device for testing and debugging.


Compiling with Gradle
---------------------
- Download and unpack the Android SDK and Android NDK.
- Run the Android SDK Manager and make sure you have the following installed:
    - Android SDK Tools
    - Android SDK Platform-tools
    - Android SDK Built-tools (latest version, write down the revision number)
    - The latest SDK Platform
    - Under 'Extras':
        - Android Support Repository
        - Android Support Library
        - Google Play services
        - Google Repository
        - For Windows users: Google USB driver, if you have a Nexus phone.
-  Create a 'local.properties' file to define your SDK and NDK locations, e.g.:
``sdk.dir=/Users/johndoe/Downloads/android-sdk-macosx``
``ndk.dir=/Users/johndoe/Downloads/android-ndk-r9d``
-  Change the 'buildToolsVersion' in build.gradle to match your latest version.
You will find this inside your sdk's ``build-tools`` directory or check the SDK
manager entry.
-  Run:
``
 ./gradlew assembleDebug
``
- Then install:
``
adb install -d -r app/build/outputs/apk/app-debug.apk
``

Quick Overview
--------------

The original C sources of sdltrs reside in the 'jni' folder. The Android
layer uses JNI (Java Native Interface) to access the C sources from
Java. Whenever I made a change to the original sdltrs sources, I used
\#ifdef ANDROID to annotate my changes. This should make it easy to see
what was changed.

Some of the key files:

* MainActivity.java: main entry point of the Android app.
* EmulatorActivity.java: this is the Android activity that
  runs the emulator.
* XTRS: this class is the gateway to the C sources of xtrs/sdltrs.
  All down-calls and up-calls to the C code will go through
  this class. The native methods declared in XTRS are implemented
  in jni/native.c. Folder jni/SDL contains a SDL emulation to
  facilitate integration of the sdltrs sources.
* Hardware: defines the hardware characteristics of the TRS machine
  (e.g., TRS Model, disks to be mounted, etc). An instance of class
  Hardware is passed to XTRS.init() to initialize the native layer.
* keyboard_original.xml: Android XML layout that defines the
  original Model 3 keyboard layout.
* Key.java: the accompanying Android custom widget that implements
  the behavior of one key of the keyboard.


Details of the keyboard
-----------------------

The emulator features different keyboard layouts (original, compact, etc).
The XML layout resources can be found in res/layout/keyboard_\*.xml. Class Key
implements the behavior of one key of the keyboard. Class Key is a custom
Android widget that is referenced from the aforementioned XML layout files.
Whenever the user 'clicks' on a key, class Key uses the KeyboardManager to
add a key event which will eventually be delivered to xtrs via SDL_PollEvent.
File res/values/attrs.xml defines an enum for all the keys available on a TRS
machine. The TK_\* constants in class Key mirror the definitions in attrs.xml.
When a user presses a key, the TK_\* ID needs to be mapped to a
SDL_KeyboardEvent. Specifically, the SDL virtual key code and the unicode
character are needed to populate SDL_KeyboardEvent. This mapping is achieved
with file res/xml/keymap_us.xml. Once the virtual key code and unicode
character are determined, KeyboardManager uses XTRS.addKeyEvent() to fill a
key buffer in the native layer.


External Resources
------------------

The following resources have been used for this project:

* [sdltrs][sdltrs]
* [xtrs][xtrs]
* [Retro Fonts][Retro Fonts]
* [Font Squirrel][Font Squirrel]
* [Icons 1][Icons 1]
* [Icons 2][Icons 2]
* [ACRA][ACRA]
* [ACRA Mailer][ACRA Mailer]

[TRS-80 Emulator for Android]:https://play.google.com/store/apps/details?id=org.puder.trs80
[sdltrs]:http://sdltrs.sourceforge.net/
[xtrs]:http://www.tim-mann.org/xtrs.html
[Retro Fonts]:http://www.kreativekorp.com/software/fonts/index.shtml#retro
[Font Squirrel]:http://www.fontsquirrel.com/fonts/DejaVu-Sans-Mono
[Icons 1]:http://www.iconarchive.com/show/oxygen-icons-by-oxygen-icons.org/Mimetypes-inode-directory-icon.html
[Icons 2]:http://www.iconarchive.com/show/oxygen-icons-by-oxygen-icons.org/Mimetypes-mime-2-icon.html
[ACRA]:http://acra.ch/
[ACRA Mailer]:https://github.com/d-a-n/acra-mailer
