# Working in this repository

A TRS-80 emulator: a C core, a Kotlin Multiplatform module holding the whole app, and two thin
hosts. `shared/commonMain` is where nearly everything lives — if a change can go there, it should.
See `doc/MODERNIZATION-PLAN.md` for how it got this shape.

| Where | What |
| --- | --- |
| `shared/commonMain` | the app: every screen, the domain, the emulator session |
| `shared/androidMain`, `shared/iosMain` | only what a platform alone can answer — files, clipboard, links, input devices |
| `app/` | the Android host: the JNI binding, one activity, an Application, and the C core it builds |
| `app/src/main/c` | the emulator itself, shared by both platforms |

The Android app module is deliberately four Kotlin files. A change that adds a fifth should be
looked at twice: unless it is something only an activity or a `Context` can do, it belongs in
`shared`. The same goes for a string or a layout — what the app says lives in the shared module's
Compose resources, in both languages, because both platforms say it.

## The version number

**Set it in `gradle.properties`, and nowhere else.**

```properties
trs80VersionName=0.99
trs80VersionCode=49
```

Everything that shows or records a version reads those two lines:

| Who | How |
| --- | --- |
| Android's manifest | `app/build.gradle` reads the properties into `versionName` / `versionCode` |
| The settings screen, on both platforms | `:shared:generateBuildVersion` writes `VERSION_NAME` and `VERSION_CODE` into a generated `BuildVersion.kt`, and `appVersion()` formats them |

To release, raise both: `trs80VersionName` is what a person reads back to you, and
`trs80VersionCode` is what Play orders releases by — it must go up, and a number that has been
uploaded before is refused. Nothing else needs editing; no test hardcodes the value.

Check it with `./gradlew :shared:iosSimulatorArm64Test` — `AppVersionTest` covers the wiring rather
than the number, so it fails if the constants stop being generated but not when you bump them. For
a one-off build without editing the file: `-Ptrs80VersionName=1.2.3 -Ptrs80VersionCode=77`.

**Do not let a platform answer for itself.** Android used to read its `PackageManager` and iOS its
bundle, and they disagreed — iOS showed whatever its `Info.plist` happened to carry, which for a
long time was `0.1`, because nothing in the build ever set it. Two sources of truth for one number
is how that happens. An iOS app target, when there is one, should generate its `Info.plist` from
these properties too.

A version quoted in a document — `doc/UI-SPEC.md` opens with one — is a note about when that
document was written. It is not a place to update.

## The iOS app

`iosApp/TRS80.xcodeproj` is the app: a window, one view controller, and nothing
else. Everything on screen is `Trs80ViewController` from the shared module, so a
new screen never means a change here.

```sh
xcodebuild -project iosApp/TRS80.xcodeproj -scheme TRS80 \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' build
```

Two build phases do the work that is not Xcode's. The first runs
`:shared:embedAndSignAppleFrameworkForXcode`, which builds the framework for
whatever Xcode is building, embeds it, signs it, and syncs the Compose resources
— every font and every string — into the bundle. The last writes
`CFBundleShortVersionString` and `CFBundleVersion` from `gradle.properties`; it
has to be last, because the app's `Info.plist` does not exist until Xcode has
processed it, and signing happens afterwards.

Things that are easy to lose and hard to diagnose:

- **`CADisableMinimumFrameDurationOnPhone` must be `true`.** Compose checks for
  it at start-up and throws if it is missing, so the app aborts before drawing.
- **`NSMotionUsageDescription` must be present**, or the system ends the app the
  moment the tilt keyboard asks CoreMotion for a reading.
- The icon is `var/icons/playstore_high_res_icon.png`, recomposed: iOS refuses an
  alpha channel and masks the corners itself, so the machine is lifted out of
  its transparent bands, scaled to 86% and set on the app's light ground.
- No disk image ships, so the first machine is a bare Model III. Drop a
  `disk_0.dsk` into the target's resources and it is used instead.

## Building and checking

The fast loop, about ten seconds, and enough for most changes:

```sh
./gradlew :shared:iosSimulatorArm64Test :shared:compileAndroidMain :app:compileDebugKotlin
```

`./gradlew build` is what CI runs: it compiles both platforms, runs lint with `abortOnError`, and
builds the release APKs. Worth running before pushing, not on every edit.

The shared module's tests run on the iOS simulator (`:shared:iosSimulatorArm64Test`). Tests in
`commonTest` are compiled for both platforms, so keep the Android framework out of them — a test
that needs a real `android.graphics.Bitmap` belongs elsewhere.
