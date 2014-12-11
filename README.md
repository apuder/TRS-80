
TRS-80 Emulator for Android
===========================

This is the first version of a [TRS-80 Emulator for Android][TRS-80 Emulator for Android].
It is based on [sdltrs][sdltrs] that is itself derived from the popular [xtrs][xtrs]
emulator originally written for X-Windows. This port adds a layer for Android.

Compiling with gradle
---------------------
-  Create a 'local.properties' file to define your SDK location, e.g.:
``
sdk.dir=/Users/haeberling/Downloads/android-sdk-macosx
``
-  Change the 'buildToolsVersion' in build.gradle to match your latest version.
You will find this inside your sdk's ``build-tools`` directory.
-  Run:
``
 ./gradlew assembleRelease 
``

Compiling from Source
---------------------

It is recommended to use Eclipse for compiling the sources. You will
need to install the Android SDK and NDK. You should also install the
accompanying ADT plugin for Eclipse (be sure to check "NDK" when
installing the ADT for Android in Eclipse).

Next clone the TRS-80 Emulator sources via git:

``
git clone git clone git://git.code.sf.net/p/trs80/code trs80
``

The TRS-80 emulator depends on two [Support Libraries][Support Library]
(v7 appcompat and media router libraries) as well as the
[Google Play Services][Google Play Services]. 
In order to make the project independent of user-specific directory layouts,
those Libraries need to be copied to the parent directory where the emulator
sources were cloned:

    cp -r <android-sdk-root>/extras/android/support/v7/appcompat/*
          <parent-dir-of-git-repo>/android-support-v7-appcompat/
    cp -r <android-sdk-root>/extras/android/support/v7/mediarouter/*
          <parent-dir-of-git-repo>/android-support-v7-mediarouter/
    cp -r <android-sdk-root>/extras/google/google_play_services/libproject/*
          <parent-dir-of-git-repo>/google-play-services_lib/


Note that these libraries need to be imported as Android Library projects in
Eclipse, otherwise there will be compile errors with the TRS-80 emulator
sources.

At this point you should be able to compile the sources. Note that running
the TRS-80 emulator inside the Android emulator is very slow and Chromecast
is also not supported by the Android emulator. It is recommended to use a real
device for testing and debugging.


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
[Support Library]:http://developer.android.com/tools/support-library/setup.html
[Google Play Services]:http://developer.android.com/google/play-services/setup.html
[Retro Fonts]:http://www.kreativekorp.com/software/fonts/index.shtml#retro
[Font Squirrel]:http://www.fontsquirrel.com/fonts/DejaVu-Sans-Mono
[Icons 1]:http://www.iconarchive.com/show/oxygen-icons-by-oxygen-icons.org/Mimetypes-inode-directory-icon.html
[Icons 2]:http://www.iconarchive.com/show/oxygen-icons-by-oxygen-icons.org/Mimetypes-mime-2-icon.html
[ACRA]:http://acra.ch/
[ACRA Mailer]:https://github.com/d-a-n/acra-mailer
