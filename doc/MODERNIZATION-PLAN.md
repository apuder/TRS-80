# TRS-80 Emulator — Modernization Plan

**Decision:** Kotlin Multiplatform with Compose Multiplatform for shared UI, keeping the existing C
emulator core. Android ships continuously throughout. iOS is *proven* early with a throwaway spike
(§5) and *shipped* last, so its **technical** unknowns are answered before anything is built on top of
them. The one unknown that cannot be retired early is App Store policy (§10) — App Review does not
rule on apps that do not exist yet — so that risk is carried deliberately, and every phase before
submission pays for itself on Android alone. The UI is fully redesigned as part of the work, not
ported.

This document says what has to happen, in what order, and what has to be decided along the way.
It is grounded in an audit of the current code — see `doc/UI-SPEC.md` for the UI inventory.

---

## 1. Target architecture

```
trs80/
├── core/                    C emulator, no platform coupling
│   ├── include/trs80_core.h the entire host-facing API (~20 functions)
│   └── src/                 z80.c, trs_disk.c, trs80_render.c, …
│
├── shared/                  Kotlin Multiplatform module
│   ├── commonMain/          domain + emulator session + keyboards + ALL UI (Compose)
│   ├── androidMain/         JNI binding, storage, Cast
│   └── iosMain/             cinterop binding, storage, audio
│
├── androidApp/              thin host: one Activity, manifest, Play packaging
└── iosApp/                  thin host: SwiftUI App wrapping ComposeUIViewController
```

The goal is that `shared/commonMain` holds essentially the whole product, and the two app modules are
a few hundred lines each.

One thing moved the other way. **The renderer ended up in C, not in `commonMain`** — the core
rasterizes the screen and each host uploads one image (§6). That was not the plan, and the reason is
that the authentic character generator ROMs were already vendored in `trs_chars.c`, so the expensive
part of doing it there was already written. Each host is left with a blit rather than a renderer,
which is less shared Kotlin but less code overall.

### 1.1 Three principles that shape everything below

**Every phase ends shippable to Play.** No long-lived branch. Compose interoperates with Views, so
the redesign lands screen by screen while the old screens keep working.

*One deliberate exception:* the iOS spike (§5) does not ship, because its entire purpose is to be
thrown away. It buys answers, not features. Everything else holds to the rule, including the
renderer (§6), which ships on Android inside the current UI before any redesign exists.

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
| Rendering | Glyph rasterization + blitting live in Java (`Hardware`, `RenderThread`, `DirtyRect` ≈ 330 LOC) | *(Phase 2: moved into C, which already had the character ROMs — see §6)* |
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

## 4. Phase 1 — Kotlin, modern dependencies, KMP skeleton ✅ DONE

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

Also done, though not a move: `EmulatorCore` and `NativeScreenBuffer` in `iosMain` (§5), which give
the `ScreenBuffer` interface its second implementation and prove the abstraction holds.

Still to move, and this is all that is left of Phase 1:

- **1d** — `Configuration`, `ConfigurationManager`, `ConfigurationPersistence`,
  `ConfigurationBackup`, `ConfigurationImpl`, `EmulatorState`, `RomManager`, `FileManager`. **All
  blocked on D8**, which is why the store and its migration are the whole of 1d: one decision
  releases eight classes.
- **1e** — `KeyboardManager`'s mapping tables and the keymap currently in `res/xml/keymap_us.xml`.
  Small, and *not* blocked on storage, so it can go before, after or alongside 1d.

`Hardware` is no longer listed here. Phase 2 (§6) deleted its glyph rasterization outright — the core
does that now — leaving only the cell-metric arithmetic, which is what the core is told. Moving what
remains into `commonMain` is small and no longer urgent.

**Done when:** the Android app runs entirely on `shared/`, still ships, and `commonMain` has no
Android imports.

---

## 5. Phase 1c — Prove iOS end to end (the spike) ✅ DONE

*Runs in parallel with 1d. Does not ship. Wholly throwaway — nothing here is an artifact we keep.*

This phase did not exist in the original plan, which put all of iOS after the redesign. That is the
wrong order: the core build, cinterop and the audio sink do not care what the app looks like, so
deferring them means discovering any iOS blocker at the most expensive possible moment, with a UI
already built for a platform that might reject it.

**The one question this phase answers is "does the emulator work on iOS at all?"** Not "does it look
right" — that is §6's job. Keeping the two apart matters, because a renderer performance problem and
an iOS viability problem would otherwise be indistinguishable.

**It bypasses storage entirely**, so it does not wait on 1d — the two run in parallel.

