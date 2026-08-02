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

Every per-machine action Android had is now in the plate's overflow. What is left is the drawer:
**Rate**, **Help**, **Community** and **Share the app** (`activity_main_drawer.xml`). Settings was
the only part of it worth porting on its own, and it is done; these four are links out of the app
and want somewhere to live that is not a navigation drawer, since this UI has none.

## 2. The running machine

One of `EmulatorActivity`'s options is left.

- **Chromecast.** `CastMessageSender` and sixteen references to it. Deferred deliberately; whether
  it still works at all is unchecked.

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
- The TRS-Xray dump is described by two copies of one schema: Android's
  `app/src/main/proto/system_state.proto`, compiled by protobuf-lite, and
  `shared/src/commonMain/proto/trs_protos/system_state.proto`, compiled by Wire into a different
  package. One file would put two `NativeSystemState` classes in the same package on Android's
  classpath, and moving Android onto the Wire type means unpicking the protobuf plugin from the
  app that is in the store. If the dump format changes, both files change.
- Sharing a state has never been run against the live store — the upload path is exercised only as
  far as building the request.
- The document picker, disk drag-to-swap, tap-to-focus, the screens viewer's swipe and the
  joystick, tilt and gamepad inputs have never been exercised interactively — they compile and
  their logic is tested, but no one has driven them.
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

1. **The screens viewer on a wide window** (§3) — the largest gain for the least work left.
2. The rest, by appetite. Chromecast is the biggest single piece and the least certain to still
   work at all.

---

## Not gaps, though they look like ones

Recorded because both were listed here for a while and one of them cost real time. Someone reading
the Android sources will meet them again.

- **Model 4 / 4P ROM settings.** `res/xml/settings_with_m4.xml` lists all four models and is never
  loaded — `SettingsFragment` reads `R.xml.settings`, which has Model I and Model III only. Android
  never shipped this, so it is not something the port is behind on. Nor does it leave a promise
  unkept here: the editor offers a model only when `hasRom` finds an image for it, and without a
  way to supply one it never does.
- **The tutorial**, both halves — the app Android installs on first run and the hint framework in
  the emulator that needs it. Not wanted for now, by decision rather than oversight.

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
- **Stop and Share** — both in the plate's overflow. Stop discards the paused session and rewinds
  the tape, as Android's does, and asks first. Share is behind an experimental flag, found by
  tapping the version at the foot of settings ten times and then switched on there: two gates, so
  finding the door is not the same as walking through it.
- **The saved-state crash** — a machine resuming mid-transfer read from a drive whose image could
  not be reopened, and `getc(NULL)` took the process with it. Both the crash and the stale path
  behind it are fixed; see §5 for what the fix chose.
- **The machine always draws dark** — its own screen is dark glass with phosphor on it and can be
  nothing else, so the chrome around it stopped following the app's light register. That also fixed
  the on-screen keyboard, whose keys are white at a fifth strength with white labels: on the light
  ground a label sat at 1.00:1 against the key it was printed on, and now reads at 9.28:1.
- **Where a machine came from** — configurations record the catalog program they were installed
  from, so an entry knows which machines are its own. Not an Android feature: Android had the same
  name-matching guess and the same ways of getting it wrong.
