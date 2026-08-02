# Missing from the port

**What this is:** everything the Android app does that the Compose Multiplatform UI does not do
yet. It is an audit, not a plan — the ordering at the end is a suggestion, and nothing here is
committed to a phase.

Taken from the Android sources rather than from memory: `EmulatorActivity`'s menu constants,
`MainActivity`'s options and drawer, `ConfigurationItemListener`, the `res/menu` and `res/xml`
resources, and what `shared/` actually contains as of this writing.

What is left is above the line; what has been done since the first audit is listed at the end, in
one place, so that this stays a list of work rather than a list of achievements.

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

## 2. The running machine

Only two of `EmulatorActivity`'s options are left.

- **Chromecast.** `CastMessageSender` and sixteen references to it. Deferred deliberately; whether
  it still works at all is unchecked.
- **Tutorial.** `MENU_OPTION_TUTORIAL` is a hint framework rather than a machine control, and it
  wants the tutorial app below.

## 3. Wider windows

The emulator and the library handle them; the rest do not.

- **The editor** and **settings** are single columns of rows with nothing to fill the width they
  would gain. The editor is the one that would benefit: the spec puts it in the library's pane so
  a disk can be changed with the list in view, which means it stops being a navigation destination
  at wide widths. Left alone deliberately — see §5 for why that is not free.
- **The screens viewer** would gain the most for the least: the pictures are wider than they are
  tall, and it still letterboxes them into whatever it is given.
- **The detail sheet** on a phone in landscape rises to a fixed inset from the top, leaving it
  nearly full height with a sliver of list showing. Wide windows do not use the sheet at all, so
  this is now only a phone-sideways problem.
- **The library on a phone in landscape** is still the portrait column, deliberately: two panes
  need height as much as width, and a phone turned sideways has 440dp of it.

## 4. Smaller gaps

- **Model 4 / 4P ROMs.** Settings lists only the two ROMs the app knows how to download, so there
  is no way to supply a Model 4 or 4P image — even though the editor offers those models as soon
  as one exists. Android has all four in `settings_with_m4.xml`.
- **Tutorial app.** Android downloads and installs it on first run
  (`InitialSetupDialogFragment`, `TUTORIAL_APP_ID`). The port fetches ROMs only.
- **Crash reporting.** `crash_dialog_*` strings and the reporting path have no equivalent.

## 5. Known rough edges in what *is* ported

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
- **The on-screen keyboard is close to invisible in the light theme.** Its keys are white at 20%
  with white labels, drawn for the dark ground they have always had; on the light ground they wash
  out almost completely. Found while testing on an iPad, which defaults to light. Predates the
  overlay work — the overlay register has its own colours and is fine.
- The editor stays a pushed screen at every width, so opening it on a tablet loses the list. Making
  it a pane is not just layout: the editor holds its draft in `remember` until Save, so it cannot
  survive being navigated away from, which is also why the blank-disk panel is a panel.
- A saved state written before the app was last reinstalled resumes with whatever disk the
  configuration names *now*, since the path in the state no longer exists. That is the right
  answer when the image is the same one under a new name, which is the case this arises in. It
  would be the wrong answer if the disk in that drive had genuinely been changed since — the
  machine would carry on reading a disk it did not save against.

---

## Suggested order

1. **Stop and Share** (§1) — small, and the overflow menu they belong in now exists.
2. **The keyboard in the light theme** (§5) — it is unusable rather than untidy, and a tablet
   defaults to light.
3. The rest, by appetite.

---

## Done since this audit was written

Kept because the list above is otherwise hard to read progress from, and because a few of these
were done differently from Android and it is worth recording that they were a choice.

- **The running machine's controls** — reset, rewind cassette, paste, sound, help. Behind the
  overflow in the emulator's bar. Android's Pause is `finish()`, so the scaffold's Back already is
  it and there is no second control saying so.
- **All five keyboard layouts** — Original and Compact as key grids, Joystick as an on-screen stick
  and fire button, Tilt as fire with the accelerometer steering, Game controller as a physical
  gamepad with nothing on screen. Tilt and the gamepad are tested as logic only; there is no
  accelerometer or controller in the simulator.
- **Making a blank disk image** — as a panel over the editor rather than Android's own screen, and
  reached from the drive it will fill rather than from a file browser. `Destination.CreateDisk`
  went with it.
- **The emulator sideways** — the picture takes the window and the keyboard lies on it as outlines,
  which is what the Android app does. Landscape also reads the per-orientation keyboard layout that
  had been stored, offered and never used since the Android app.
- **The library on a wide window** — the list caps at 380dp and the sheet becomes a permanent pane
  beside it, so browsing a 300-entry catalog no longer costs you your place. Gated on 840x600dp:
  measured rather than proportioned, because a phone in landscape is the widest-aspect thing here
  and the least suited to two panes, while a foldable's inner screen is nearly square and the best.
  Not an Android feature — Android had one column at every size.
- **Bringing an Android install's settings across** — listed here for a while as missing, wrongly.
  `LegacyImport` has been live on Android since the storage was unified: `TRS80Application` runs it
  before anything reads a configuration, and `MainActivity` tells the user if it failed. It covers
  every per-configuration key the old preference screens wrote, the configuration list, the next-id
  counter and the ROM paths; the three keys it does not carry (`conf_is_custom`, `conf_last_used`,
  `conf_store_id`) do not appear in the Android sources at all, having been added during the port.
  Disk paths survive because `resolveStoredPath` leaves an absolute legacy path alone, and on
  Android the files directory does not move.

  There was never anything for iOS to import: the legacy layout is Android SharedPreferences files,
  and there was no previous iOS app. Moving a library *between* the two platforms is a real gap and
  a different feature — an export both ends understand — and nobody has asked for it yet.
- **The saved-state crash** — a machine resuming mid-transfer read from a drive whose image could
  not be reopened, and `getc(NULL)` took the process with it. Both the crash and the stale path
  behind it are fixed; see §5 for what the fix chose.
- **Where a machine came from** — configurations record the catalog program they were installed
  from, so an entry knows which machines are its own. Not an Android feature: Android had the same
  name-matching guess and the same ways of getting it wrong.