The `TRS80_EMBEDDED` rename (§3.2) was **already done** in Phase 0 — the only `ANDROID` conditionals
left in the native tree are two sites inside vendored SDL. That was the prerequisite that would
otherwise have made an iOS build silently take the SDL rendering paths.

1. ✅ **Build the core for iOS.** Done. `CMakeLists.txt` now produces either the Android shared
   library (with `native.c` and `audio_opensl.c`) or a bare static library for iOS, from one source
   list. Three things were in the way, all small: the single unconditional `add_library`,
   `<android/log.h>` in the SDL shim (duplicate reporting — the same macro already calls the
   `trs80_host` callback), and `system()` in `do_emt_system`, which iOS does not have and which is
   now compiled out and refused the way `trs_emtsafe` refuses it.
2. ✅ **cinterop bindings.** Done. A `.def` over `trs80_core.h` with a `headerFilter` so the rest of
   the native tree cannot leak into Kotlin. CMake runs once per iOS target from the build script.
   `EmulatorCore` in `iosMain` is the counterpart of `XTRS`, and `NativeScreenBuffer` implements the
   `commonMain` `ScreenBuffer` interface straight over the core's pointer — no copy, the same
   contract as the JNI direct buffer.
3. ✅ **iOS audio sink.** Done. `audio_audioqueue.c` on AudioQueue, matching `audio_opensl.c`'s
   parameters exactly. The linker asked for it precisely: with the sink absent,
   `trs80_audio_init`/`trs80_audio_shutdown` were the *only* undefined symbols in the whole core.
4. ✅ **Run it, in CI.** Done. `:shared:iosSimulatorArm64Test` executes the core on the simulator,
   covering the CMake build, cinterop, linking and execution in one step. A synthetic Z80 program
   is assembled in the test and run, so the CPU is proven to execute and to reach video RAM without
   bundling a copyrighted ROM or touching the network.
**Done when:** the emulator core demonstrably executes on iOS under CI. ✅

Note what this phase deliberately does *not* prove: that anything is drawn. Seeing the screen needs
the renderer, which is §6.

Nor does it settle App Store policy. An earlier draft of this plan put "ask Apple" here on the
argument that it was the cheapest moment to find a blocker. That argument does not survive contact
with how App Review works: Apple does not issue advisory rulings on hypothetical apps, so the
question cannot actually be answered until there is something to submit. It belongs with submission,
in §8.

---

## 6. Phase 2 — The renderer ✅ ANDROID DONE

*Ships on Android. No redesign — the new renderer draws into the app exactly as it looked before,
only from a different source.*

Why this is its own phase, ahead of the redesign, is unchanged: the emulator surface is the only part
of the UI whose geometry comes from the hardware rather than a designer, and the only part whose
performance was unknown. Building it first means it can ship on Android inside the existing UI, where
a frame-rate problem cannot be confused with a design problem.

**What it turned into is not what was planned.** The plan was a Compose Multiplatform renderer over
the character-cell model, with a glyph atlas built at runtime or bake time. Instead the emulator core
rasterizes, and each host uploads one image. That answers **D1** and makes **D6** moot; see §9.

The reason is a discovery rather than a preference: `trs_chars.c` already holds the authentic
character generator ROMs — 2,302 lines, eleven charsets — compiled into every build and, until now,
entirely unused. The expensive part of "render in C" was supposed to be writing a glyph rasterizer,
and it was already there.

### 6.1 What was built

1. ✅ **`trs80_render()`** rasterizes video RAM into an 8-bit coverage mask that the host tints and
   scales. It redraws only the cells that changed and reports whether anything did, so an idle screen
   costs neither an upload nor a draw.
2. ✅ **`trs80_set_cell_size()`.** The core rasterizes at the size the host actually draws a cell,
   not at the ROM's 8×12. This is not an optimization — see 6.2.
3. ✅ **The Android host** blits that mask once per frame, tinted, on a hardware canvas.
4. ✅ **Pacing on the display's clock**, via a `Choreographer` on the render thread's own `Looper`.
5. ✅ **`Hardware`'s glyph generation deleted** — 101 lines, both generators, the 256-entry font
   array, and the `Bitmap`/`Canvas`/`Paint`/`Typeface` work behind them. What remains is the
   arithmetic deciding cell size, which is what the core is now told.

Still to do: **draw the same mask on iOS.** `EmulatorCore.render()` and `pixelBuffer` exist and are
covered by §5's tests; nothing consumes them yet, and that needs Compose Multiplatform in the build.

### 6.2 Three things that were only learned by measuring

Each of these was got wrong first, and none was visible from reasoning about the design.

