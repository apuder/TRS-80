# TRS-80 Emulator for Android — UI Specification & Redesign Brief

**Purpose of this document.** This describes every screen in the existing app, what it does, what it
shows, and how the user moves between screens — in enough detail that a designer who has never seen
the app or the source code can produce a modern UI for it. It documents the app *as it is today*,
flags the places where today's behaviour is a bug rather than a decision, and lists the constraints a
redesign must respect.

Nothing in this document is a design decision. It is the raw material for one.

Written against version 0.99 (`versionCode` 49), July 2026.

**This describes a UI that no longer exists.** The Android app it inventories was replaced by the
shared Compose UI and deleted; this is kept as the record of what that app did, which is what the
port was measured against. For what is still missing, see `doc/MISSING-FEATURES.md`; for the
sources, git history.

---

## 1. What this app is

An emulator for the **TRS-80**, a home computer Tandy/Radio Shack sold from 1977. The app runs
original 1970s/80s software — games, BASIC programs, disk operating systems — by emulating the
original hardware. The emulation core is C (a port of the `xtrs`/`sdltrs` emulator); the Android
layer is what this document covers.

The audience is retro-computing hobbyists. Many are nostalgic adults who used the original machine;
they care about authenticity, and they generally know what a "floppy disk image" is. Nothing about
the current UI is friendly to a newcomer who does not.

### 1.1 Domain glossary

A designer needs these terms, because they appear all over the UI:

| Term | What it means | Why the UI cares |
|---|---|---|
| **Model I / Model III** | Two generations of the TRS-80 machine. | The user picks one per configuration. Only these two actually work today. |
| **Model 4 / 4P** | Later machines. | Present in the data model and in dead code, but **not emulated**. Configurations using them install fine and then refuse to launch. |
| **ROM** | The machine's built-in firmware, a file the user must supply. | The app cannot legally ship these, so it downloads them. Without a ROM nothing runs. |
| **Disk image** | A file (`.dsk`) that represents the contents of a physical floppy disk. | Up to **four** can be mounted per configuration, like four floppy drives. |
| **Cassette image** | A file representing an audio cassette — the cheaper storage of the era. | One per configuration. Crucially, **a cassette has a position and must be rewound** before you can load from it, exactly like real tape. |
| **JV1 / JV3 / DMK** | Three file formats for disk images, of increasing fidelity. | Offered when creating a blank disk. DMK has extra parameters (sides, density, size). |
| **BASIC** | The programming language built into the ROM. | With no disk mounted, the machine boots straight into it. This is the default "empty" experience. |
| **BREAK / CLEAR** | Two keys on the original keyboard with no modern equivalent. | They must exist on every on-screen keyboard. BREAK interrupts a running program; CLEAR clears the screen. |
| **Configuration** | The app's core object: one emulated machine setup (model + disks + cassette + preferences). | The home screen is a list of these. Think "saved machine," not "document." |
| **RetroStore** | An online catalogue of TRS-80 software (retrostore.org). | Installing from it creates a ready-to-run configuration. This is how most users get content. |

### 1.2 The core user journey

1. Install the app. It offers to download the two ROMs plus a tutorial program.
2. Browse RetroStore, install a game. This creates a configuration with the disks already mounted.
3. Tap the configuration. The emulator launches and the game runs.
4. Type on an on-screen keyboard, or tilt the device, or use the virtual joystick.
5. Leave the emulator. State is saved automatically; a screenshot becomes the configuration's thumbnail.
6. Return later and tap the same configuration to resume exactly where it left off.

Steps 3–6 work well and are the heart of the product. Steps 1–2 and everything involving files are
where the current design struggles.

---

## 2. Platform and technical constraints

