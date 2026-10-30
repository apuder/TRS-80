# App Store listing assets

What the App Store listing is built from, and the rules the files have to obey.
Apple's own page is the authority and it moves —
[App Store Connect: screenshot specifications](https://developer.apple.com/help/app-store-connect/reference/screenshot-specifications/) —
so check before a submission rather than trusting this page.

## Screenshots

**Only the largest size in each family is needed.** Apple scales those down for
every smaller device, which is a recent simplification and a considerable
saving: two sets rather than eight.

| Family | Accepted sizes | Simulator that gives it natively |
| --- | --- | --- |
| 6.5-inch iPhone | 1242×2688, 2688×1242, 1284×2778, 2778×1284 | iPhone 11 Pro Max → **1242×2688** |
| 13-inch iPad | 2064×2752, 2752×2064, 2048×2732, 2732×2048 | iPad Pro 13-inch (M4) → **2064×2752** |
| iPhone Duo, outer display | 1398×2034, 2034×1398 | iPhone Duo, folded → **1398×2034** |
| iPhone Duo, inner display | 2007×2853, 2853×2007 | iPhone Duo, unfolded → **2007×2853** |

**iPhone Duo screenshots are what earn the badge.** A build made with the iOS
27.1 SDK or later, plus Duo screenshots, puts "optimized for iPhone Duo" on the
product page. They are optional for now. From April 2027 they are required for
any app built with the 27.1 SDK. Before the app has an approved version, they
can only go in with an app version's submission: the Asset Library will take
the upload, but it will not submit it on its own.

- **Up to 10 per family, per localisation.** Two is not a listing; the slots are
  the pitch.
- JPG or PNG, RGB, **flattened — no transparency**. `simctl` writes RGBA, so
  every capture has to be flattened before it is uploaded.
- The app is localised in **English and German**, so a listing in both wants a
  set in each. The screens differ: German strings are longer and wrap.

### What is here

Sixteen: nine English in `screenshots/` and seven German in `screenshots-de/`.
`phone-*` are 1242×2688 and `ipad-*` are 2752×2064. `duo-outer-*` is 1398×2034
(folded). `duo-inner-*` is 2853×2007 (unfolded and turned sideways, which is
where the inner display is wide enough for two panes).

| | Phone | iPad | iPhone Duo |
| --- | --- | --- | --- |
| `-1-library` | your machines, then the catalog | the same, two-pane, beside what you were last playing | outer: as on the phone; inner: two-pane, as on the iPad |
| `-2-playing` | a machine mid-game with the keyboard | a machine with the screen to itself | — |
| `-3-catalog-entry` | an entry, its description and its screens | the same, in the pane beside the list | — |
| `-4-settings` | settings | — | — |

The Duo set is English only so far.

A catalog entry's description comes from RetroStore and is English in both
sets; only the app's own chrome around it is translated. That is what the app
shows, not something to fix here.

Ten per family per language is the allowance, so there is still room for more
of each.

### Taking them

Use a simulator that is already the right size. Do not scale a capture
afterwards — it softens the phosphor text, which is most of what there is to
look at here.

```sh
xcrun simctl create "TRS80-6.5in" \
  com.apple.CoreSimulator.SimDeviceType.iPhone-11-Pro-Max \
  com.apple.CoreSimulator.SimRuntime.iOS-18-5
xcrun simctl boot <udid>
xcrun simctl install <udid> <path>/TRS-80.app
xcrun simctl io <udid> screenshot out.png
magick out.png -background white -alpha remove -alpha off -depth 8 PNG24:out.png
```

**The iPad framebuffer comes out portrait even when the device is landscape.**
`simctl` captures 2064×2752 with the content lying on its side; rotate it 90°
afterwards and the result is 2752×2064, which is an accepted size. Lossless, and
it catches people out — check which way up it landed rather than assuming, the
rotation direction depends on which way the device was turned.

**Driving the simulator needs idb.** `simctl` cannot inject taps and AppleScript
against the Simulator window needs an Accessibility grant a shell does not have.
`brew install idb-companion` plus `pip install fb-idb` in a virtualenv gets
there, with one catch: fb-idb 1.1.7 calls `asyncio.get_event_loop()`, which
Python 3.12 removed, so on a modern Python it dies before doing anything. The
fix is to return the running loop when there is one and a new loop otherwise —
substituting `new_event_loop()` everywhere instead trades the crash for
"attached to a different loop", which is the same bug wearing a hat.

**A language is a launch argument**, so a German set needs no reboot and no
second simulator:

```sh
xcrun simctl launch <udid> com.trs80app -AppleLanguages '(de)' -AppleLocale de_DE
```

It lasts for that launch only. Anything that restarts the app drops back to
English, and the screenshot looks perfectly fine until you read it.

**Ask the screen where things are** rather than measuring a screenshot:

```sh
idb ui describe-all --udid <udid>     # every element, with its frame
idb ui tap --udid <udid> <x> <y>      # points, not pixels
```

On a landscape iPad the two disagree about which way is up: `describe-all`
answers in the app's landscape space, `tap` wants the portrait framebuffer.
Convert with `x = landscapeY`, `y = deviceHeightInPoints - landscapeX` — 1376
for the 13-inch. A tap that silently does nothing is usually this.

### The iPhone Duo

The Duo simulator needs Xcode 27.1 and the iOS 27.1 runtime: its device type
refuses anything older. `xcodebuild -downloadPlatform iOS` fetches the runtime,
which takes about 8 GB of free space to download and install.

```sh
xcrun simctl create "iPhone Duo" \
  com.apple.CoreSimulator.SimDeviceType.iPhone-Duo \
  com.apple.CoreSimulator.SimRuntime.iOS-27-1
```

**Simulator.app is gone in Xcode 27.1.** Its replacement is Device Hub, at
`Xcode.app/Contents/Applications/DeviceHub.app`. Folding, unfolding and turning
the Duo happen there: `simctl` has no way to fold it.

**It has two displays, and a screenshot takes the inner one unless told
otherwise.** When folded, that is a black frame. `simctl io <udid> enumerate`
lists the displays: the inner is 2007×2853 and the outer 1398×2034. Name one by
its UUID:

```sh
xcrun simctl io <udid> screenshot --mask=ignored --display=<display-uuid> out.png
ffmpeg -i out.png -pix_fmt rgb24 flat.png    # drops the alpha, lossless
```

Unlike the iPad's, these come out the right way up in either orientation. The
status bar was set to 9:41 with full battery and signal first
(`simctl status_bar <udid> override ...`). On the Duo it runs down the right
edge, so check that nothing on screen sits under it.

A build for the simulator from the command line wants `ARCHS=arm64`. The generic
simulator destination also asks for x86_64, which the shared module does not
build, and Gradle fails with `Unknown iOS simulator arch: 'x86_64'`.

## App icon

1024×1024, **no alpha channel and no rounded corners** — iOS masks the corners
itself, and an icon that arrives already rounded gets rounded twice. It comes
from the app bundle rather than being uploaded separately, so it follows
whatever `iosApp/TRS80/Assets.xcassets/AppIcon.appiconset` holds.

## What the two stores do not share

Worth saying plainly, because the assets look interchangeable and are not:

- Play wants **1024×500** for its feature graphic. Apple has no equivalent.
- Play's icon is **512×512 with alpha**; Apple's is **1024×1024 without**.
- Play caps a screenshot's long side at **twice** the short side. Apple has no
  such rule, which is why a tall phone screenshot is fine here and rejected
  there.
