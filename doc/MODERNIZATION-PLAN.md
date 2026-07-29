# TRS-80 Emulator — Modernization Plan

**Decision:** Kotlin Multiplatform with Compose Multiplatform for shared UI, keeping the existing C
emulator core. Android ships continuously throughout. iOS is *proven* early with a throwaway spike
(§5) and *shipped* last, so its unknowns are answered before anything is built on top of them. The UI
is fully redesigned as part of the work, not ported.

This document says what has to happen, in what order, and what has to be decided along the way.
It is grounded in an audit of the current code — see `doc/UI-SPEC.md` for the UI inventory.

---

## 1. Target architecture

```
trs80/
├── core/                    C emulator, no platform coupling
│   ├── include/trs80_core.h the entire host-facing API (~13 functions)
│   └── src/                 z80.c, trs_disk.c, … (unchanged)
│
├── shared/                  Kotlin Multiplatform module
│   ├── commonMain/          domain + emulator session + renderer + keyboards + ALL UI (Compose)
│   ├── androidMain/         JNI binding, storage, Cast
│   └── iosMain/             cinterop binding, storage, audio
│
├── androidApp/              thin host: one Activity, manifest, Play packaging
└── iosApp/                  thin host: SwiftUI App wrapping ComposeUIViewController
```

The goal is that `shared/commonMain` holds essentially the whole product, and the two app modules are
a few hundred lines each.

### 1.1 Three principles that shape everything below

**Every phase ends shippable to Play.** No long-lived branch. Compose interoperates with Views, so
the redesign lands screen by screen while the old screens keep working.

*One deliberate exception:* the iOS spike (§5) does not ship, because its entire purpose is to be
thrown away. It buys answers, not features. Everything else holds to the rule — and the one piece of
the spike that is *not* throwaway, the renderer, ships on Android as part of Phase 2.

**Write the new UI in `commonMain` from day one — even before iOS exists.** This is the single most
important sequencing decision in the plan. Building the redesign as Android-only Compose and porting
it later would mean doing the UI work twice. Building it in `commonMain` with `expect`/`actual` seams
for platform bits costs very little extra *if planned up front*, and is expensive to retrofit.

**The C core is the asset; treat its API as a product.** It has run for years and encodes a lot of
hardware knowledge. Do not rewrite it. Give it a clean C API and never touch it again.

---

## 2. What the audit found (the starting position)

| Area | Finding | Consequence |
|---|---|---|
| Emulator core | ~23,000 LOC C, of which only **~590 lines in 4 files** are Android-coupled | Port is mechanical, not a rewrite |
| Native boundary | 13 coarse functions; screen is a **2 KB shared buffer**, zero copies per frame; audio is 100 % native | Interop is a non-issue on any toolkit |
| Audio | OpenSL ES behind a **10-line interface** (`opensl.h`) | iOS needs one new implementation, nothing else |
| Rendering | Glyph rasterization + blitting live in Java (`Hardware`, `RenderThread`, `DirtyRect` ≈ 330 LOC) | Must be rewritten — once, in `commonMain` |
| Protobuf | **271,000 vendored LOC** to serialize one 84-line state dump | *(Phase 0: removed — `libxtrs.so` 12.5 MB → 2.8 MB per ABI)* |
| AndroidX | `com.android.support:*:33.0.0` substituted by AGP to **AndroidX 1.0.0** (2018), `enableJetifier` still on | Full dependency refresh needed — see §4.2 |
| RetroStore | Client is a **JVM-only JAR** from a private Maven repo, and the app uses exactly **four** of its methods | **Blocks iOS** — retire and reimplement, see D7 |
| Storage | App data is already app-scoped (`filesDir`); SharedPreferences bound to the preference UI; the **import browser** walks external storage | Smaller than it looks — see D8 |
| Hi-res graphics | Grafyx / HRG modes hit an unimplemented stub and `longjmp` out | Latent crash, inherited by any port |

---

## 3. Phase 0 — Clean the native boundary ✅ DONE

*Merged to master July 2026. CI green, and verified by running the app on an emulator: the Z80 core
executed and rendered through the new buffer ownership, audio initialized through the renamed sink,
and the state file written by the new encoder was validated by official protoc.*