| Constraint | Value | Implication for design |
|---|---|---|
| Minimum Android | API 26 (Android 8.0) | Modern Material is fully available. |
| Target Android | API 36 (Android 16) | Edge-to-edge and predictive back are current-platform expectations. |
| Languages | English, German | All strings need translation; German runs ~30 % longer. |
| Orientation | Both, on every screen | Not a nice-to-have. The emulator behaves very differently in each. |
| Tablets | Supported | The home screen already becomes a grid at width ≥ 600 dp. |
| Current UI toolkit | Views + AppCompat; settings screens use the **deprecated** `android.preference` API | A redesign will effectively rebuild the settings screens regardless. |
| Predictive back | Explicitly disabled app-wide | Because several screens override back with custom logic. A redesign should fix the logic and re-enable it. |

### 2.1 The single hardest layout constraint

The emulated screen is **64 columns × 16 rows** of characters, with each character cell three times
taller than it is wide. The overall picture is therefore **4:3**, and it is scaled by **whole
integers only** — never stretched, never cropped, never letterboxed at a fractional scale. This is
non-negotiable; fractional scaling makes the text unreadable.

The consequence is that the emulated display's size is dictated by the window, and the keyboard has
to live with whatever is left:

- **Portrait**: the picture is width-limited. It spans the full width and occupies roughly the top
  40 % of the screen. There is real, comfortable room below it for a keyboard. This is the good case.
- **Landscape**: the picture is height-limited. It is nearly full-height and centred horizontally.
  There is **no free space left**, so the on-screen keyboard is drawn *on top of the picture*.

Everything odd about the current keyboard design — translucent keys, 43 %-opacity labels, no key
press pop-ups — follows from that landscape overlay. A redesign must have an answer for landscape.
It does not have to be the current answer.

---

## 3. Information architecture

```
Configuration list  (home — launcher activity, no splash)
├── nav drawer
│   ├── Settings ─────────────► App settings (ROM files)
│   │                            └── row tap ──► File browser
│   ├── Community ────────────► external browser (retrostore.org/community)
│   ├── Share ────────────────► system share sheet
│   ├── Rate ─────────────────► Play Store listing
│   └── Help ─────────────────► dialog
├── toolbar
│   ├── Cast ─────────────────► system Chromecast picker
│   ├── Download (conditional)► first-run ROM download
│   ├── Add configuration ────► Configuration editor (new)
│   └── Help ─────────────────► dialog
├── FAB (shopping cart) ──────► RetroStore browse
│                                └── app row ──► RetroStore detail ──► install (background)
└── configuration card
    ├── tap front ────────────► Emulator
    ├── tap info ─────────────► flips card to back
    └── back of card
        ├── Run ──────────────► Emulator
        ├── Edit ─────────────► Configuration editor
        │                        └── storage row tap ──► File browser
        │                                                 ├── "Add" ──► Disk creator
        │                                                 └── "Add" ──► system file import
        ├── Delete ───────────► confirm dialog
        ├── Stop ─────────────► confirm dialog (discards saved session)
        └── Share ────────────► uploads machine state to RetroStore
```

Two structural observations worth carrying into a redesign:

- **The file browser is a shared leaf screen** reached from five different places (cassette, four
  disk slots, two ROM slots). It always returns a single file path to whoever opened it.
- **The card back is a hidden menu.** Five of the app's most important actions — run, edit, delete,
  stop, share — exist *only* on the reverse side of a flip card, behind a 40 %-opacity ⓘ icon.

---

## 4. Current visual language (the baseline being replaced)

There is essentially **no visual design** in place today. This is a blank slate, not a brand to
preserve.

- **Theme**: stock `Theme.AppCompat.Light` with zero customisation. No brand colour, no custom
  typography, no elevation system. The emulator screen alone uses the dark variant.
- **Colour**: the only colours defined anywhere are `#FFFFFF` (key labels), `#11FFFFFF` (tutorial
  scrim) and `#808080` (secondary text). Everything else is an AppCompat default.
