# App Store listing assets

Nothing here yet. The iOS app has not been submitted, and screenshots taken from
a simulator before the new icon exists would only have to be taken again.

What goes here when it does, and the rules, so nobody has to look them up twice.
Apple's own page is the authority and it moves —
[App Store Connect: screenshot specifications](https://developer.apple.com/help/app-store-connect/reference/screenshot-specifications/) —
so check before a submission rather than trusting this page.

## Screenshots

**Only the largest size in each family is needed.** Apple scales those down for
every smaller device, which is a recent simplification and a considerable saving:
two sets rather than eight.

| Family | Size | Device it matches |
| --- | --- | --- |
| iPhone | 1320×2868 | 6.9-inch, the Pro Max |
| iPad | 2064×2752 | 13-inch iPad Pro, and only if the app ships for iPad |

- Up to **10** per localisation.
- JPG or PNG, RGB, **flattened — no transparency**. The same rule Play has, for
  the same reason, and the same trap: a simulator capture carries alpha.
- The app is localised in English and German, so a listing in both wants a set
  in each. The screens differ: the German strings are longer.

Take them with the simulator at the right device, rather than scaling:

```sh
xcrun simctl list devices available            # find the 6.9-inch one
xcrun simctl io <udid> screenshot out.png
magick out.png -background white -alpha remove -alpha off -depth 8 PNG24:out.png
```

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
  such rule, which is why a tall phone screenshot is fine here and rejected there.
