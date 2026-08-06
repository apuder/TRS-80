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

- **Up to 10 per family, per localisation.** Two is not a listing; the slots are
  the pitch.
- JPG or PNG, RGB, **flattened — no transparency**. `simctl` writes RGBA, so
  every capture has to be flattened before it is uploaded.
- The app is localised in **English and German**, so a listing in both wants a
  set in each. The screens differ: German strings are longer and wrap.

### What is here

| File | Size | |
| --- | --- | --- |
| `screenshots/phone-1-library.png` | 1242×2688 | The library: your machines, then the catalog |
| `screenshots/ipad-1-library.png` | 2752×2064 | The two-pane layout, and what you were last playing |

**Two of a possible twenty**, and both English. Still wanted, in rough order of
what a listing gains from it: a machine mid-game; a machine sideways with the
screen to itself; a catalog entry with its description and screens; settings.
Then the same again in German.

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

**Driving the simulator is the hard part.** `simctl` cannot inject taps, `idb`
is a separate install, and AppleScript against the Simulator window needs
Accessibility permission that a shell does not have by default. Without one of
those, screenshots can only be taken of whatever screen somebody else has
navigated to.

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