- **Titles**: almost every screen's title bar reads **"TRS-80"**, because most activities declare no
  label. The editor, settings, file browser, disk creator and RetroStore detail page are all titled
  "TRS-80". Only RetroStore's list screen says something else ("TRS-80 / RetroStore"). Users have no
  idea where they are.
- **Iconography**: a mix of Material vector icons and legacy bitmap PNGs, most drawn at 40 % opacity.
- **Spacing**: ad-hoc; the card list uses a 40 dp gutter, most other screens use 16–20 dp.
- **The emulated picture** is the one place with a deliberate look: dark grey (`#444444`) background
  with **green (`#00FF00`) or white** phosphor text, in authentic TRS-80 bitmap fonts. This is the
  product's identity and should be treated as sacred.

**Design opportunity:** the app has one genuinely distinctive visual asset — the green-phosphor CRT
aesthetic — and currently confines it to a rectangle inside an anonymous grey shell. The most
obvious direction for a redesign is to let that aesthetic define the whole app.

---

## 5. Screens

### 5.1 Configuration list (home)

**Purpose.** The library of saved machines. Everything starts here.

**Current layout.** Toolbar, navigation drawer, a vertical list of cards, and a floating action
button in the bottom-right corner. On screens wider than ~600 dp the list becomes a 2–4 column grid
(one column per 300 dp of width), chosen once at launch.

**The configuration card** is a two-sided flip card, 10 dp rounded corners, flat grey gradient with a
1 dp border and no shadow.

*Front:*
- Configuration name, large, top-left.
- A ⓘ info button, top-right, at 40 % opacity.
- A **screenshot** of the emulated screen as it was when the user last left, in a decorative border.
  If the configuration has never been run, this is a **black 4:3 rectangle with a ▶ start glyph**.

*Back* (reached by tapping ⓘ, revealed with a 3D horizontal flip):
- The name again.
- A six-row detail table:

  | Label | Values it can show |
  |---|---|
  | Hardware: | Model I / Model III / Model 4 / Model 4P / – |
  | Disks: | 0–4 |
  | Cassette: | rewound / not rewound |
  | Sound: | enabled / disabled |
  | Keyboard (P): | original / compact / joystick / controller / tilt / – |
  | Keyboard (L): | *(same set)* |

- A row of five icon buttons at 40 % opacity, bottom-right: **share, edit, delete, stop, run**.
  *Stop* appears only when a saved session exists; *share* only when uploadable machine state exists.
  So the row can show three, four or five icons with no labels.
- A back arrow, bottom-left, to flip to the front.

**Interactions.**
- Tap card front → run the emulator.
- Tap ⓘ → flip to the back.
- Long-press and drag → reorder. Order persists. This is only discoverable through help text.
- Swipe → nothing (deliberately disabled).
- The **very first tap on any card is swallowed by a one-time "Hint" dialog** and does not run
  anything. The user must tap again.

**States.**
- *Empty*: a single centred line of text — "Tap on the shopping icon to download apps." No
  illustration, no button, no pointer to the FAB it refers to.
- *Populated*: cards, plus an invisible 85 dp spacer at the end so the last card clears the FAB.
- *Card being dragged*: whole card drops to 50 % opacity.

**Failure paths.** Tapping run can fail with one of three bottom snackbars, each a dead end with no
corrective action attached:
- "Only Model I and Model III are supported at this time."
- "No valid ROM found. Please use Settings to set ROM."
- "Disk image not found."

**Toolbar.** Cast (always), Download (only while ROMs are missing), Add configuration, Help.

**Drawer.** A grey gradient header with the TRS-80 logo and the caption "TRS-80 Emulator", then a
flat list: Settings, Community, Share, Rate, Help.

**Problems to solve.**
1. Five primary actions are hidden behind a flip animation with no affordance.
2. The first tap on a card does nothing but dismiss a dialog.
3. The empty state names an icon rather than offering a button.
4. Reordering is undiscoverable.
5. The detail table is a debug dump — "Keyboard (P): original" means nothing to a new user.

