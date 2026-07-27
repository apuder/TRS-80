# TRS-80 Emulator — Modernization Plan

**Decision:** Kotlin Multiplatform with Compose Multiplatform for shared UI, keeping the existing C
emulator core. Android ships continuously throughout; iOS is enabled once the shared layer exists.
The UI is fully redesigned as part of the work, not ported.

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
| Protobuf | **271,000 vendored LOC** to serialize one 84-line state dump | Replace with nanopb (already vendored) |
| AndroidX | `com.android.support:*:33.0.0` substituted by AGP to **AndroidX 1.0.0** (2018), `enableJetifier` still on | Full dependency refresh needed |
| RetroStore | Client is a **JVM-only JAR** from a private Maven repo | **Blocks iOS** — must be reimplemented |
| Storage | Raw filesystem paths, external storage, custom file browser | Already fragile under scoped storage; unworkable on iOS |
| Hi-res graphics | Grafyx / HRG modes hit an unimplemented stub and `longjmp` out | Latent crash, inherited by any port |

---

## 3. Phase 0 — Clean the native boundary

*Android-only. Ships. No user-visible change. This is the de-risking phase and should happen first
regardless of anything else.*

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

### 4.2 Refresh the dependency stack

This is overdue independent of the port:

| Current | Replace with |
|---|---|
| `com.android.support:*:33.0.0` → AndroidX **1.0.0** | Current AndroidX + Material 3 |
| `android.enableJetifier=true` | Remove |
| `android.preference` (deprecated) | Compose settings screens (Phase 2) |
| `AsyncTask`, `ProgressDialog` | Coroutines, Compose |
| EventBus 3.0.0 (reflection-based) | `StateFlow` |
| Glide 3.8.0 | Coil |
| Guava 27 (Android flavor) | Kotlin stdlib |

Removing Guava and EventBus also removes the reflection dependencies that currently make R8
minification unsafe — which is what the Play Console deobfuscation notice was pointing at.

### 4.3 Stand up the KMP module and move domain logic into `commonMain`

Create `shared/` with the three source sets even though only Android builds. Move, with
`expect`/`actual` only where genuinely platform-bound (file I/O, preferences):

- `Configuration`, `ConfigurationManager`, `ConfigurationPersistence`, `ConfigurationBackup`
- `EmulatorState`, `RomManager`
- `KeyboardManager`'s mapping tables and the keymap currently in `res/xml/keymap_us.xml`
- `Hardware`'s cell-metric arithmetic and `DirtyRect` — both are pure algorithms

**Done when:** the Android app runs entirely on `shared/`, still ships, and `commonMain` has no
Android imports.

---

## 5. Phase 2 — The new UI, in `commonMain`

*Android-only initially. Ships incrementally. This is the bulk of the work and where the redesign lands.*

Build the redesigned screens as Compose Multiplatform composables in `commonMain`, replacing the old
Views one screen at a time via Compose/View interop. Order them so the riskiest thing comes first.

### 5.1 The emulator surface (do this first)

Everything else is a conventional app; this is the part with no precedent. It is also the part that
proves the architecture, so it should not be last.

- **Renderer**: a Compose `Canvas` reading the shared 2 KB buffer at 60 fps, drawing glyphs from an
  atlas of `ImageBitmap`s. `DirtyRect` carries over as pure Kotlin. Benchmark early — worst case is
  1024 draws per frame today, and if that is a problem the fix is a batched atlas draw, not a
  redesign.
- **Glyph rasterization**: currently `Canvas.drawText` into 256 `Bitmap`s from bundled TTFs. Compose
  has equivalent text APIs in common code; alternatively bake the atlas at build time. Decide early
  (see D6).
- **Keyboards**: the five input modes become composables. This is a rewrite regardless, and per the
  UI spec it needs real design attention — the current 43 %-opacity labels fail contrast outright.
- **Landscape**: the spec's biggest open problem. The emulated picture is height-limited in
  landscape, leaving no room for controls, which is why the action bar disappears entirely today.
  The designer needs to solve this; the implementation follows.

