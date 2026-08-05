# TRS-80

![build](https://github.com/apuder/TRS-80/actions/workflows/build-with-gradle.yml/badge.svg)

A TRS-80 emulator — Model I, III, 4 and 4P — for Android, iOS and the web.

| | |
| --- | --- |
| Android | [Google Play](https://play.google.com/store/apps/details?id=org.puder.trs80) |
| iOS | [App Store](https://apps.apple.com/app/id6797618793) |
| Web | [trs-80.web.app](https://trs-80.web.app) |

The emulator itself is [sdltrs][sdltrs], derived from [xtrs][xtrs], in C. Around
it is one Kotlin Multiplatform module holding the entire app — every screen, the
domain, the emulator session — and three thin hosts that supply a window and the
few things only a platform can answer. The three apps are the same app.

## What is where

| | |
| --- | --- |
| `shared/src/commonMain` | the app: every screen, the domain, the emulator session |
| `shared/src/androidMain`, `iosMain`, `wasmJsMain` | only what one platform can answer — files, clipboard, links, input devices |
| `app/` | the Android host: the JNI binding, one activity, an Application |
| `app/src/main/c` | the emulator, shared by all three |
| `iosApp/` | the iOS host: a window and one view controller |
| `doc/` | the modernization plan, the UI spec, what is still missing |

More on how it got this shape, and on the conventions worth keeping, in
[AGENTS.md](AGENTS.md).

## Before you start

| For | You need |
| --- | --- |
| Anything | JDK 21 |
| Android | Android SDK 37 and NDK 28.2.13676358 — Android Studio installs both |
| iOS | Xcode, on a Mac |
| Web | [Emscripten](https://emscripten.org/docs/getting_started/downloads.html), with `emcc` on the path |
| Publishing the web app | the [Firebase CLI](https://firebase.google.com/docs/cli) |

```sh
git clone git@github.com:apuder/TRS-80.git
```

Android Studio opens the root directory as it is; `local.properties` is only
needed when the SDK is somewhere the tooling cannot guess. You only need the
toolchain for the platform you are working on — `buildWebCore` skips itself when
there is no `emcc`, which is how CI builds without one.

## Android

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

The APK is split per ABI (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`), so
install the one that matches: `arm64-v8a` for anything recent, `x86_64` for the
emulator on an Intel Mac.

**Debugging.** Android Studio debugs the Kotlin and, with the NDK installed, the
C too — put a breakpoint anywhere in `app/src/main/c` and it will be hit. From a
terminal, logging is tagged by component:

```sh
adb logcat -s Trs80App PlayerInput FileManager
```

The machine runs at full speed on the Android emulator, and `adb shell input
text HELLO` reaches it through the same path a real keyboard does, which makes
input bugs reproducible without a device. Crashes and usage in the field come
back through Firebase, which needs a `firebase.xml` that is deliberately not in
the repository; see [doc/CRASH-REPORTING.md](doc/CRASH-REPORTING.md).

**Publishing.** Play takes an app bundle:

```sh
./gradlew :app:bundleRelease   # app/build/outputs/bundle/release/app-release.aab
```

It is signed when four properties are set in `~/.gradle/gradle.properties`, and
left unsigned when they are absent so that CI can still build a release:

```properties
TRS80_KEYSTORE=/path/to/keystore.jks
TRS80_KEYSTORE_PASSWORD=…
TRS80_KEY_ALIAS=…
TRS80_KEY_PASSWORD=…
```

They belong there rather than in the repository. Raise the version before
building — see [The version number](#the-version-number) — because Play refuses
a `versionCode` it has already seen.

## iOS

```sh
xcodebuild -project iosApp/TRS80.xcodeproj -scheme TRS80 \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' build
```

or open `iosApp/TRS80.xcodeproj` and press Run. The project is a window, one
view controller and nothing else: everything on screen is `Trs80ViewController`
from the shared module, so a new screen never means a change here.

Two build phases do the work that is not Xcode's. One runs
`:shared:embedAndSignAppleFrameworkForXcode`, which builds the shared framework
for whatever Xcode is building, embeds it, signs it, and syncs the Compose
resources — every font and every string — into the bundle. The other stamps the
version from `gradle.properties` into a copy of `Info.plist`.

**Debugging.** Xcode steps through the Swift, the Kotlin and the C in one
session; the shared framework carries its own symbols. If the app dies at
start-up before drawing, suspect `Info.plist` rather than the code — Compose
aborts without `CADisableMinimumFrameDurationOnPhone`, and the system ends the
app the moment the tilt keyboard asks CoreMotion for a reading without
`NSMotionUsageDescription`.

**Publishing.** In Xcode: pick *Any iOS Device* as the destination, then
**Product → Archive**, then **Distribute App → App Store Connect**. The bundle
identifier is `com.trs80app` and the app's Apple ID is `6797618793`. Raise the
version in `gradle.properties` first: the build reads it from there, and a
version typed into Xcode's General tab changes nothing about the build.

## Web

```sh
./gradlew :shared:wasmJsBrowserDevelopmentRun   # serves it on localhost
./gradlew :shared:wasmJsBrowserDistribution     # writes shared/build/dist/wasmJs
```

Compose draws to a canvas through the same Skia iOS uses, so every screen came
across without a line of UI changing. The emulator is the same C, compiled by
Emscripten to `trs80core.wasm` and reached from Kotlin through the JavaScript
module beside it.

**Debugging.** The browser's own tools — the app logs to the console and the
Kotlin ships source maps. A WebAssembly stack is bare function indices unless
the core keeps its name section, so when reading a trap, link it with
`--profiling-funcs` first.

Worth knowing before it costs you a day: WebAssembly type-checks an indirect
call, and no other target here does. A function pointer called as a signature it
does not have traps with `function signature mismatch`, reported from wherever
the call happened rather than from the cast that caused it — and it is a real
bug in the C, not something the browser is doing wrong.

**Publishing.** Firebase Hosting, project `trs-80`:

```sh
./gradlew :shared:wasmJsBrowserDistribution
firebase deploy --only hosting:web --project trs-80
```

The distribution is not in git, so the build has to come first; what ships is
whatever `shared/build/dist/wasmJs/productionExecutable` holds. There are two
hosting targets — `web` is this app, `receiver` an old Chromecast receiver in a
different project — so a deploy has to name one.

## Building and checking

The fast loop, about ten seconds, and enough for most changes:

```sh
./gradlew :shared:iosSimulatorArm64Test :shared:compileAndroidMain :app:compileDebugKotlin
```

`./gradlew build` is what CI runs: it compiles both platforms, runs lint with
`abortOnError`, and builds the release APKs. Worth running before pushing, not
on every edit. A second CI job links the shared framework for iOS on macOS,
because Kotlin/Native cannot cross-compile and JVM-only constructs fail nowhere
else.

The shared module's tests run on the iOS simulator
(`:shared:iosSimulatorArm64Test`). Tests in `commonTest` are compiled for every
platform, so keep the Android framework out of them.

## The version number

Set it in `gradle.properties`, and nowhere else:

```properties
trs80VersionName=0.99.4
trs80VersionCode=54
```

Android's manifest reads those properties. `:shared:generateBuildVersion` writes
them into a generated `BuildVersion.kt`, which is what the settings screen shows
on all three platforms. The iOS build phase stamps them into `Info.plist`.
Letting a platform answer for itself is how the iOS app spent a long time
reporting `0.1`.

For a one-off build without editing the file:
`-Ptrs80VersionName=1.2.3 -Ptrs80VersionCode=77`.

## Credits

* [sdltrs][sdltrs] and [xtrs][xtrs], the emulator this is built around
* [RetroStore](https://retrostore.org), which supplies the catalog
* [Retro Fonts][Retro Fonts] and [Font Squirrel][Font Squirrel]

[sdltrs]:http://sdltrs.sourceforge.net/
[xtrs]:http://www.tim-mann.org/xtrs.html
[Retro Fonts]:http://www.kreativekorp.com/software/fonts/index.shtml#retro
[Font Squirrel]:http://www.fontsquirrel.com/fonts/DejaVu-Sans-Mono