---

### 5.2 Emulator

**Purpose.** Where the emulated machine actually runs. The product's centre of gravity.

**Layout.** A z-stack: the emulated picture at the top, the on-screen keyboard at the bottom, and a
small keyboard-switch icon floating over the keyboard's bottom-left corner at 40 % opacity.

- **Portrait**: action bar visible; picture across the top ~40 %; keyboard below it.
- **Landscape**: action bar and status bar **hidden**; picture nearly full-height; keyboard drawn
  translucently **on top of the picture**.

**Startup.** A brief centred spinner on a dark screen while character bitmaps are generated, then the
picture appears. The screen is kept awake for the whole session.

**Toolbar (portrait only).**

| Label | Behaviour |
|---|---|
| **Pause** | Exits to the home screen. Saves machine state, cassette position, and a screenshot. |
| **Reset** | Immediately reboots the emulated machine. **No confirmation.** |
| **Rewind** | Rewinds the cassette. Confirms with a "Rewinding cassette…" snackbar. |
| **Paste** | Types the clipboard's contents into the machine. Greyed out when the clipboard is empty (the enabled state does not live-update). |
| **Sound On / Sound Off** | Toggles audio. The icon shows the *current* state. |
| **TRS-80 Tutorial** | Overflow only. Starts a scripted walkthrough. |
| **Help** | Dialog. |

**In landscape none of this is reachable** — the action bar is gone, and the only on-screen controls
are the keyboard-switch icon and the system back gesture. This is the single largest usability
problem in the app, and it affects the mode most people play games in.

**Back button** behaves exactly like Pause: saves and exits. There is no confirmation and no explicit
"quit" — the model is that leaving always suspends, never discards. This is good behaviour and
should be preserved.

**Overlays.** A one-time "Hint" dialog on first launch listing the toolbar icons; per-keyboard hints
the first time each input mode is chosen; and the tutorial overlay (§5.11).

**Chromecast.** If a cast session is already running, the picture is **not** drawn on the phone at
all — a cast glyph is shown centred instead, and the phone acts purely as a keyboard while the
picture appears on the TV. Cast sessions are *started* from the home screen, not here.

**Hardware input.** An attached physical keyboard is detected automatically and replaces the
on-screen keyboard entirely (`Ctrl-B` = BREAK, `Ctrl-C` = CLEAR). Game controllers work in every
mode: D-pad and sticks drive the cursor keys with 8-way resolution, and A/B/X/Y/L1/R1 all fire.

**Absent from this screen.** There is no way to change disks, mount a cassette, edit the
configuration, or take a manual screenshot. All of that requires pausing and going back to the home
screen — which the help text explains, but the UI does not.

---

### 5.3 On-screen keyboards

Five input modes, chosen **per configuration and per orientation** (so a user can have the original
layout in portrait and a joystick in landscape). Switching is done via the small keyboard icon in the
bottom-left, which opens an unlabelled radio-button dialog.

**Universal key styling today:** square keys, 10 px rounded corners, dark translucent fill (~19 %),
grey border at 51 %, and **white labels at only 43 % opacity**. No press pop-up, no haptics, no
sound. Keys are sized to fit a fixed number per row (15, 10 or 8 depending on mode), capped at 55 dp.

**1. Original layout** — the faithful reproduction, and the default.

```
1(!) 2(") 3(#) 4($) 5(%) 6(&) 7(') 8(() 9()) 0  :(*) -(=)  BREAK
 ↑   Q  W  E  R  T  Y  U  I  O  P  @  ←  →
 ↓   A  S  D  F  G  H  J  K  L  ;(+)  ENTER   CLEAR
SHIFT  Z  X  C  V  B  N  M  ,(<) .(>) /(?)  SHIFT
                    SPACE
```
Parenthesised characters are the shifted alternates. Note the arrow keys down the left edge, BREAK
top-right, CLEAR beside ENTER, and twin SHIFT keys — all as on the real machine.

