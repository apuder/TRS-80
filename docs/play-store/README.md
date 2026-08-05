# Play Store listing assets

What the Google Play listing is built from, and the rules the files have to obey.
Requirements are Google's, from
[Preview asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151);
they change, so check before a listing update rather than trusting this page.

## Screenshots

In `screenshots/`. Five phone, two tablet, all taken from the Android emulator
running the release build.

| File | Size | What it shows |
| --- | --- | --- |
| `phone-1-library.png` | 1080×1920 | The library: your machines, then the catalog |
| `phone-2-playing.png` | 1080×1920 | Armored Patrol, mid-game, with the keyboard |
| `phone-3-catalog-entry.png` | 1080×1920 | A catalog entry, its description and its screens |
| `phone-4-settings.png` | 1080×1920 | Settings |
| `phone-5-landscape.png` | 1920×1080 | The machine with the screen to itself, sideways |
| `tablet-1-library.png` | 1200×1920 | The library on a 10-inch tablet |
| `tablet-2-two-pane.png` | 1920×1200 | The two-pane layout, and what you were last playing |

### The rules

- **JPEG or 24-bit PNG, and no alpha channel.** `adb exec-out screencap -p`
  writes RGBA, so every capture has to be flattened before it is uploaded.
- Between **320px and 3840px** on a side, and the long side may be **no more
  than twice** the short one.
- Two screenshots is the minimum to publish; eight per device type is the most.
- **Four at 1080px or better** is what Play wants before it will consider the app
  for its recommendation surfaces. That is the bar these are built to.

### The trap worth knowing

A modern phone is taller than Play allows. The Pixel 10 Pro emulator is
1280×2856 — a ratio of **2.23:1**, over the 2:1 limit — so a raw screencap is
rejected. Do not scale one afterwards either; that softens the phosphor text,
which is most of what there is to look at here.

Set the display to a compliant size and capture natively instead:

```sh
adb shell wm size 1080x1920 && adb shell wm density 420   # phone
adb shell wm size 1200x1920 && adb shell wm density 240   # 10-inch tablet
adb shell wm size 1920x1200 && adb shell wm density 240   # the same, sideways
adb shell wm size reset && adb shell wm density reset     # afterwards
```

Then flatten, and check what you have:

```sh
magick in.png -background white -alpha remove -alpha off -depth 8 PNG24:out.png
magick identify -format '%wx%h %A\n' out.png              # wants alpha=Undefined
```

Give the catalog half a minute before capturing anything with cover art in it.
The art is fetched from RetroStore one image at a time, and a screenshot taken
too early is a grid of black rectangles.

## Icon and feature graphic

Not here yet; they are part of the logo work. Both live in `var/icons` today.

- **Icon** — 512×512, 32-bit PNG **with** alpha, at most 1024 KB.
- **Feature graphic** — 1024×500, JPEG or 24-bit PNG, **no** alpha. Required for
  editorial placement.

Note the icon is the one asset that wants an alpha channel and the only one.
