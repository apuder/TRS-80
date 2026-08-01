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

The app draws two of the six layouts: Original and Compact. The editor now offers only those,
derived from `keyboardFor` so the two cannot drift apart.

Still missing are the layouts themselves — Joystick as an on-screen control, and behind it the two
hardware ones: `GameController` / `GameControllerListener` for a physical gamepad, and accelerometer
tilt. External is not a gap: on Android it is never a choice either, it is what an attached
hardware keyboard makes it.

An earlier version of this file claimed choosing an undrawable layout left a machine with no
keyboard at all. That was wrong — the emulator falls back to the original keyboard. What was
actually wrong is that the editor offered choices which did nothing.

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
- The document picker, disk drag-to-swap, tap-to-focus and the screens viewer's swipe have never
  been exercised interactively — they compile and their logic is tested, but no one has driven them.

---

## Suggested order

1. **Keyboards** (§3) — or, immediately and for nothing, stop offering the four that cannot be
   drawn.
2. **Create disk** and **legacy import** (§4) — the second matters most to anyone upgrading.
3. The rest, by appetite.