**SHIFT is sticky and one-shot**: tap it (it latches on *release*), every key with an alternate
repaints to show its shifted label and highlights, then the next key press consumes the shift.
Tapping SHIFT again cancels it.

**2. Compact layout** — for small screens. Ten keys per row instead of fifteen, so keys are ~50 %
larger. There is **no SHIFT**; instead an `Alt` key flips between a letters page and a
digits/symbols page. BREAK and CLEAR are abbreviated to `BRK` and `CLR`.

**3. Joystick** — a ~250 dp band across the bottom: a 210 dp virtual joystick puck in the
bottom-right (a grey ring with eight direction triangles and a draggable thumb, all at ~39 % opacity,
with a centre dead zone), and an **invisible full-width fire button** filling the rest of the band.
Eight directions including diagonals. Fire = SPACE.

**4. Tilt** — no visible controls whatsoever. The whole screen is an invisible fire button and the
accelerometer drives the cursor keys. Orientation locks while active.

**5. Game controller** — nothing is drawn, and even the keyboard-switch icon is hidden. The picture
has the screen to itself.

**Problems to solve.** Labels at 43 % opacity fail contrast requirements outright. There is no press
feedback of any kind. The mode switcher is a 40 %-opacity 18 dp icon in a corner. And the fundamental
tension — a keyboard that must overlay the picture in landscape — has never really been designed, only
worked around with transparency.

---

### 5.4 Configuration editor

**Purpose.** Define one emulated machine. Reached from "Add configuration" or the card's edit button.

**Current form** (a stock preference list — grey category headers, title with smaller summary beneath):

**General**
| Field | Type | Options / notes |
|---|---|---|
| Name | text | No validation. A new configuration reads **"unknown"** until named. |
| TRS-80 Model | list | **Model I**, **Model III**. That is the entire list. |

**Storage** — five rows, each opening the file browser:
| Field | Empty state | Assigned state |
|---|---|---|
| Cassette | "Default blank tape" | full absolute file path |
| Disk 1–4 | "File to use for disk N" | full absolute file path |

**Keyboard**
| Field | Options |
|---|---|
| Portrait keyboard | Original layout · Compact layout · Joystick control · Game controller · Tilt interface |
| Landscape keyboard | *(same)* |

**Miscellaneous**
| Field | Options |
|---|---|
| Character color | **Green** (default) · **White** |
| Mute | checkbox, default off |

**Save semantics — counter-intuitive and worth redesigning.** There is no Save button. **Back saves.**
The only way to discard changes is the "Cancel" icon in the toolbar. There is no unsaved-changes
prompt, ever. And backing out of a *newly created* configuration leaves a half-empty one named
"unknown" in the list, whereas Cancel deletes it.

**Hidden side effect.** Saving an edit **silently deletes the saved emulator session** for that
configuration and rewinds the cassette. A user who edits a running game loses their place with no
warning.

**Other notes.**
- No validation anywhere: no required fields, no name uniqueness, no check that a chosen file is
  actually a disk image.
- Assigned files show as raw absolute paths (`/storage/emulated/0/TRS-80/3/game.dsk`), not filenames.
- Nothing on this form changes based on the selected model. There are no conditional fields at all.
- No duplicate/clone action exists anywhere in the app.
- A known display bug: choosing "Game controller" shows the summary "Tilt interface".

---

### 5.5 App settings

Reached from the drawer. **The entire screen is two rows**, under one "ROMs" header:

| Row | Empty | Assigned |
|---|---|---|
| Model I | "ROM to use for Model I" | absolute path |
| Model III | "ROM to use for Model III" | absolute path |

Both open the file browser. There are no other app-wide settings — no theme, no sound, no storage
location, no about screen.

