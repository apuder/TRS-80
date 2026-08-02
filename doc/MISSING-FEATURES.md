# Missing from the port

**What this is:** everything the Android app does that the Compose Multiplatform UI does not do
yet. It is an audit, not a plan — the ordering at the end is a suggestion, and nothing here is
committed to a phase.

Taken from the Android sources rather than from memory: `EmulatorActivity`'s menu constants,
`MainActivity`'s options and drawer, `ConfigurationItemListener`, the `res/menu` and `res/xml`
resources, and what `shared/` actually contains as of this writing.

Anything that has since been ported is dropped from this file rather than ticked off — the point
is what is left. The running machine's controls (reset, rewind, paste, sound, help) and all five
keyboard layouts were here and are now done.

---

## 1. The library

Plates offer Run and an overflow menu with Edit, Duplicate and Delete. Android offers two more
actions per machine, and both now have somewhere obvious to go.

| Feature | Android | Notes |
|---|---|---|
| Stop a running machine | `onConfigurationStop` | From the list, without entering it |
| Share a machine | `onConfigurationShare` | Shown when it has a TRS-Xray state |
| Rate / Help / Community / Share the app | drawer | `activity_main_drawer.xml` |

Settings is ported. The rest of the drawer is not.

## 2. Screens that do not exist yet

- **Create disk.** `Destination.CreateDisk` is declared and has no screen. Android has
  `CreateDiskActivity`, `CreateDiskFragment`, `res/xml/mkdisk.xml` and `menu_create_media.xml`.
- **Legacy import.** The code is in `shared` and tested, and nothing on iOS calls it. An upgrading
  user's existing configurations are never picked up.

## 3. The running machine

Only two of `EmulatorActivity`'s options are left.

- **Chromecast.** `CastMessageSender` and sixteen references to it. Deferred deliberately; whether
  it still works at all is unchecked.
- **Tutorial.** `MENU_OPTION_TUTORIAL` is a hint framework rather than a machine control, and it
  wants the tutorial app below.

## 4. Landscape

Every screen is built as a portrait column and none of them respond to the device turning. This is
not one screen's problem, so it is not filed under any of them:

- **The library** puts plates at full width above a catalog list. In landscape the plates become
  letterboxes and about two rows of catalog survive below the fold.
- **The editor** and **settings** are single columns of rows with nothing to fill the width they
  would gain.
- **The emulator** is where it matters most: the machine's picture is 4:3 and a landscape phone is
  the shape that fits it. The keyboard would have to move beside the screen rather than under it,
  which is what Android does.
- **The detail sheet** rises to a fixed inset from the top, which in landscape leaves it nearly
  full-height with a sliver of list showing.
- **The screens viewer** would gain the most for the least: the pictures are wider than they are
  tall.

One thing already exists and is dead until this lands: a configuration stores a *landscape*
keyboard layout separately from its portrait one, the editor offers both, and nothing ever reads
the landscape one. It is a setting that does nothing.

## 5. Smaller gaps

- **Model 4 / 4P ROMs.** Settings lists only the two ROMs the app knows how to download, so there
  is no way to supply a Model 4 or 4P image — even though the editor offers those models as soon
  as one exists. Android has all four in `settings_with_m4.xml`.
- **Tutorial app.** Android downloads and installs it on first run
  (`InitialSetupDialogFragment`, `TUTORIAL_APP_ID`). The port fetches ROMs only.
- **Crash reporting.** `crash_dialog_*` strings and the reporting path have no equivalent.

## 6. Known rough edges in what *is* ported

Not missing features — things that are there and imperfect.

- Never-run plates draw the machine's name in a fixed green rather than its own screen color.
- The detail sheet is not a navigation destination, so Android's Back button will not dismiss it
  once those screens are ported.
- RetroStore downloads report no progress: the store returns a whole program in one response, so
  there is nothing to report until the API offers a stream.
- Machines installed before configurations recorded their origin are linked back to their catalog
  entry by name, once, on the first catalog load. A machine whose name does not match a catalog
  program exactly — or matches a name two programs share — stays unlinked for good, and its entry
  will offer to download a fresh copy alongside it. There is no way to link one by hand.
- The document picker, disk drag-to-swap, tap-to-focus, the screens viewer's swipe and the
  joystick, tilt and gamepad inputs have never been exercised interactively — they compile and
  their logic is tested, but no one has driven them.
- **A saved emulator state can kill the app.** Entering a machine that has one makes the process
  disappear about a second later, with no crash report and no exception on stderr. Deleting the
  state file makes the same machine start normally. `trs_state_load` checks its banner and version
  and returns quietly on a mismatch, so the fault is further in — most likely one of the
  per-subsystem restores. Found while testing something else; not investigated further.

---

## Suggested order

1. **The saved-state crash** (§6) — it is the only thing here that loses a user's work.
2. **Landscape** (§4) — it touches every screen, so it gets cheaper the sooner it is done and
   dearer with every screen added before it.
3. **Create disk** and **legacy import** (§2) — the second matters most to anyone upgrading.
4. **Stop and Share** (§1) — small, and the overflow menu they belong in now exists.
5. The rest, by appetite.