Two deviations from the plan below, both deliberate:

- **The xray state dump is encoded by hand, not by nanopb.** Only nanopb's *decoder* is vendored
  here, so the swap would have meant adding the encoder plus a generator toolchain plus callback
  plumbing for the 64 KB memory images. The message is three types and seventeen fields, so the wire
  format is written directly instead — about 90 lines, no new dependencies.
- **`trs_chars.c` / `blit.c` are still compiled.** Removing them requires guarding `bitmap_init`,
  which is currently compiled unguarded. Left as a small follow-up; it is a size optimization only.

The C core is now down to two Android-coupled files, both of which are *supposed* to be: `native.c`
(the JNI adapter) and `audio_opensl.c` (the Android audio backend).

### 3.1 Extract a real C API

Replace `app/src/main/c/native.c` (384 lines of JNI) with `core/include/trs80_core.h`:

- Pass configuration as a **struct**, not by reading Java static fields via `GetStaticFieldID`.
  All eight inputs are already just ints and path strings.
- Expose the screen as `const uint8_t* trs80_screen_buffer(void)` instead of
  `GetDirectBufferAddress`. Both JNI and cinterop can map that pointer with zero copies, preserving
  today's design exactly.
- Take **host callbacks as function pointers** (log, not-implemented, audio fill) instead of JNI
  upcalls.
- Plain `const char*` instead of 17 `GetStringUTFChars`/`Release` pairs.

Then reimplement the current JNI layer as a ~100-line shim in `androidMain` that calls this API. The
app keeps working identically, which is how you know the API is right.

### 3.2 Rename the `ANDROID` conditional

There are **36 `#ifdef ANDROID` sites across 14 files**, and most are not Android-specific at all —
they are "I am a library, not a process" concerns: the nine `trs_*_init()` re-init functions,
headless video, and a handful of genuine platform quirks. Rename to `TRS80_EMBEDDED`. Without this,
an iOS build silently takes the SDL rendering paths and shadows the shared screen pointer.

### 3.3 Make audio pluggable

The sink interface already exists and is ten lines. Formalize it, keep OpenSL ES on Android for now
(consider Oboe later — orthogonal to this work), and leave a clean seam for the iOS implementation.

### 3.4 Cut dead weight and fix latent bugs

Delete: `AudioHttpServer.java` plus the `simple-http` dependency (never wired up), `Memory.java`
(zero references), the `xlog` upcall (never called from C), and `trs_chars.c`/`blit.c` from the CMake
target (consumers are all behind `#ifndef ANDROID`).

Replace protobuf with **nanopb** for the TRS-Xray state dump. This removes ~271,000 lines from the
tree and is worth far more for an iOS static library than it is today.

Fix while you are in there: `isRunning` is a plain non-atomic int written by one thread and read by
another; `screen_init()` does `sizeof` on a pointer and clears 8 bytes instead of 2048;
`XTRS.java` computes a `screenBufferSize` of 1024 and then allocates 2048.

**Done when:** the app is byte-for-byte equivalent in behavior, CI is green, and `core/` compiles
standalone with no Android headers.

---

## 4. Phase 1 — Kotlin, modern dependencies, KMP skeleton

*Android-only. Ships. Still no user-visible change.*

### 4.1 Convert Java to Kotlin

Roughly 60 files, IDE-assisted, module by module. Mechanical but do it before restructuring — it
makes everything after this easier.

### 4.2 Version targets

**Do not chase the latest AGP.** AGP 9.3.0 is current (July 2026) and requires Gradle 9.5.0, but
Kotlin 2.4.0 supports AGP only up to **9.1.0**. Because the endgame is KMP, *Kotlin's* compatibility
window is the binding constraint, not AGP's newest release.