A four-row variant including Model 4 and 4P exists in the resources but is **never shown** (the code
that would use it is commented out). Treat Model 4/4P as out of scope unless told otherwise.

If a ROM's file disappears, the row silently reverts to the placeholder text with no explanation.

---

### 5.6 File browser

**Purpose.** Pick one file. Shared by all five storage slots and both ROM slots.

**Current layout.** A "Path: /storage/emulated/0" label at the top, a divider, then a plain list.
Each row is a **36 dp icon at 40 % opacity plus a filename** — nothing else. No size, no date, no
type, no modification time, no selection state.

**Navigation** is 1990s file-manager: a literal **`..` row** at the top of the list goes up one level.
The toolbar's back arrow does **not** go up — it cancels the whole picker. There is no breadcrumb.

**Toolbar:** Eject (clears the slot, with a confirmation dialog), Add (create a blank disk), and a
second button also labelled **"Add"** that actually imports a file from system storage. Two buttons
sharing one label is a straightforward bug.

**Selection** is single-tap-and-close. No preview, no confirm, no multi-select.

**Problems to solve.**
- **No filtering.** Every file and folder on the device is listed, including hidden files. Picking a
  disk image means finding it among everything else.
- **Sorting is broken.** Folders are gathered first and then the whole list is re-sorted by full
  path, which intermixes files and folders in case-sensitive ASCII order (so `Zebra` precedes `apple`).
- **No loading, empty, or error state at all.** A permission-denied folder and an empty folder both
  render as a bare `..` row. Since the app now targets API 36 with scoped storage, permission
  failures are likely and currently invisible.
- Import failures crash the app; import successes show no confirmation.

This screen would benefit more from a rethink than any other in the app. Consider whether it should
exist at all, versus delegating to the system document picker with a media-type filter.

---

### 5.7 Disk creator

**Purpose.** Create a new blank floppy disk image. Reached only from the file browser's "+" button;
the new file lands in whatever folder the browser was showing.

**Current form** (again a stock preference list):

| Field | Type | Options |
|---|---|---|
| Name | text | Must match `[-_.A-Za-z0-9]+`. `.dsk` is appended automatically. |
| Disk format | list | **JV1** (default) · JV3 · DMK |
| — *DMK Parameters* — | | *all four greyed out unless format is DMK* |
| Number of sides | list | 1 (default) · 2 |
| Density | list | Single (default) · Double |
| Size | list | 5 inch (default) · 8 inch |
| Ignore density flag | checkbox | off |

**Primary action** is a toolbar icon labelled "Create" that is **dimmed until the name is valid** —
the only signal the user gets about the naming rule. Typing an illegal character shows a snackbar and
**silently reverts the field**.

There is no explanation anywhere of what JV1/JV3/DMK mean or which to pick, no resulting capacity
shown, and no inline validation messaging. Everything is a transient snackbar.

---

### 5.8 RetroStore (browse + detail)

**Purpose.** The content store. In practice this is how users get software, so it is the most
important acquisition surface in the app — and the least designed.

**Browse screen.** A single-column list with pull-to-refresh and a refresh toolbar button. Each row
is a card: a 100 dp thumbnail on the left, then title (20 sp thin), a **two-line truncated
description**, author bottom-left and "Version 1.2" bottom-right. Tapping anywhere opens the detail
page.

- **There is no search.** None. The backend supports queries; the UI does not expose them.
- **There is no paging.** Exactly the first **100 apps** are fetched, once, with no "load more" and
  no indication the list is truncated.
- Only the *first* screenshot is used, and release year is dropped entirely although the API returns it.
- **No empty state** — zero results renders as a blank white screen, indistinguishable from loading.
- **Network errors** show a toast containing the raw exception text, in English, untranslated.
- Thumbnails have no placeholder and no error state; an app with no screenshot shows a blank hole.