**`lockCanvas()` returns a *software* canvas.** Scaling the mask to the screen was therefore a CPU
upscale of the whole picture every frame: 18.5 ms against a 16.7 ms vsync budget, so every frame that
drew missed its deadline. Since most frames are idle, the cost landed precisely on the frames
responding to a keypress, which is where it is felt. `lockHardwareCanvas()` moved the scale to the
GPU and took the frame from 18.4–18.9 ms to 0.9–2.4 ms — from four times slower than the glyph
blitting it replaced to three times faster. Note the frame overlay had been reporting 18 ms all along
and was read as "the emulator is slow"; it took someone saying the app *felt* worse to look properly.

**Scaling the mask after the fact ruins the glyphs.** At a 14×42 cell the scale is 1.75×, and a
one-pixel stem then lands on two output pixels at five of the eight phases and one at the other
three — visibly uneven strokes with columns missing. No filtering mode fixes it: bilinear turns the
same stem into fringes of differing weight. Hence `trs80_set_cell_size()`: rasterize at the target
size, blit one to one, never resample. Two details make the result match the old renderer rather than
merely differ from the old bug — coverage is thresholded at a half rather than kept as a fraction,
which is what font hinting does and what makes every stem the same width; and it is computed in
integer arithmetic, because at 1.75× a stem's edge pixel is covered *exactly* half and floating point
put that either side of the threshold depending on the glyph.

**The block graphics needed the same treatment separately.** They are not glyphs, so fixing the glyph
path left them scaled and uneven — six, seven or eight pixels for what should be half a cell. They
are now computed at the cell size directly.

### 6.3 Measured

On a Pixel 9 Pro XL, via `dumpsys gfxinfo`:

| | |
|---|---|
| Janky frames | **0 (0.00%)** |
| 50th / 90th / 95th percentile | 5 / 5 / 5 ms |
| 99th percentile | 9 ms |
| Missed vsyncs, high-input-latency frames | 0, 0 |

Stroke widths, measured rather than eyeballed: 293 stems, every one 7 px, against a spread of 6, 7
and 8 before. The screenshots that prompted the investigation were macOS-rescaled and lossy, so
comparing them by eye could never have settled it.

---

## 7. Phase 3 — The redesign, in `commonMain`

*Android-only initially. Ships incrementally. This is the bulk of the work and where the redesign lands.*

Build the redesigned screens as Compose Multiplatform composables in `commonMain`, replacing the old
Views one screen at a time via Compose/View interop.

The renderer and glyph pipeline are **already done by Phase 2** — that was the riskiest piece and
the reason the original plan said to do it first. What is left here is everything that genuinely
depends on the design.

### 7.1 The emulator surface: input and layout

The picture itself is **already done** (§6) and needs no design input — what is left here is
everything around it.

- **Keyboards**: the five input modes become composables. This is a rewrite regardless, and per the
  UI spec it needs real design attention — the current 43 %-opacity labels fail contrast outright.
  Note that the on-screen keyboards have never been verified on an emulator during this work, since
  every AVD used had `hw.keyboard=yes` and therefore took the external-keyboard path.
- **Landscape**: the spec's biggest open problem. The emulated picture is height-limited in
  landscape, leaving no room for controls, which is why the action bar disappears entirely today.
  The designer needs to solve this; the implementation follows.

### 7.2 The shell

Configuration list, editor, settings, RetroStore browse/detail, onboarding. Conventional Compose
work, driven by the design. The UI spec's prioritized problem list is the brief.

### 7.3 Finish the storage rework

Most of this lands earlier, under D8 — the app's own data is already app-scoped in `filesDir`, so
the storage problem is narrower than the original audit implied. What remains for Phase 3 is the
**import path**: `FileBrowserActivity` walks `Environment.getExternalStorageDirectory()`, which is
fragile under scoped storage and has no iOS equivalent.

Delete it in favour of **platform document pickers** (see D3), and drop the now-vestigial
`WRITE_EXTERNAL_STORAGE` permission from the manifest. This removes the screen the UI spec identifies
as the worst in the app, and it fixes real Android bugs today.

**Done when:** the redesigned Android app is fully Compose, shipping from `commonMain`, with no
Views left.

---

## 8. Phase 4 — iOS as a shipping product

*Small, because Phase 1c already proved the hard parts, Phase 2 built the renderer and Phase 3
wrote the shell in `commonMain`.
What is left is the difference between "a game runs" and "an app someone can install".*

The core build, cinterop and the audio sink are done by §5, and the renderer by §6. Remaining:

