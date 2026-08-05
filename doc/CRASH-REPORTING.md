# Crash reporting, and analytics

**What this is:** how Firebase is wired into this app — Crashlytics and Analytics both, since they
are the same wiring — what is switched on, and what is blocked and why.

The crashes worth catching here are in the emulator core, not in Kotlin or Java. The one real
crash found so far was `getc(NULL)` inside `trs_disk_data_read`, three frames below the CPU loop,
and it took the whole process with it. That is a native signal, so **native crash capture is the
requirement** and everything below is arranged around it.

---

## Android — working, with one manual step

### Turning it on

The app reports nothing until `app/src/main/res/values/firebase.xml` exists. Create it with four
values from the Firebase console (Project settings → Your apps → Android app):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="google_app_id" translatable="false">1:NNNNNNNNNNNN:android:XXXXXXXXXXXXXXXX</string>
    <string name="google_api_key" translatable="false">AIza...</string>
    <string name="project_id" translatable="false">your-firebase-project</string>
    <string name="gcm_defaultSenderId" translatable="false">NNNNNNNNNNNN</string>
</resources>
```

`google_app_id` is the **App ID**, `gcm_defaultSenderId` is the **Project number**, and
`google_api_key` is the Android key from **Web API Key** / the API keys list. None of them are
secret — they ship in every APK — so committing the file is a choice rather than a risk.

That is all. Firebase initialises itself from those resources, and `firebase-crashlytics-ndk`
installs handlers for both Java exceptions and native signals.

Without the file the app builds and runs and reports nowhere, which is what a fork, a contributor
and CI all need. It is the same bargain the release signing config makes.

### Why there is no google-services plugin

The usual setup applies `com.google.gms.google-services`, which reads `google-services.json` and
generates exactly the resources above. As of **4.5.0**, the newest published version, it still
reads `applicationVariants` — an API **AGP 9 removed** — so applying it fails the build outright:

```
Failed to apply plugin 'com.google.gms.google-services'.
> Could not get unknown property 'applicationVariants'
```

This project is on AGP 9.1.0, pinned by Kotlin 2.4's support window, so the plugin is simply not
usable yet. Writing the resources by hand is Firebase's own documented alternative and produces an
identical result.

### Native symbols: the manual step

A native crash arrives as a hex address unless the unstripped libraries have been uploaded. The
Crashlytics Gradle plugin normally does this on every release build via
`firebaseCrashlytics { nativeSymbolUploadEnabled true }`.

**That path is blocked too**, and not obviously: the Crashlytics plugin itself is fine on AGP 9,
but its upload task refuses to be created without the google-services plugin —

```
Could not create task ':app:uploadCrashlyticsSymbolFileRelease'.
> Google-Services plugin not configured properly.
```

— which fails `assembleRelease`. So the plugin is not applied at all, and symbols are uploaded by
hand after a release build:

```
firebase crashlytics:symbols:upload --app=<google_app_id> \
  app/build/intermediates/merged_native_libs/release/out/lib
```

Worth doing on every release, because without it a report from the emulator core is a hex address
and nothing else. Worth revisiting the moment google-services supports AGP 9: both blockages go
away together, and the whole thing becomes two plugin lines.

### Analytics

`com.google.firebase:firebase-analytics`, and nothing else. It reads the same four resources, so
turning on Crashlytics turns on Analytics as well and vice versa; there is no separate switch and
no code. Nothing was blocked here — the google-services plugin has nothing to do with Analytics.

It is worth having next to Crashlytics rather than on its own: crash-free-users, and the trail of
events leading up to a crash, both come from Analytics being present. Without it a crash report
says how often, not to how many.

To watch events arrive while testing, put the app in DebugView:

```sh
adb shell setprop debug.firebase.analytics.app org.puder.trs80
adb shell setprop log.tag.FA VERBOSE     # and restart the app
```

`shared_prefs/com.google.android.gms.measurement.prefs.xml` appearing under the app's data
directory is the quiet proof that it initialised; most of the logging happens in the Play services
process rather than this one, so an empty `logcat -s FA` does not mean it is off.

**Screens are not distinguished.** Automatic `screen_view` events name the Activity, and there is
one Activity for the whole app, so every screen reports as `Trs80Activity`. Per-screen analytics
would need the navigator to log it, which nothing does yet.

---

## iOS — not yet, and not for want of trying

**The thing that blocked this is gone.** There was no iOS app target when this was written — the
shared framework was linked by a hand-made harness calling `swiftc` — and Crashlytics on iOS is a
binary the *app* links and initialises, so there was nothing to attach it to.
`iosApp/TRS80.xcodeproj` is now a real target, so what remains is to add the Firebase SDK to it and
register an iOS app in the `trs-80` project, which today has only the Android one.

**CrashKiOS needs that binary.** Touchlab's library
(`co.touchlab.crashkios:crashlytics`, plus the `crashlyticslink` Gradle plugin) is what makes an
uncaught Kotlin exception arrive as a Kotlin stack trace instead of an opaque termination. It works
by referencing Crashlytics symbols and letting the app supply them at link time. Adding it to
`shared` before the app links Firebase would leave every consumer of the framework failing to
link — the iOS app, and anything else built against it.

So the order is: Firebase SDK in the iOS target, then CrashKiOS in `shared` and
`setCrashlyticsUnhandledExceptionHook()` at startup. Not before.

Note what is *not* blocked: the C core's crashes are native signals, and the Crashlytics iOS SDK
catches those with no Kotlin involvement at all. CrashKiOS only covers Kotlin exceptions, which on
current evidence are the less interesting half.

---

## Before shipping either

Crash reporting would be the first third-party telemetry in this app. iOS wants a
`PrivacyInfo.xcprivacy` declaring what is collected; Play wants a Data Safety declaration. Both are
product decisions rather than build ones, and neither is done.