**Detail page.** Titled "TRS-80" (not the app's name). Shows the thumbnail, name, version, author, a
"Description" heading, the scrollable description, and one full-width **Install** button. There is no
screenshot gallery, no year, no model, no size, no share, and no indication of whether the app is
already installed.

**Install flow — the weakest part of the app.**
1. Tap Install → a confirmation dialog: *Do you want to install "Frogger" now?* / **Yes, Install** / **No**.
2. Confirm → **the dialog closes and nothing visible happens.** The user stays on the detail page.
3. There is **no progress indicator of any kind** while potentially several megabytes download.
4. On success, a toast appears — often over a different screen by then.
5. **On failure, nothing happens at all.** An error string exists in the resources but is never shown.
6. Installing the same app twice silently creates a duplicate configuration.

**What "install" does:** downloads the app's media, creates a new configuration named after the app,
mounts up to four disk images and one cassette into it, and adds it to the home list. Apps requiring
Model 4/4P install successfully and then fail to launch.

---

### 5.9 First-run experience

1. App opens to the **empty state** ("Tap on the shopping icon to download apps.").
2. An **"Initial Setup"** dialog appears immediately: *Do you want to download the necessary TRS-80
   ROM-images from well-known Internet locations?* with OK / Cancel.
3. OK → a **non-cancellable spinner** reading "Downloading (1/3)…", "(2/3)…", "(3/3)…". No filenames,
   no percentage, no size, no cancel. It downloads two ROMs and the tutorial program.
4. On success the dialog closes and one card appears — "TRS-80 Tutorial" — with a black placeholder
   thumbnail. On failure: a snackbar, "Not all ROMs could be downloaded!"
5. Cancelling leaves the user at the empty state with a Download icon in the toolbar as the only way
   back into setup. The dialog never reappears on its own.
6. The **first tap on any card** is consumed by a one-time "Hint" dialog explaining that tapping runs
   the emulator and long-pressing reorders.

There is no onboarding, no explanation of what a configuration is, and no first-run guidance toward
RetroStore beyond the one line of empty-state text. A user who cancels at step 3 is effectively stuck.

---

### 5.10 Tutorial overlay

Launched from the emulator's overflow menu, and **only** works if the running configuration is
literally named "TRS-80 Tutorial" (otherwise it shows a dialog telling the user to go download it).

It replaces the keyboard with a bottom panel over a 7 % white scrim, showing the command about to be
typed (in the TRS-80 font, 30 dp), a one-line explanation, a ✕ to cancel, and a button reading
**"Next (1/8)"** through **"Next (8/8)"**. Pressing Next auto-types the command into the machine at
180 ms per keystroke.

The eight steps walk through: listing a disk directory, entering BASIC, writing and running a
"HELLO WORLD" program, saving it to cassette and to disk, exiting BASIC, and listing the directory
again to see the saved file.

This is genuinely good content trapped in an awkward mechanism — it is hidden in an overflow menu,
gated on a magic configuration name, and never surfaced during onboarding where it would do the most
good.

---

## 6. Cross-cutting inventories

### 6.1 Every dialog in the app

| Screen | Title | Message | Buttons |
|---|---|---|---|
| Home | Initial Setup | Do you want to download the necessary TRS-80 ROM-images from well-known Internet locations? | OK / Cancel |
| Home | TRS-80 | Do you want to delete configuration 'X'? | OK / Cancel |
| Home | TRS-80 | Do you want to stop emulation for configuration 'X'? | OK / Cancel |
| Home | Help: Configurations | *long help text* | OK |
| Home | Hint | Tapping a configuration runs the emulator. Long-press to reorder… | Got it! |
| Home | *(none)* | Downloading (n/3)… | *(none — not cancellable)* |
| Emulator | Hint | Use the virtual keyboard… lists toolbar icons | Got it! / TRS-80 Tutorial |
| Emulator | Hint | *(per keyboard mode: joystick, tilt, external)* | Got it! |
| Emulator | Help: Emulator | *long help text* | OK |
| Emulator | *(none)* | *keyboard type radio list* | *(none)* |
| Editor | Help: Edit Configuration | *long help text* | OK |
| Settings | Help: Settings | *long help text* | OK |
| File browser | TRS-80 | Do you want to eject the currently mounted file? | OK / Cancel |
| RetroStore | *(none)* | Do you want to install "X" now? | Yes, Install / No |

Note how much of the app's actual instruction lives in five separate "Help:" dialogs full of prose.
That is a strong signal the interface is not explaining itself.

### 6.2 State coverage matrix

| Screen | Loading | Empty | Error |
|---|---|---|---|
| Home | — | one line of text | snackbars |
| Emulator | spinner | n/a | none (silently exits) |
| Editor | — | n/a | none |
| Settings | — | n/a | none |
| File browser | **none** | **none** | **none** (crashes on import failure) |
| Disk creator | **none** | n/a | snackbars only |
| RetroStore list | spinner | **none** | raw exception in a toast |
| RetroStore detail | — | n/a | silently closes |
| Install | **none** | n/a | **none** |

Every cell marked in bold is a gap a redesign should fill.

---

## 7. Redesign opportunities, in priority order

1. **Landscape emulator has no controls at all.** No toolbar, no pause, no reset, no sound toggle —
   in the orientation most games are played in. Highest-impact problem in the app.
2. **The install flow gives no feedback.** No progress, no failure message, silent duplicates. This
   is the main content-acquisition path.
3. **Five primary actions are hidden on the back of a flip card**, behind a 40 %-opacity icon, with
   no labels.
4. **RetroStore has no search and a hard 100-app cap.** The backend supports both; only the UI doesn't.
5. **The file browser** lists every file on the device with no filter, broken sort, no metadata, and
   no error states — while now running under scoped storage, where failures are likely and invisible.
6. **Editor save semantics are backwards** (back = save, no unsaved-changes prompt) and editing
   silently destroys a saved session.
7. **Contrast and touch feedback on the keyboards.** 43 %-opacity labels, no press states, no haptics.
8. **Onboarding barely exists**, and the good tutorial content is buried behind an overflow menu and
   a magic configuration name.
9. **Every screen is titled "TRS-80."** Users cannot tell where they are.
10. **The visual identity is unused.** Green-phosphor CRT is a strong, distinctive look confined to
    one rectangle inside a stock grey shell.

---

## 8. Constraints the redesign must respect

**Cannot change:**
- The 64×16 character grid, the 4:3 aspect ratio, and integer-only scaling of the emulated picture.
- The authentic TRS-80 fonts and the green/white-on-dark phosphor rendering.
- BREAK and CLEAR must be reachable on every keyboard mode; so must all four arrow keys, and the
  arrow-left key doubles as backspace.
- Four disk slots plus one cassette slot per configuration — this mirrors the real hardware.
- Cassette rewind must remain an explicit, visible user action; it is not an implementation detail.
- Leaving the emulator suspends and resumes; it must never silently discard a session.

**Should be preserved as behaviour, even if redesigned in form:**
- Per-orientation keyboard choice (a user genuinely may want a joystick in landscape and a keyboard
  in portrait).
- Automatic screenshot-as-thumbnail when leaving the emulator. It is the app's nicest touch.
- Physical keyboard and game controller support, including auto-detection.

**Open questions for the product owner** — a designer will need answers:
- Are Model 4 / 4P ever going to be supported? They are half-present throughout the data model.
- Should the app keep its own file browser, or delegate to the system document picker?
- Is Chromecast support still worth designing around?
- Should RetroStore become the primary home screen, rather than a destination behind a FAB?

---

## 9. Reference

Screenshots of the current app are in `doc/screenshots/`. The app is on Google Play as
`org.puder.trs80`. RetroStore is at [retrostore.org](https://retrostore.org).
