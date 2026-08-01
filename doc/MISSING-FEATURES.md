# Missing from the port

**What this is:** everything the Android app does that the Compose Multiplatform UI does not do
yet. It is an audit, not a plan — the ordering at the end is a suggestion, and nothing here is
committed to a phase.

Taken from the Android sources rather than from memory: `EmulatorActivity`'s menu constants,
`MainActivity`'s options and drawer, `ConfigurationItemListener`, the `res/menu` and `res/xml`
resources, and what `shared/` actually contains as of this writing.

---

## 1. The running machine

Mostly done. The controls live behind the overflow in the emulator's bar.

| Feature | Android | Port | Notes |
|---|---|---|---|
| Pause / resume | `MENU_OPTION_PAUSE` | ✓ | Android's Pause is `finish()`; Back already is it |
| Reset | `MENU_OPTION_RESET` | ✓ | |
| Rewind cassette | `MENU_OPTION_REWIND` | ✓ | |
| Paste | `MENU_OPTION_PASTE` | ✓ | Says so when the clipboard is empty |
| Sound on/off while running | `MENU_OPTION_SOUND_ON/OFF` | ✓ | Session only; the editor is where it persists |
| Help | `MENU_OPTION_HELP` | ✓ | Rewritten for this version, and translated |
| Tutorial | `MENU_OPTION_TUTORIAL` | — | A hint framework, not a machine control; wants the tutorial app of §5 |
| Chromecast | `CastMessageSender`, 16 refs | — | Deferred deliberately; whether it still works is unchecked |

## 2. The library

Plates offer Run and an overflow to the editor. Android offers more per machine.

| Feature | Android | Port | Notes |
|---|---|---|---|
| Stop a running machine | `onConfigurationStop` | — | From the list, without entering it |
| Share a machine | `onConfigurationShare` | — | Shown when it has a TRS-Xray state |
| Rate / Help / Community / Share the app | drawer | — | `activity_main_drawer.xml` |

Settings is ported. The rest of the drawer is not.

## 3. Keyboards

Done. All five choosable layouts work: Original and Compact as key grids, Joystick as an on-screen
stick and fire button, Tilt as fire with the accelerometer steering, and Game controller as a
physical gamepad with nothing on screen. External is not a gap: on Android it is never a choice
either, it is what an attached hardware keyboard makes it.

Two of those are unexercised in practice. There is no accelerometer in the simulator and no
gamepad attached to it, so tilt and the gamepad have been verified only as far as their logic,
which is tested, and their wiring, which compiles and starts cleanly.

## 4. Screens that do not exist yet

- **Create disk.** `Destination.CreateDisk` is declared and has no screen. Android has
  `CreateDiskActivity`, `CreateDiskFragment`, `res/xml/mkdisk.xml` and `menu_create_media.xml`.
- **Legacy import.** The code is in `shared` and tested, and nothing on iOS calls it. An upgrading
  user's existing configurations are never picked up.

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
- The document picker, disk drag-to-swap, tap-to-focus, the screens viewer's swipe and the new
  joystick, tilt and gamepad inputs have never been exercised interactively — they compile and
  their logic is tested, but no one has driven them.
- **A saved emulator state can kill the app.** Entering a machine that has one makes the process
  disappear about a second later, with no crash report and no exception on stderr. Deleting the
  state file makes the same machine start normally. `trs_state_load` checks its banner and version
  and returns quietly on a mismatch, so the fault is further in — most likely one of the
  per-subsystem restores. Found while testing something else; not investigated further.

---

## Suggested order

1. **Create disk** and **legacy import** (§4) — the second matters most to anyone upgrading.
2. The rest, by appetite.
