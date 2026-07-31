# TRS-80 Emulator — Modernization Plan

**Decision:** Kotlin Multiplatform with Compose Multiplatform for shared UI, keeping the existing C
emulator core. Android ships continuously throughout. iOS is *proven* early with a throwaway spike
(§5) and *shipped* last, so its **technical** unknowns are answered before anything is built on top of
them. The one unknown that cannot be retired early is App Store policy (§11) — App Review does not
rule on apps that do not exist yet — so that risk is carried deliberately, and every phase before
submission pays for itself on Android alone. The UI is ported to Compose Multiplatform first
(§7) and restyled afterwards (§9) — two phases, because otherwise a port bug and a design change are
indistinguishable — and it is fully redesigned by the end, not merely carried across.

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

The same argument, applied once more, is why the UI work is two phases rather than one: **port to
Compose Multiplatform first, restyle second** (§7, §9). Restyling while porting would make every
difference ambiguous — a screen that looks wrong could be either — and would land the design on code
that is not yet on both platforms, which is the two-passes problem again one level up.

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
| Kotlin | — | **2.4.10** | Latest stable (14 July 2026); sets the AGP ceiling above |
| Compose Multiplatform | — | **1.11.1** | Latest stable; always tracks the latest Kotlin |
| JDK | 17 (CI) / 21 (pin) | **21 everywhere** | LTS, above AGP's minimum of 17 |