1. **iOS host app.** SwiftUI `App` wrapping `ComposeUIViewController`. The spike's throwaway
   entry point is replaced by a real one.
2. **Storage `actual`s for iOS** — the other half of D8. The spike sidestepped this by bundling
   files; a real app needs the key-value store on `NSUserDefaults` and `appDataDirectory()` on the
   sandbox's Documents directory.
3. **Content acquisition.** `FileDownloader` is still JVM-only (`java.net.URL`,
   `java.util.zip.ZipInputStream`) and has to move to Ktor plus okio. The RetroStore client is
   already multiplatform-ready (D7).
4. **Native document picking**, per D3.
5. **App Store submission** — signing, metadata, and review. This is where the policy question in
   §10 finally gets an answer, because it is the first point at which there is an app to ask about.

**Done when:** the iOS app is installable and does everything the Android app does.

*Historical note:* items 1–3 of the original Phase 3 (core build, cinterop, audio) moved into
Phase 1c and are done, and item 4 (the RetroStore client) was pulled all the way forward and is done. Item 6, the
macOS CI runner, is done — though today it compiles only the Kotlin shared module, and Phase 1c
extends it to the C core.

---

## 9. Decisions to make (and my recommendation)

**D1 — Screen model: keep character cells, or move rendering into C?** ✅ *Resolved in Phase 2 (§6):
rendering moved into C, and the character buffer stayed.*

Both, as it turns out. The core still exposes one byte per cell — `trs80_screen_buffer()` is
unchanged, and Cast still reads it — and it *additionally* rasterizes into a coverage mask through
`trs80_render()`. Nothing was taken away, so the character model is still there for anything that
wants characters rather than pixels.

The original recommendation here was to keep the host rasterizing, on the grounds that under Compose
Multiplatform the renderer would be written once anyway. That was reversed once `trs_chars.c` turned
up: the authentic character generator ROMs were already vendored and compiled into every build, so
the expensive half of moving rendering into C did not have to be written at all.

Two of the arguments made for it beforehand did not survive contact:

*"Fewer draw calls will be faster."* Not on its own. Trading 1024 small blits for one scaled blit was
four times **slower** until the scale was moved off the CPU, which had nothing to do with draw-call
count. See §6.2.

*"The ROM is more authentic than the TrueType approximation."* Barely. The bundled fonts are
kreativekorp's *replica* TRS-80 faces, and only 4.2 % of pixels differ between the two renderers on
the same screen. This should have been checked by looking in `assets/fonts` before it was claimed.

What did hold, and is what the decision now rests on: **one rasterizer instead of one per host**, no
glyph machinery on iOS at all, and the hi-res graphics modes become representable, since a mask can
carry pixels that a character buffer cannot.

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

**D6 — Font pipeline.** ✅ *Moot, as of Phase 2 (§6).* There is no atlas to decide about: the glyphs
come from the character generator ROMs already vendored in `trs_chars.c`, and the core rasterizes
them. Neither runtime rasterization nor build-time baking is needed on either host.

The bundled TrueType faces stay, but only for the tutorial's text (`Fonts.kt`) and the on-screen
keyboard's key labels (`Key.kt`). The emulated screen no longer uses them.

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
work to wait for the Compose rewrite of those screens, which is Phase 3 (§7).

It does not. The deprecated `android.preference` has no way to redirect where a screen stores values,
but **`androidx.preference` has `PreferenceDataStore`**, which exists precisely to back preference
screens with something other than SharedPreferences. The code already carries five
`@Suppress("DEPRECATION")` notes saying the androidx migration is a separate change.

*Sequence:*

1. `android.preference` → `androidx.preference` (independently useful; removes five deprecation suppressions).
2. New namespaced store behind `PreferenceDataStore`, with the one-time migration. Existing screens
   read and write it directly.
3. Domain classes move to `commonMain`.
4. Compose replaces the preference screens in Phase 3 (§7), on its own schedule — and the
   string-encoded ints die with them.

This turns one hard ordering dependency into two independent steps, and unblocks §4.5 without waiting
on design work.

---

## 10. Risks

**RetroStore was the hidden iOS blocker** ✅ *retired.* It was easy to plan the whole port around the
emulator and discover late that content acquisition did not exist on iOS. Pulled forward and done —
see D7. `FileDownloader` is the remaining JVM-only piece of content acquisition (§8 item 3).

**App Store review** — the *only* remaining App Store unknown, and it is policy, not code. Apple has
allowed retro *console* emulators since 2024; a home-computer emulator is adjacent but not squarely
covered.

