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

Two build phases do the work that is not Xcode's. One runs
`:shared:embedAndSignAppleFrameworkForXcode`, which builds the framework for
whatever Xcode is building, embeds it, signs it, and syncs the Compose resources
— every font and every string — into the bundle. The other copies
`TRS80/Info.plist` to `$(TARGET_TEMP_DIR)/Info.plist` and writes
`CFBundleShortVersionString` and `CFBundleVersion` into the copy from
`gradle.properties`. `INFOPLIST_FILE` points at the copy, so what the phase
writes is what Xcode then processes into the app.

It stamps the copy rather than the finished bundle for a reason worth keeping:
the build system orders work by the files each task reads and writes, not by the
order of the phase list, so a phase that edits the built `Info.plist` is racing
the task that puts it there. That phase was last in the list and still lost —
`ProcessInfoPlistFile` ran after it and the build came out `0.0`, having said
`note: version 0.99.2 (51)` on the way. Naming the generated file as the phase's
output is what orders the two.

Things that are easy to lose and hard to diagnose:

- **The version is not in the Xcode project.** `MARKETING_VERSION` and
  `CURRENT_PROJECT_VERSION` are deliberately absent; typing a version into
  Xcode's General tab writes them and changes nothing about the build.
- **What the General tab writes is mostly inert.** It saves display name and
  category as `INFOPLIST_KEY_*` build settings, which only apply when
  `GENERATE_INFOPLIST_FILE` is `YES`, and here it is `NO`. Put the key in
  `TRS80/Info.plist` instead, and check it landed:
  `PlistBuddy -c 'Print :CFBundleDisplayName' <built>.app/Info.plist`.

- **`CADisableMinimumFrameDurationOnPhone` must be `true`.** Compose checks for
  it at start-up and throws if it is missing, so the app aborts before drawing.
- **`NSMotionUsageDescription` must be present**, or the system ends the app the
  moment the tilt keyboard asks CoreMotion for a reading.
- The icon is `var/icons/playstore_high_res_icon.png`, recomposed: iOS refuses an
  alpha channel and masks the corners itself, so the machine is lifted out of
  its transparent bands, scaled to 86% and set on the app's light ground.
- No disk image ships, so the first machine is a bare Model III. Drop a
  `disk_0.dsk` into the target's resources and it is used instead.

## The web app

A third host, and a whole one: `shared/src/wasmJsMain`. Compose draws to a canvas
through the same Skia iOS uses, so every screen came across without a line of UI
changing, and the emulator is the same C compiled by Emscripten.

```sh
./gradlew :shared:wasmJsBrowserDevelopmentRun   # serves it on localhost
./gradlew :shared:wasmJsBrowserDistribution     # writes shared/build/dist/wasmJs
```

The core needs `emcc` on the path. `buildWebCore` skips when there is none —
CI has no Emscripten and no browser to run the result in — and the app then
loads with everything but a machine.

Three things a browser answers differently, each in the file that says so:

- **No thread for the machine.** `runMachine` is an expect for exactly this. Here
  there is no thread: the run loop's own frame pause is an `emscripten_sleep`,
  ASYNCIFY turns that into a yield, and the machine hands the one thread a page
  has back between frames so Compose can draw.
- **A file system that is not a file system.** okio has no browser backend, so
  `BrowserFileSystem` is an in-memory one mirrored into localStorage.
- **HTTP through the page.** `httpGetBytes` is fetch; the ROMs and the catalog
  both come across, so `retrostore.org` is serving the CORS headers it needs to.

**An indirect call is type-checked here and nowhere else.** Casting a function
pointer to a different signature costs nothing on a real calling convention, and
in WebAssembly it traps — `RuntimeError: function signature mismatch`, thrown
from wherever the call happened to be rather than from the cast. One such cast
in the cassette code killed every machine at start-up. If that error appears,
look for a pointer being called as something it is not before suspecting
ASYNCIFY, whose frames are on every stack because the run loop lives in it.

To read a trap, link the core with `--profiling-funcs` so the wasm keeps its
name section; without it the stack is bare function indices.

## Hosting the web app

`https://trs-80.web.app` — Firebase Hosting, project `trs-80`.

```sh
./gradlew :shared:wasmJsBrowserDistribution
firebase deploy --only hosting:web --project trs-80
```

The distribution is not in git, so the build has to happen first; what is
deployed is whatever `shared/build/dist/wasmJs/productionExecutable` holds.

Two hosting targets, because `firebase.json` already had one: `receiver` is the
Chromecast receiver in a different project, left as it was, and `web` is this.
Deploying without naming a target would try for both.

Caching is set so that only the two content-hashed `.wasm` files — 12 MB of the
15 — are held for a year. Everything else, `trs80core.wasm` included, is
`no-cache`: those names do not change when their contents do, and a browser
holding yesterday's core against today's page is a bug nobody can clear.

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