Sources: [AGP releases](https://developer.android.com/build/releases/gradle-plugin),
[KMP compatibility guide](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html),
[CMP compatibility](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html).

*A correction worth keeping.* This briefly ran on Kotlin 2.2.10 and Compose Multiplatform 1.9.3, on
the belief that AGP 9 supplies its own Kotlin and the KMP module has to use it. That is wrong. AGP 9
has a *runtime dependency* on KGP 2.2.10 and uses it when nothing else is present, but putting a
newer KGP on the buildscript classpath overrides it for the whole build — which this project was
already doing, just pinned low. Nothing forced the old versions, and pinning them cost real time
before it was noticed. The table above is what the project is actually on.

The binding constraint is the one the table already states: Kotlin 2.4.x supports AGP only up to
9.1.0, so AGP stays there until Kotlin's window moves, not until AGP ships something newer.

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
4. **Then** the newest Kotlin AGP will accept, and the Java→Kotlin conversion.

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

## 6. Phase 2 — The renderer ✅ DONE

*Ships on Android. No redesign — the new renderer draws into the app exactly as it looked before,
only from a different source. The same mask now also draws on iOS, through Compose Multiplatform.*

Why this is its own phase, ahead of the redesign, is unchanged: the emulator surface is the only part
of the UI whose geometry comes from the hardware rather than a designer, and the only part whose
performance was unknown. Building it first means it can ship on Android inside the existing UI, where
a frame-rate problem cannot be confused with a design problem.

**What it turned into is not what was planned.** The plan was a Compose Multiplatform renderer over
the character-cell model, with a glyph atlas built at runtime or bake time. Instead the emulator core
rasterizes, and each host uploads one image. That answers **D1** and makes **D6** moot; see §10.

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

6. ✅ **The iOS host** draws the same mask, through Compose Multiplatform. `EmulatorScreen` in
   `commonMain` is the whole renderer UI: it samples the machine on Compose's frame clock, skips a
   frame entirely when the core reports nothing changed, and blits one image. What is
   platform-specific is only `EmulatorScreenSource` — the copy into a Skia `Bitmap` on iOS, and an
   `android.graphics.Bitmap` on Android — because turning a byte buffer into something the graphics
   stack will draw is where the two genuinely differ. Both hold the mask alpha-only and tint it at
   draw time; neither rasterizes.
7. ✅ **The cell-size arithmetic moved to `commonMain`** (`fitCellSize`), since it is the one piece
   of layout the core has to be told and both hosts need the same answer. `Hardware` now delegates
   to it.

Android still drives its own render thread rather than `EmulatorScreen`; it moves across in Phase 3,
when the surrounding UI does. The composable is written and proven now so that the redesign inherits
a renderer rather than starting one.

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

**Two Compose mistakes, both silent.** `mutableIntStateOf` without `remember` gives every
recomposition a fresh counter, so the one the frame loop increments is never the one the canvas
reads — it compiles, runs, and simply never redraws. And `Bitmap().apply { allocPixels(ImageInfo(width,
height, ...)) }` resolves `width` and `height` to the *bitmap's* own, which are zero until it is
allocated: `installPixels` then returns `true` on a 0×0 bitmap and nothing draws. Both presented
identically — a blank screen with the emulator demonstrably running — and neither was findable
without printing what the draw path actually saw.

**Skia does tint an alpha-only image.** The blank screen above was first blamed on that, and the mask
was expanded to full colour through a palette to work around it. It was not the cause: with the real
bugs fixed, `ColorFilter.tint` over an `ALPHA_8` bitmap works exactly as it does on Android, and the
expansion — 98 KB to 393 KB per frame, on the main thread — was pure waste. Worth remembering as the
general shape of the error: a plausible mechanism, adopted before the actual one was located.

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

## 7. Phase 3 — Port the current UI to Compose Multiplatform

*Ships on Android as a release nobody notices, then on iOS. No redesign: the ported screens look and
navigate exactly as they do today.*

This phase used to be the redesign. It was split in two after Phase 2, because porting and restyling
are different risks and doing them at once makes every difference ambiguous — a screen that looks
wrong could be the port or the design, and there is no way to tell which.

**The port's value is not in the composables.** It is that going to iOS forces out the whole non-UI
iOS surface — storage `actual`s, content downloading, document pickers, a navigation model that is
not Activities and Intents — which is what the old Phase 4 was mostly made of, and none of which the
restyle touches. That work gets written once, here.

Three further reasons to port before restyling:

- **The current UI is an unambiguous specification.** It is running code, so "did the port change
  behaviour?" is a question with an answer. The restyle has no specification until the design work
  lands, which makes the port the part that is blocked on nothing.
- **It ships to Play as a no-visible-change release**, which is §1.1's rule applied honestly: the
  Compose migration reaches real users before any design change is riding on it.
- **The restyle then happens once**, on multiplatform code, instead of being written Android-first
  and ported afterwards.

### 7.1 What moves to `commonMain` first

The screens are the thin part. Underneath them:

- `configuration/`, `io/` and `localstore/` — the domain. Their platform coupling is shallower than
  it looks: `java.io.File`, `android.util.Log`, `SparseArray`, and a couple of `Bitmap` uses. okio
  for files, an `expect` logger, and they are portable.
- **`FileDownloader`** off `java.net.URL` and `java.util.zip.ZipInputStream`. The archive handling
  moves to okio, which reads a ZIP as a file system. The *fetch* does **not** move to Ktor — see the
  note below.
- **Storage `actual`s for iOS** — the other half of D8. `NSUserDefaults` behind
  multiplatform-settings, and `appDataDirectory()` on the sandbox's Documents directory.
- **Navigation.** Activities and `startActivityForResult` become one composable navigation model:
  `Destination` (a sealed set of the screens the app already has), `NavigationResult` (the three
  things that travel backwards) and `Navigator` (the back stack). Done, with tests.

  **Navigation 3 is the intended renderer, and is deliberately not a dependency yet.** It is stable
  for Compose Multiplatform at 1.1.1, needs Compose ≥ 1.10 (we are on 1.11.1) and publishes both of
  our iOS targets. Its whole design is a *user-owned* back stack — an observable list the app keeps
  and `NavDisplay` renders — so the model above is the deliverable either way, and adopting the
  library is adding `NavDisplay`, `: NavKey`, and the serializer configuration Nav3 needs for state
  restoration on non-JVM targets. Adding it now would be a dependency with nothing to render and
  nothing to verify, which is how the Ktor and Compose pins went wrong (§4.2, §7.1). It goes in at
  the start of 7.2, alongside the first screen, where it can actually be run on both platforms.

**On Ktor, and on a wrong conclusion that is worth recording.** The fetch is currently an
`expect`/`actual` over `HttpURLConnection` and `NSURLSession` (`httpGetBytes`, about forty lines
each) rather than Ktor. That was first justified by "Ktor does not work here", which was **wrong**,
and wrong in an instructive way: the original symptom was Ktor's klibs failing to load, which was
really the project sitting on Kotlin 2.2.10 for no reason (see §4.2). On the current stack Ktor 3.5.1
resolves and links, and constructing an `HttpClient` passes in the iOS test target.

What is *actually* still true is much narrower: linking `ktor-client-core` **together with Compose
Multiplatform** into the iOS framework crashes the throwaway spike app at start-up, inside
`ComposeUIViewController`. Dependency resolution is clean — one `kotlinx-coroutines` 1.11.0, one
`atomicfu` — and the same framework is fine under XCTest, so the spike's hand-rolled `swiftc` bundle
is itself a suspect. This is not worth chasing now: nothing needs Ktor until the RetroStore module
becomes KMP (7.2), and by Phase 4 there is a real Xcode-built app to retest against. Decide it then,
with the shared ZIP and error handling unchanged either way.

### 7.2 The screens

Configuration list, configuration editor, settings, disk creation, onboarding, and the scaffolding
around the emulator surface §6 already draws.

**The configuration list is ported**, and is what the iOS app now opens on: the cards, their two
faces, the details and the actions, reading the shared domain and drawing the saved screenshot. The
3D flip is gone — it was Android view animation with no multiplatform equivalent, and the spec
replaces the flip card anyway — so turning a card over is a crossfade, which keeps the behaviour
without inventing the design that replaces it. Actions the app cannot yet perform are not drawn at
all rather than drawn dead, so the list grows buttons as the screens behind them land.

**Sessions are saved and resumed on iOS**, so the list stops saying "not run yet" and a machine
picks up where it was left. `trs80_save_state` and `trs80_load_state` were already in the core API
and only needed exposing; the screenshot needed a PNG encoder per platform and a colour copy of the
screen, since what the renderer keeps is a colourless mask. That expansion happens once, when a
machine is put away — doing it per frame is what cost 75% of a core before (§6.2).

The session is written on the way *out* rather than in `onDispose`: the list reloads the moment the
back stack pops, so writing afterwards means it reads the previous screenshot. Backgrounding the app
does not save yet, which Android does in `onPause`.

`EmulatorScaffold` is the scaffolding around the emulator surface: a title and a way out. iOS needs
it more than Android, which at least has a system Back — without it the emulator is a place the app
can go and never leave.

**Navigation 3 is in and proven on both platforms**, which was the first step and the one worth
doing first. `Trs80App` renders the navigator's stack through a `NavDisplay`; `rememberNavigator`
hands the stack to Nav3 so it survives the app being put away. The iOS spike now reaches the
emulator *through* that path rather than around it, so the whole chain is exercised on a device and
not only by tests — the lesson from Ktor being that a passing test target proves less than it looks
(§7.1).

Two things learned in the process. Nav3's real packages are `androidx.navigation3.runtime.*`, not
the `androidx.navigation3.*` the documentation shows. And every `Destination` has to be registered
by hand in a `SavedStateConfiguration`: Kotlin/Native has no reflection to derive it from, so a
destination that is missing there fails at restore time on iOS, not at compile time. Adding a
destination means adding a line.

**RetroStore is a second UI**, and easy to overlook: `retrostore/src/main/java/org/retrostore/android/`
holds its own Activities, RecyclerView adapters and a Glide image loader — a browse screen, a detail
screen, and async image loading.

**Its client is now shared.** `RetrostoreClient`, `ApiException` and the Wire-generated messages moved
into `:shared`, and the module that is left is the Android UI, depending on `:shared` for them. The
client takes its transport as a parameter rather than building one: every call it makes is a POST of
an encoded message that comes back as another, so it needs one function, not an HTTP library. That
keeps it dependency-free, makes it testable without a network, and sidesteps Ktor — which still
cannot be linked alongside Compose in the iOS framework (§7.1).

**Both store screens are ported too**, and iOS can browse the catalogue and install from it. The
installer came with them: it is small, and once the client was shared it needed nothing from Android
but the wrapper it used to go through. What the screens did need was an image loader — every app has
cover art over the network, Android uses Glide, and `commonMain` has nothing. `RemoteImage` is the
part of Glide a list of covers actually uses: fetch once through the HTTP seam, decode off the main
thread, keep it, and treat a failure as a blank box rather than an error, because a cover is
decoration.

What is left of RetroStore is Android's own screens, which still use the Activities in
`retrostore/`, and paging — both screens fetch one page, as the Android list does.

**The keyboards are ported functionally**, which is what unblocked everything else: a machine you
cannot type at cannot be driven to a state worth saving, screenshotting or resuming. The two key
grids -- the original and the two-page compact one, 123 keys -- were *extracted* from the Android
layout XML rather than retyped, and every name verified to resolve against `KeyboardMapping` at
extraction time; a single mistyped key would type the wrong character on a machine with nothing to
check it against. `KeyboardState` holds the behaviour and is tested without a screen, because the
behaviour is the surprising part: shift **latches** rather than being held, and is released by the
next key, which is the only way one finger can type a shifted character.

**A real keyboard reaches the machine too**, which on Android is free and on iOS was not. Compose's
`onKeyEvent` does not fire on an emulator screen: focus works — the node reports itself focused — but
iOS only delivers key presses to the *first responder*, and Compose's view becomes that when a text
field takes focus. A screen with no text field never receives a key. So the presses are taken one
level up in UIKit, in a `UIViewController` that wraps the Compose one — which is where Android takes
them as well, in `dispatchKeyEvent` rather than in a view.

The joystick and tilt layouts are *not* ported. They contain no keys at all — they are gesture
surfaces with a fire button — and they are a different job from a key grid.

**Styling is deliberately not carried across.** Roughly 850 lines of custom Views across
`Key`, `FireKey`, `JoystickView` and `KeyboardManager` — and per the UI spec the styling needs real
attention, since the 43 %-opacity labels fail contrast outright. Port the *function*: the layouts are
essentially data, and the key-matrix and hit-testing logic survives intact. Do not preserve the
paint; that is Phase 5's job. Note also that the on-screen keyboards have never been verified on an
emulator during this work, because every AVD used had `hw.keyboard=yes` and took the
external-keyboard path.

### 7.3 What is deliberately left behind

Not ported — either Android-only, already condemned, or cheaper to rebuild once the restyle has
happened:

| Left out | Why |
|---|---|
| `cast/` and play-services-cast | Android-only, and wants a keep-or-drop decision of its own before Phase 5 |
| `browser/FileBrowserActivity` | Already condemned; replaced by document pickers, not ported (see 7.4) |
| `AnimationFactory` / `FlipAnimation` — the 3D flipping cards | 508 lines of Android view animation with no multiplatform equivalent, and the spec replaces the flip card anyway |
| `drag/` reordering | Comes back in Phase 5, against the new design |
| `GameController` | Android `InputDevice`. iOS has GameController.framework; a later job |
| `Tutorial` | Rebuild against the redesigned UI rather than the current one |

### 7.4 Stop storing absolute paths ✅ DONE

Configurations store the *absolute* path of every disk, cassette and ROM. That works on Android,
where `filesDir` never moves. It does not work on iOS: the app's data container is a UUID that
changes when the app is reinstalled, and Apple's guidance is explicitly not to persist paths into
it. The symptom is a configuration that survives with every value intact and every file gone — the
spike showed "You do not have a ROM image installed for Model 3" after a reinstall, with the ROM
sitting right there under a different container.

Done. Paths inside the app's own directory are stored relative to it and made absolute again on the
way out, in `ConfigurationPersistence` and `RomManager` — one seam, so nothing above them changed. A
stored path that is already absolute is passed through untouched, which is what every configuration
written before this holds and what keeps Android working; those are rewritten as relative the next
time they are set. Verified by reinstalling the iOS app into a fresh container and booting the same
configuration.

### 7.5 Finish the storage rework

`FileBrowserActivity` walks `Environment.getExternalStorageDirectory()`, which is fragile under
scoped storage and has no iOS equivalent. Delete it in favour of **platform document pickers** (D3),
and drop the now-vestigial `WRITE_EXTERNAL_STORAGE` permission from the manifest. This removes the
screen the UI spec identifies as the worst in the app, and it fixes real Android bugs today.

**Done when:** both apps run the same screens from `commonMain`, Android has no Views left, and iOS
does everything Android does.

---

## 8. Phase 4 — iOS as a shipping product

*Small, because §7 did the substance. What is left is the difference between "it runs" and "someone
can install it".*

The core build, cinterop and audio are done by §5, the renderer by §6, and the screens, storage and
downloads by §7. Remaining:

1. **A real iOS host app.** SwiftUI `App` wrapping `ComposeUIViewController`, replacing the spike's
   throwaway entry point, plus icon, launch screen and bundle configuration.
2. **App Store submission** — signing, metadata, and review. This is where the policy question in
   §11 finally gets an answer, because it is the first point at which there is an app to ask about.

**Done when:** the iOS app is on the App Store.

*Historical note:* items 1–3 of the original Phase 3 (core build, cinterop, audio) moved into Phase
1c and are done, and item 4 (the RetroStore client) was pulled all the way forward and is done. Item
6, the macOS CI runner, is done. Storage, downloads and document picking moved *forward* into Phase 3
when the port was split out, which is what left this phase so short.

---

## 9. Phase 5 — The restyle

*Both platforms at once, because by this point there is only one UI. Ships incrementally, screen by
screen.*

Everything here waits on the design work in §12, and none of it is structural — same screens, same
navigation. That is what makes it safe to do last, and what made it worth separating from the port:
a restyle landing on already-multiplatform code is written once and appears on both platforms.

- **The screens.** The UI spec's prioritized problem list is the brief.
- **The keyboards**, ported functionally in §7.2 with their styling deliberately not preserved. This
  is where they get designed properly.
- **Landscape** — the spec's biggest open problem and the one genuinely structural exception. The
  emulated picture is height-limited in landscape, leaving no room for controls, which is why the
  action bar vanishes entirely today. The designer solves it; the implementation follows.
- **Restore what 7.3 left behind** — drag-to-reorder, the tutorial — against the new design rather
  than the old one.

**Done when:** the UI spec's problem list is addressed on both platforms.

---

## 10. Decisions to make (and my recommendation)

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

## 11. Risks

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

## 12. Parallel track: design

Design work does not block on any of this and should start now — `doc/UI-SPEC.md` is the brief.
Phases 0 through 4 are invisible to users, or deliberately identical to what shipped before, and all
of them can run underneath it. Nothing before Phase 5 (§9) needs a single design decision, which is
the main practical reason the port was split out of the redesign.

The specific things the designer must resolve before Phase 5 can start in earnest:

1. **Landscape controls for the emulator** — the picture leaves no room, and today the toolbar simply
   vanishes.
2. **On-screen keyboard visual system** — contrast, press feedback, and how a keyboard coexists with
   the picture it overlays. Phase 3 ports these functionally and deliberately does not preserve their
   current styling (§7.2), so this is the one item where the port leaves a visible gap on purpose.
3. **What replaces the flip card**, which currently hides run/edit/delete/stop/share.
4. **Onboarding**, including where the (genuinely good) built-in tutorial surfaces.
5. **Whether the green-phosphor CRT identity extends to the whole app** or stays inside the emulator
   rectangle.

---

## 13. Summary

| Phase | Scope | Ships | User-visible |
|---|---|---|---|
| 0 ✅ | Clean C API, kill dead code, drop protobuf runtime | Android | No |
| 1a ✅ | JDK fix → AndroidX → AGP 9.1 / Gradle 9.5 → Kotlin 2.4 (§4.2–4.4) | Android | No |
| 1b ✅ | Java→Kotlin, KMP skeleton, RetroStore client in `commonMain` (D7) | Android | No |
| 1c ✅ | **iOS spike**: core for iOS, cinterop, audio, Z80 running in CI (§5) | No | No |
| 1d ✅ | Storage: `androidx.preference`, namespaced store, legacy import (D8) | Android | No |
| 1e ✅ | `KeyboardManager`'s mapping tables into `commonMain` | Android | No |
| 2 ✅ | **The renderer** — rasterized in C, drawn from `commonMain` on both platforms (§6) | Android | Barely — same look, new engine |
| 3 | **Port the current UI** to Compose Multiplatform; iOS storage, downloads, pickers (§7) | Android, then iOS | No — deliberately identical |
| 4 | iOS host app, App Store submission (§8) | iOS | Yes — new platform |
| 5 | **The restyle** — screens, keyboards, landscape (§9) | Both, incrementally | **Yes — the redesign** |

**Phase 1 is complete.** `commonMain` now holds the keyboard layout and mapping, `DirtyRect`,
`ScreenBuffer`, `CellMetrics`, the storage keys and the legacy import — all compiling for iOS, with
tests running on the simulator on every push. The emulator core itself builds for iOS and executes
Z80 code under CI.

**Phase 2 is complete on both platforms.** Placing it between the spike and the UI work paid off: it
is the only design-independent part of the UI and the only part whose performance was unknown, so
building it inside the existing Android UI meant a frame-rate problem could not be confused with a
design problem — and there was one, twice (§6.2). Both were caught because the app could be run and
felt, which would not have been true inside a half-built redesign. The same reasoning is why the
redesign has since been split into a port (§7) and a restyle (§9).

**Splitting Phase 3 changed what the phases are for.** The old Phase 3 was "redesign, on Android"
and the old Phase 4 was "and now make it work on iOS". That ordering wrote the shell twice — once
Android-first, once ported — and deferred every iOS unknown to the end. The port now carries the
iOS work (storage, downloads, pickers, navigation), which the restyle does not touch, so it is
written once; and the restyle lands on code that is already on both platforms. It also means the
next phase depends on no design work at all, which the old Phase 3 could not have started without.

Phases 0 and 1 are pure risk reduction and pay for themselves on Android alone. If the iOS ambition
ever stalls, stopping after Phase 5 leaves a modern, native, fully redesigned Android app — which is
the asymmetry that made this the right option in the first place.