This is the one risk in this plan with **no cheap early mitigation**, and it is worth being honest
about that rather than pretending otherwise. An earlier draft scheduled "ask Apple" into Phase 1c on
the grounds that finding a blocker early is cheapest. But App Review does not rule on apps that do
not exist yet, so there is nothing to ask until §8. The risk is therefore carried, not retired — and
the mitigation is structural instead: every phase before §8 pays for itself on Android alone, so a
rejection costs the iOS host app and the iOS storage `actual`s, not the modernization.

*Correction:* earlier versions of this plan also listed cleartext HTTP tripping App Transport
Security. **That is no longer true.** Retiring the JVM SDK (D7) replaced the raw-socket client with
Ktor against `https://retrostore.org/api/%s`, and the initial ROM downloads are HTTPS from GitHub.
The app's own networking is TLS throughout. (The *native* C RetroStore client still serves the
emulated machine's TRS-IO card, but that is the guest talking to a virtual peripheral, not the app's
network stack, and it is decode-only.)

**Hi-res graphics crash.** Grafyx/HRG `longjmp` out of the CPU loop. Not caused by the port, but it
will look like a port regression the first time someone hits it.

Phase 2 makes this *fixable* without making it fixed. The modes are inherently pixel-based, which is
why a character buffer could never carry them; the core now rasterizes into a pixel mask, and
`grafyx_unscaled` is already a pixel buffer sitting in C next to it. So the remaining work is
compositing one into the other rather than inventing a representation. Still unclaimed, and still a
latent crash until someone does it.

**Scope creep between redesign and port.** Two large efforts at once. The mitigation is the shipping
discipline in §1.1: if a phase cannot ship, it is too big.

---

## 11. Parallel track: design

Design work does not block on any of this and should start now — `doc/UI-SPEC.md` is the brief.
Phases 0 and 1 are invisible to users and can run underneath it. The specific things the designer
must resolve before Phase 3 (§7) can start in earnest — the renderer in §6 needed none of it:

1. **Landscape controls for the emulator** — the picture leaves no room, and today the toolbar simply
   vanishes.
2. **On-screen keyboard visual system** — contrast, press feedback, and how a keyboard coexists with
   the picture it overlays.
3. **What replaces the flip card**, which currently hides run/edit/delete/stop/share.
4. **Onboarding**, including where the (genuinely good) built-in tutorial surfaces.
5. **Whether the green-phosphor CRT identity extends to the whole app** or stays inside the emulator
   rectangle.

---

## 12. Summary

| Phase | Scope | Ships | User-visible |
|---|---|---|---|
| 0 ✅ | Clean C API, kill dead code, drop protobuf runtime | Android | No |
| 1a ✅ | JDK fix → AndroidX → AGP 9.1 / Gradle 9.5 → Kotlin 2.4 (§4.2–4.4) | Android | No |
| 1b ✅ | Java→Kotlin, KMP skeleton, RetroStore client in `commonMain` (D7) | Android | No |
| 1c ✅ | **iOS spike**: core for iOS, cinterop, audio, Z80 running in CI (§5) | No | No |
| 1d ✅ | Storage: `androidx.preference`, namespaced store, legacy import (D8) | Android | No |
| 1e ✅ | `KeyboardManager`'s mapping tables into `commonMain` | Android | No |
| 2 | **The renderer** — rasterized in C (§6). **Android done**; iOS still to draw it | Android | Barely — same look, new engine |
| 3 | Redesigned shell, keyboards, landscape (§7) | Android, incrementally | **Yes — the redesign** |
| 4 | iOS host app, iOS storage, `FileDownloader`, submission (§8) | iOS beta | Yes — new platform |

**Phase 1 is complete.** `commonMain` now holds the keyboard layout and mapping, `DirtyRect`,
`ScreenBuffer`, `CellMetrics`, the storage keys and the legacy import — all compiling for iOS, with
tests running on the simulator on every push. The emulator core itself builds for iOS and executes
Z80 code under CI.

**Phase 2 was deliberately placed between the spike and the redesign**, and that placement paid off.
It is the only design-independent part of the UI and the only part whose performance was unknown, so
doing it inside the existing Android UI meant a frame-rate problem could not be confused with a
design problem — and there was one, twice (§6.2). Both were caught because the app could be run and
felt, which would not have been true inside a half-built redesign.

**What is left of Phase 2 is iOS drawing the mask**, which needs Compose Multiplatform in the build.
The C half and the Kotlin bindings are done and tested.

Phases 0 and 1 are pure risk reduction and pay for themselves on Android alone. If the iOS ambition
ever stalls, stopping after Phase 3 leaves a modern, native, fully redesigned Android app — which is
the asymmetry that made this the right option in the first place.