| Component | Phase 0 state | Target | Why that number |
|---|---|---|---|
| Gradle | 8.13 | **9.5.0** | Kotlin 2.4's maximum; satisfies AGP 9.x |
| AGP | 8.13.2 | **9.1.0** | Kotlin 2.4's maximum — *not* 9.3.0 |
| Kotlin | — | **2.4.0** | Latest stable (22 July 2026) |
| Compose Multiplatform | — | **1.11.1** | Needs Kotlin ≥ 2.1; tracks latest Kotlin |
| JDK | 17 (CI) / 21 (pin) | **21 everywhere** | LTS, above AGP's minimum of 17 |

Sources: [AGP releases](https://developer.android.com/build/releases/gradle-plugin),
[KMP compatibility guide](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html),
[CMP compatibility](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html).

**Fix the JDK ambiguity first.** There are currently three answers in three places: CI provisions
Temurin 17, `gradle/gradle-daemon-jvm.properties` demands JetBrains JDK 21, and Gradle papers over
the gap by auto-provisioning through foojay using a feature it labels incubating. Three steps:

1. Delete `gradle/gradle-daemon-jvm.properties` — generated by the Studio upgrade, pins a *vendor*
   for no benefit.
2. Move CI to `java-version: '21'` (Temurin).
3. Add a Java toolchain (`jvmToolchain(21)`) so the compiling JDK is pinned independently of the one
   launching Gradle.

### 4.3 Refresh the dependency stack

Six current dependencies are JVM- or Android-only and therefore **cannot move into `commonMain`**.
Every replacement is also a reduction:

| Current | Replace with | Note |
|---|---|---|
| Guava 27.0.1 (2018) | Kotlin nullable types | `Optional.orNull()` is used throughout the config code |
| commons-io | okio | |
| EventBus 3.0.0 | `StateFlow` / `SharedFlow` | Also removes the reflection that makes R8 unsafe |
| Glide 3.8.0 | Coil 3 | Coil 3 is multiplatform |
| protobuf-lite 3.0.0 (Java) | Wire or kotlinx-serialization-protobuf | |
| `org.retrostore:retrostore-client` | Ktor client (see D7) | Also retires `maven.haberling.net` |

Play Services Cast stays Android-only by design — that belongs behind an `expect`/`actual`, not a
replacement.

Separately, the whole `com.android.support:*:33.0.0` set must become real AndroidX coordinates. Those
fake version numbers resolve to **AndroidX 1.0.0 from 2018** through jetifier substitution, so this
is less an upgrade than adopting AndroidX for the first time. Also drop `android.enableJetifier` and
flip `nonTransitiveRClass` / `nonFinalResIds` off their legacy `false` settings.

Housekeeping found along the way: `com.google.gms:google-services:4.3.15` sits on the root classpath
but **the plugin is never applied anywhere** — dead weight from an old Firebase experiment. And
introduce a `gradle/libs.versions.toml` version catalog before the KMP restructure; sharing versions
across modules and source sets gets unpleasant without one.

### 4.4 Upgrade order — one rule that matters

1. **Fix the JDK ambiguity** (§4.2). Isolated, low risk.
2. **Migrate to real AndroidX**, drop jetifier, flip the R-class flags.
3. **Then** AGP → 9.1.0 and Gradle → 9.5.0.
4. **Then** Kotlin 2.4.0 and the Java→Kotlin conversion.

Step 2 must come **before** step 3. The AndroidX migration is the riskiest change here, and you want
a working, testable app between "new libraries" and "new build system" rather than both failing at
once.

### 4.5 Stand up the KMP module and move domain logic into `commonMain`

Create `shared/` with the three source sets even though only Android builds. Move, with
`expect`/`actual` only where genuinely platform-bound (file I/O, preferences).

*Module created July 2026, with the Android and three iOS targets building; the iOS compile runs in
CI on every push.* Migrated so far:

- ✅ `KeyboardLayout`, `KeyMap` — plain data, moved as-is
- ✅ `DirtyRect`, plus the `ScreenBuffer` and `CellMetrics` types extracted to free it of
  `android.graphics.Rect`, `java.nio.ByteBuffer` and `Hardware`

Still to move:

- `Hardware`'s cell-metric arithmetic — the seam already exists as `Hardware.cellMetrics`; the glyph
  rasterization (Bitmap/Canvas/Paint) stays on Android until the Phase 2 renderer replaces it
- `KeyboardManager`'s mapping tables and the keymap currently in `res/xml/keymap_us.xml`
- `Configuration`, `ConfigurationManager`, `ConfigurationPersistence`, `ConfigurationBackup`,
  `ConfigurationImpl`, `EmulatorState`, `RomManager`, `FileManager` — **all blocked on D8**, which is
  the next thing to settle

**Done when:** the Android app runs entirely on `shared/`, still ships, and `commonMain` has no
Android imports.

---

## 5. Phase 1c — Prove iOS end to end (the spike)

*Runs in parallel with D8. Not shipped. Deliberately throwaway except for the renderer, which is
not.*

This phase did not exist in the original plan, which put all of iOS after the redesign. That
ordering is wrong, for two reasons.

**The emulator surface is design-independent.** 64×16 character cells, 4:3, integer-only scaling —
that geometry comes from the hardware, not from a designer. It is also the one part of the UI with
no precedent in the codebase and the only part whose performance is genuinely unknown. Building it
before the design lands costs nothing against the redesign and removes the largest unknown from it.

**Everything else about iOS is UI-independent plumbing.** The core build, cinterop and the audio
sink do not care what the app looks like. Deferring them until after the redesign means discovering
any iOS blocker at the most expensive possible moment, having already built a UI for a platform that
might reject it.

So: get a game running on iOS with **no shell at all**. Bundle a ROM and a disk image as resources
and hardcode one configuration. No configuration list, no settings, no RetroStore browser.

**Critically, this bypasses storage entirely**, so it does not wait on D8 — the two run in parallel.

The `TRS80_EMBEDDED` rename (§3.2) is **already done** — the only `ANDROID` conditionals left in the
native tree are two sites inside vendored SDL. That was the prerequisite that would otherwise have
made an iOS build silently take the SDL rendering paths.

1. **Build `core/` for iOS.** CMake with an iOS toolchain producing a static library packaged as an
   XCFramework. Only ~5 lines of `CMakeLists.txt` are Android-coupled (`find_library(log)`,
   `OpenSLES`, and their `target_link_libraries` entries), plus the C++ flags Gradle injects today.
   `audio_opensl.c` and `native.c` are the two files that do not come along: the first is replaced
   by the iOS sink below, the second by cinterop.
   Add it to CI — the iOS job currently compiles only the *Kotlin* shared module, so the C core has
   never been built for iOS at all.
2. **cinterop bindings.** A `.def` file over `trs80_core.h` — 13 functions, no marshalling design.
3. **iOS audio sink.** Implement the ten-line interface on AudioQueue or AVAudioEngine. Match the
   existing contract: 44.1 kHz, mono, S16LE, pull-style, 1024-byte buffers.
4. **The renderer, in `commonMain`.** A Compose `Canvas` reading the shared 2 KB buffer at 60 fps,
   drawing glyphs from an atlas of `ImageBitmap`s. `DirtyRect` is already in `commonMain` and
   carries over unchanged. Benchmark on a real device: worst case is 1024 draws per frame, and if
   that will not hold, the first fix is a batched atlas draw.
5. **Glyph rasterization.** Currently `Canvas.drawText` into 256 `Bitmap`s from bundled TTFs.
   Compose has equivalent text APIs in common code; alternatively bake the atlas at build time —
   see D6, which this phase is the right place to settle.
6. **Ask Apple.** See §9: policy is now the *only* remaining App Store unknown, and answering it
   costs nothing at this stage.

**Done when:** a game runs on an iOS device, with measured frame timings, and the App Store question
has an answer.

This phase also makes **D1** decidable on evidence instead of guesswork. If 1024 draws per frame
will not hold 60 fps on iOS, moving rasterization into C stops being hypothetical.

---

## 6. Phase 2 — The new UI, in `commonMain`

*Android-only initially. Ships incrementally. This is the bulk of the work and where the redesign lands.*

Build the redesigned screens as Compose Multiplatform composables in `commonMain`, replacing the old
Views one screen at a time via Compose/View interop.

The renderer and glyph pipeline are **already done by Phase 1c** — that was the riskiest piece and
the reason the original plan said to do it first. What is left here is everything that genuinely
depends on the design.

### 6.1 The emulator surface: input and layout

- **Keyboards**: the five input modes become composables. This is a rewrite regardless, and per the
  UI spec it needs real design attention — the current 43 %-opacity labels fail contrast outright.
  Note that the on-screen keyboards have never been verified on an emulator during this work, since
  every AVD used had `hw.keyboard=yes` and therefore took the external-keyboard path.
- **Landscape**: the spec's biggest open problem. The emulated picture is height-limited in
  landscape, leaving no room for controls, which is why the action bar disappears entirely today.
  The designer needs to solve this; the implementation follows.

### 6.2 The shell

Configuration list, editor, settings, RetroStore browse/detail, onboarding. Conventional Compose
work, driven by the design. The UI spec's prioritized problem list is the brief.

### 6.3 Finish the storage rework

Most of this lands earlier, under D8 — the app's own data is already app-scoped in `filesDir`, so
the storage problem is narrower than the original audit implied. What remains for Phase 2 is the
**import path**: `FileBrowserActivity` walks `Environment.getExternalStorageDirectory()`, which is
fragile under scoped storage and has no iOS equivalent.

Delete it in favour of **platform document pickers** (see D3), and drop the now-vestigial
`WRITE_EXTERNAL_STORAGE` permission from the manifest. This removes the screen the UI spec identifies
as the worst in the app, and it fixes real Android bugs today.

**Done when:** the redesigned Android app is fully Compose, shipping from `commonMain`, with no
Views left.

---

## 7. Phase 3 — iOS as a shipping product

*Small, because Phase 1c already proved the hard parts and Phase 2 wrote the UI in `commonMain`.
What is left is the difference between "a game runs" and "an app someone can install".*

The core build, cinterop, the audio sink and the renderer are all done by then (§5). Remaining:

1. **iOS host app.** SwiftUI `App` wrapping `ComposeUIViewController`. The spike's throwaway
   entry point is replaced by a real one.
2. **Storage `actual`s for iOS** — the other half of D8. The spike sidestepped this by bundling
   files; a real app needs the key-value store on `NSUserDefaults` and `appDataDirectory()` on the
   sandbox's Documents directory.
3. **Content acquisition.** `FileDownloader` is still JVM-only (`java.net.URL`,
   `java.util.zip.ZipInputStream`) and has to move to Ktor plus okio. The RetroStore client is
   already multiplatform-ready (D7).
4. **Native document picking**, per D3.
5. **App Store submission** proper — signing, review, metadata. The policy question was already
   answered in Phase 1c.

**Done when:** the iOS app is installable and does everything the Android app does.

*Historical note:* items 1–3 of the original Phase 3 (core build, cinterop, audio) moved into
Phase 1c, and item 4 (the RetroStore client) was pulled all the way forward and is done. Item 6, the
macOS CI runner, is done — though today it compiles only the Kotlin shared module, and Phase 1c
extends it to the C core.

---

## 8. Decisions to make (and my recommendation)

**D1 — Screen model: keep character cells, or move rendering into C?**
Today C exposes one byte per character cell and the host rasterizes glyphs. The alternative is for C
to render pixels into a shared RGBA framebuffer, making every host trivially simple *and* fixing the
hi-res graphics modes for free, since those are inherently pixel-based.
*Recommend:* keep character cells for the port — it works today and under Compose Multiplatform the
renderer is written once anyway. Revisit only if you decide to support Grafyx/HRG.

**D2 — Chromecast.** Android-only SDK, and the audio half of it is already dead code.
*Recommend:* keep it Android-only behind an `expect` that no-ops on iOS, or drop it. Do not port it.

**D3 — File access.** *Recommend:* delete the custom file browser. Use platform document pickers for
import and keep an app-scoped library of disk/cassette images. This is the right answer on both
platforms and removes a screen that the UI spec identifies as the worst in the app.

**D4 — Model 4 / 4P.** The C core emulates them and supports 80×24; the UI has never exposed them,
and half-finished support is scattered through the code. This changes both the renderer and the
configuration form, so **decide before the design is finalized**, not after.

**D5 — Protobuf.** *Resolved in Phase 0:* replaced with a hand-written encoder rather than nanopb —
see §3.

**D6 — Font pipeline.** Rasterize TTFs at runtime in `commonMain`, or bake a glyph atlas at build
time. *Recommend:* decide during the Phase 1c renderer work (§5); build-time baking is simpler and the
fonts never change.

**D7 — The RetroStore JVM SDK** ✅ DONE (July 2026) (`github.com/shaeberling/retrostore-jvm-sdk`). The app depends on it
as `org.retrostore:retrostore-client:0.2.13`, published to `maven.haberling.net`.

What it is: ~66 KB of Java, of which **37 KB is CLI test harnesses** (`TestCli`, `TestCliOldApi`) —
the real client is roughly 26 KB. Last pushed **August 2023**. No license. Dependencies are Guava 20
(2016), Gson 2.8.0 (2016) and protobuf-lite 3.0.0. And the app calls exactly **four** of its methods:
`fetchApps`, `fetchMediaImages`, `getApp`, `uploadState`.

*Resolved:* retired and reimplemented with Wire (protobuf) plus Ktor. The full `ApiProtos.proto`
now lives in `retrostore/src/main/proto/` and Wire generates Kotlin from it; the client is five
suspend functions. This removed `maven.haberling.net`, the `allowInsecureProtocol` workaround,
Guava, and protobuf-lite from the module. Original recommendation follows.

*Recommended at the time:* **retire it and reimplement those four calls in `commonMain`** with Ktor
plus Wire/kotlinx-serialization over the existing `.proto`. Vendoring the SDK source into this repo would
kill the private Maven dependency but leaves it Java and JVM-only — relocating the iOS blocker rather
than removing it, while importing Guava and Gson into a graph we are trying to shrink.

Retiring it deletes, in one move: the private Maven repo and the `allowInsecureProtocol` workaround,
Guava 20, Gson, protobuf-lite 3.0.0, and the hardest iOS blocker in this plan. The result will likely
be *smaller* than the Java original, since it skips the CLI harnesses and Guava-era boilerplate.

Keep `ApiProtos.proto` as the artifact that matters — it is the real contract, and it already exists
twice (that repo, plus a nanopb-generated copy in this repo's native tree). Consolidate on one copy
here and generate from it. **The native C RetroStore client stays as-is**: it serves the emulated
machine's TRS-IO card, is decode-only, and is a genuinely different consumer — do not unify them.

Archive the SDK repo rather than deleting it, and give it a `LICENSE` if it stays public (TRS-80's
sources are Apache 2.0; that repo currently has no license at all).

**D8 — Storage.** The last structural decision before the domain classes can move to `commonMain`.
`ConfigurationPersistence`, `ConfigurationManager`, `RomManager`, `EmulatorState`, `ConfigurationImpl`
and `FileManager` all queue behind it.

Storage here is **two** problems, and conflating them is the main trap.

#### D8a — Files: leave the native core on real paths

The C core does its own file I/O. `trs80_init()` takes `romFile`, `cassette` and `disk0..disk3` as
path strings; `saveState`/`loadState` take a path; C opens them with `fopen`. So the shared file API
cannot be opaque — it has to be able to *produce* a path.

That is not an iOS problem. **iOS is POSIX**: `fopen`/`fseek`/`fwrite` work against the app sandbox
exactly as on Android, and the native code compiles unchanged. What shared code owes the core is one
`expect fun appDataDirectory(): Path`. With okio — **already on the classpath at 3.17.0 via Wire** —
that is roughly ten lines per platform, and the file half of storage is done.

The tempting alternative is to change the native API to exchange **blobs** instead of paths. Rejected,
because the four file kinds are not alike:

| File | Native I/O pattern | Blob-able? |
|---|---|---|
| ROM | `trs_load_rom()`, read once | Trivially |
| Save state | 14 reads/writes, **0 seeks** — pure streaming | Trivially |
| Blank disk creation | sequential write | Trivially |
| Cassette | 5 seeks, read+write, position tracked across the session | Moderate |
| **Floppy / hard disk images** | **28 seeks, interleaved read/write, `ftruncate`, `FILE*` held open all session** | **No** |

`trs_disk.c` is 3,931 lines and does not do whole-file I/O. It opens the image `rb+`, holds the handle,
and seeks around writing individual sectors and JV3 sector-ID records as the guest OS writes to disk.

The decisive detail is `fflush` after sector writes (`trs_disk.c:1909, 2340, 2756, 2775`). That is
deliberate write-through: when the emulated machine writes to disk, it reaches storage immediately.
Blobs would mean holding a dirty image in memory and writing back at chosen moments — on a mobile OS
that kills backgrounded apps without warning, converting **"always durable"** into *"durable if we
picked the right moments"*. That is a real data-safety regression on users' disk images, bought in
exchange for portability to a platform with no filesystem (web/wasm), which is not on the roadmap.

If this is ever revisited, the right shape is a **callback VFS** (à la SQLite's VFS or SDL_RWops) —
function pointers replacing `fopen/fseek/fread/fwrite/fclose` — not whole-file blobs. That preserves
the random-access model and write-through semantics, and swaps one I/O layer instead of restructuring
4,000 lines of format handling. The risk there is not crashes but *silent* JV1/JV3/DMK corruption:
an image that simply stops mounting.

*Decided:* native keeps paths. Shared code provides `appDataDirectory()` + okio. The save state — zero
seeks — can convert to a blob in isolation later if RetroStore upload ever wants the bytes in memory.

#### D8b — Key-value: one namespaced store, plus a one-time migration

The migration surface is small: ~7 global keys (`conf_first_time`, `conf_ran_new_assistant`, four ROM
paths, `KEY_NEXT_ID`, `KEY_CONFIGURATIONS`) plus 11 keys per configuration. A user with a dozen configs
has under 150 flat scalar values. **No file data moves** — ROMs, disk images, cassettes and save states
stay where they are; only the pointers migrate. `RomManager.hasRom` already drops stale ROM paths and
re-downloads, so the one path-shaped risk has a self-healing case in place.

Because a migration is cheap and safe, format compatibility should *not* drive the design. And the
current shape has a real defect: `ConfigurationPersistence` uses **one preference file per
configuration** (`PREF_NAME_PREFIX + configId`). On iOS that maps to `NSUserDefaults(suiteName:)`,
which works, but suites exist for app groups — not for spawning dozens of per-entity stores. That
shape only ever made sense because `PreferenceFragment` wanted a SharedPreferences *name*.

*Recommend:* **one store with namespaced keys** (`config.7.name`) via multiplatform-settings, plus a
one-time migration. Fixes the iOS mapping, drops the string-encoded ints in the same pass (see below),
keeps the synchronous API, adds no heavy dependency.

*Not* SQLite/Room/SQLDelight: ~150 flat scalars do not justify a schema and a migration framework.
Revisit only if configurations grow real structure.

**Sync or suspend:** keep key-value **synchronous** (tiny, memory-cached on both platforms — and going
suspend would touch every call site); use **suspend** for file I/O, which is genuinely slow.

**The string-encoded ints.** `ConfigurationPersistence.getInt` reads `getString(key, "").toIntOrNull()`
— ints are stored as strings because `ListPreference`/`EditTextPreference` only write strings. A UI
artifact leaked into the data format. It cannot be cleaned up while the preference screens still write
the old format, so it is fixed *by* the migration, not before it.

#### D8c — Ordering: `androidx.preference` unlocks this early

`ConfigurationPersistence` hands out `android.preference.Preference` objects via
`PreferenceFinder`/`PreferenceProvider`, and five UI files are `PreferenceFragment`s bound to the same
keys. Storage and settings UI are two-way bound by key name — which looks like it forces the storage
work to wait for the Compose rewrite in Phase 2.

It does not. The deprecated `android.preference` has no way to redirect where a screen stores values,
but **`androidx.preference` has `PreferenceDataStore`**, which exists precisely to back preference
screens with something other than SharedPreferences. The code already carries five
`@Suppress("DEPRECATION")` notes saying the androidx migration is a separate change.

*Sequence:*

1. `android.preference` → `androidx.preference` (independently useful; removes five deprecation suppressions).
2. New namespaced store behind `PreferenceDataStore`, with the one-time migration. Existing screens
   read and write it directly.
3. Domain classes move to `commonMain`.
4. Compose replaces the preference screens in Phase 2, on its own schedule — and the string-encoded
   ints die with them.

This turns one hard ordering dependency into two independent steps, and unblocks §4.5 without waiting
on design work.

---

## 9. Risks

**RetroStore was the hidden iOS blocker** ✅ *retired.* It was easy to plan the whole port around the
emulator and discover late that content acquisition did not exist on iOS. Pulled forward and done —
see D7. `FileDownloader` is the remaining JVM-only piece of content acquisition (§7 item 3).

**App Store review** — now the *only* remaining App Store unknown, and it is policy, not code.
Apple has allowed retro *console* emulators since 2024; a home-computer emulator is adjacent but not
squarely covered. **Answer it in Phase 1c**, before any iOS UI exists — that is the cheapest possible
moment to discover a blocker.

*Correction:* earlier versions of this plan also listed cleartext HTTP tripping App Transport
Security. **That is no longer true.** Retiring the JVM SDK (D7) replaced the raw-socket client with
Ktor against `https://retrostore.org/api/%s`, and the initial ROM downloads are HTTPS from GitHub.
The app's own networking is TLS throughout. (The *native* C RetroStore client still serves the
emulated machine's TRS-IO card, but that is the guest talking to a virtual peripheral, not the app's
network stack, and it is decode-only.)

**Hi-res graphics crash.** Grafyx/HRG `longjmp` out of the CPU loop. Not caused by the port, but it
will look like a port regression the first time someone hits it.

**Scope creep between redesign and port.** Two large efforts at once. The mitigation is the shipping
discipline in §1.1: if a phase cannot ship, it is too big.

---

## 10. Parallel track: design

Design work does not block on any of this and should start now — `doc/UI-SPEC.md` is the brief.
Phases 0 and 1 are invisible to users and can run underneath it. The specific things the designer
must resolve before Phase 2 can start in earnest:

1. **Landscape controls for the emulator** — the picture leaves no room, and today the toolbar simply
   vanishes.
2. **On-screen keyboard visual system** — contrast, press feedback, and how a keyboard coexists with
   the picture it overlays.
3. **What replaces the flip card**, which currently hides run/edit/delete/stop/share.
4. **Onboarding**, including where the (genuinely good) built-in tutorial surfaces.
5. **Whether the green-phosphor CRT identity extends to the whole app** or stays inside the emulator
   rectangle.

---

## 11. Summary

| Phase | Scope | Ships | User-visible |
|---|---|---|---|
| 0 ✅ | Clean C API, kill dead code, drop protobuf runtime | Android | No |
| 1a ✅ | JDK fix → AndroidX → AGP 9.1 / Gradle 9.5 → Kotlin 2.4 (§4.2–4.4) | Android | No |
| 1b ✅ | Java→Kotlin, KMP skeleton, RetroStore client in `commonMain` (D7) | Android | No |
| 1c | **iOS spike**: core for iOS, cinterop, audio, renderer, ask Apple (§5) | No | No |
| 1d | Storage: `androidx.preference` ✅ → namespaced store + migration (D8) | Android | No |
| 2 | Redesigned UI in `commonMain` — shell, keyboards, landscape | Android, incrementally | **Yes — the redesign** |
| 3 | iOS host app, iOS storage, `FileDownloader`, submission | iOS beta | Yes — new platform |

**1c and 1d are independent and can run in parallel** — the spike bundles its files and so needs no
storage, and the storage work needs no iOS. 1c is the higher-value one to do first: it answers the
questions that could invalidate the whole iOS ambition, at the point where they cost least.

Phases 0 and 1 are pure risk reduction and pay for themselves on Android alone. If the iOS ambition
ever stalls, stopping after Phase 2 leaves a modern, native, fully redesigned Android app — which is
the asymmetry that made this the right option in the first place.