### 5.2 The shell

Configuration list, editor, settings, RetroStore browse/detail, onboarding. Conventional Compose
work, driven by the design. The UI spec's prioritized problem list is the brief.

### 5.3 Rework storage while you are here

The current model — raw absolute paths into external storage, browsed with a custom file browser —
is already fragile under scoped storage and does not translate to iOS at all. Replace with an
app-scoped library plus **platform document pickers** for import (see D3). This is a prerequisite
for iOS, not a nicety, and it fixes real Android bugs today.

**Done when:** the redesigned Android app is fully Compose, shipping from `commonMain`, with no
Views left.

---

## 6. Phase 3 — iOS enablement

*No new UI work if Phase 2 was done in `commonMain`. This phase is plumbing.*

1. **Build `core/` for iOS.** CMake with an iOS toolchain producing a static library, packaged as an
   XCFramework. The `CMakeLists.txt` needs six lines guarded (`find_library(log)`, `OpenSLES`, and
   their `target_link_libraries` entries) plus the C++ flags currently injected by Gradle.
2. **cinterop bindings.** A `.def` file over `trs80_core.h` — 13 functions, no marshalling design work.
3. **iOS audio sink.** Implement the ten-line interface on AudioQueue or AVAudioEngine. Match the
   existing contract: 44.1 kHz, mono, S16LE, pull-style, 1024-byte buffers.
4. **Reimplement the RetroStore client in `commonMain`.** *This is the critical path.* The current
   client is a JVM-only JAR, so nothing about RetroStore works on iOS until it is replaced — Ktor
   plus protobuf is the obvious shape. Bonus: it is the only artifact from the private Maven repo, so
   this also removes the `allowInsecureProtocol` workaround added during the API 36 work.
5. **iOS host app.** SwiftUI `App` wrapping `ComposeUIViewController`, plus native document picking.
6. **CI.** Add a macOS runner for the iOS build.

**Done when:** the iOS app runs a game end to end.

---

## 7. Decisions to make (and my recommendation)

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

**D5 — Protobuf → nanopb.** *Recommend:* yes, in Phase 0.

**D6 — Font pipeline.** Rasterize TTFs at runtime in `commonMain`, or bake a glyph atlas at build
time. *Recommend:* decide during the Phase 2 renderer spike; build-time baking is simpler and the
fonts never change.

---

## 8. Risks

**RetroStore is the hidden iOS blocker.** It is easy to plan the whole port around the emulator and
discover late that content acquisition does not exist on iOS. Schedule it early in Phase 3, or pull
it forward into Phase 2 — replacing the JAR benefits Android immediately too.

**App Store review.** Apple has allowed retro *console* emulators since 2024; a home-computer
emulator is adjacent but not squarely covered. Separately, the RetroStore backend uses raw sockets
over cleartext HTTP, which trips App Transport Security. **Resolve both before investing in the iOS
UI**, not after — this is the cheapest possible time to discover a blocker.

**Hi-res graphics crash.** Grafyx/HRG `longjmp` out of the CPU loop. Not caused by the port, but it
will look like a port regression the first time someone hits it.

**Scope creep between redesign and port.** Two large efforts at once. The mitigation is the shipping
discipline in §1.1: if a phase cannot ship, it is too big.

---

## 9. Parallel track: design

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

## 10. Summary

| Phase | Scope | Ships | User-visible |
|---|---|---|---|
| 0 | Clean C API, kill dead code, nanopb | Android | No |
| 1 | Kotlin, AndroidX refresh, KMP skeleton | Android | No |
| 2 | Redesigned UI in `commonMain` | Android, incrementally | **Yes — the redesign** |
| 3 | iOS core build, audio, RetroStore client, host app | iOS beta | Yes — new platform |

Phases 0 and 1 are pure risk reduction and pay for themselves on Android alone. If the iOS ambition
ever stalls, stopping after Phase 2 leaves a modern, native, fully redesigned Android app — which is
the asymmetry that made this the right option in the first place.
