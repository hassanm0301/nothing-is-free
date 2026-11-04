# Nothing is Free

An Android app in the style of Nothing OS. Jetpack Compose, Kotlin, min Android 14.
Three tabs:
- **Glyph** – always-on Glyph Toys for the Phone (4a) Pro, in two lists (see `docs/GLYPH.md`):
  - *Toys*: **Digital clock** (Standard or Minimal font, the fewest LEDs; adjustable brightness),
    **Binary clock** (HH:MM in BCD, dot or 2x2 square bits, adjustable active/inactive brightness)
    and **Compact digital clock** (just two digits: the hour's first and the minute's second). Each opens a settings page with a
    live matrix and a **Preview** sheet: scrub the time of day and simulate a notification.
  - *Addons*: drawn on top of whichever toy is showing, in categories with one addon of each on
    at a time, switched once for every toy from the Addons list. *Notification* – **Notification pulse**:
    the outer ring plays a pattern of up to 8 steps (brightness and time each) a set number of
    times after a notification, with a battery saver that only checks once a minute, with the clock.
    *Battery* – **Battery level**: five dots on the top, bottom, left or right edge show the
    charge; top and bottom can drain right to left or left to right, and the highest dot can fade
    with what is left of its 20%. **Binary battery**: the exact percentage in BCD on a 5x2 block at
    an edge, with charging and battery saver dots. **Battery ring**: the whole outer edge is lit
    when full and empties from the top, clockwise or anticlockwise, as the charge drops.
- **Widgets** – **Precise volume**: the volume level as text (percent or steps) beside the system
  volume bar while you change the volume, from an accessibility service (see `docs/WIDGETS.md`).
  Below it, previews of the home-screen widgets to come.
- **Settings** – updates, theme, device info and about.

## Build
Requires JDK 17+ and an Android SDK with platform 37.

```bash
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`. Install with
`adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Updates
On launch the app asks GitHub for the latest release of `hassanm0301/nothing-is-free`. If its tag
(e.g. `v0.2.0`) is newer than `versionName`, the Settings tab shows an update card (and its tab gets a red dot); **Update** downloads
the release's `.apk` asset and installs it with `PackageInstaller`. The first time, Android asks
to allow "Install unknown apps" and to confirm the install. After that this app is the installer
of record, so later updates may skip the prompt.

Android only accepts an update signed with the **same key** as the installed copy, so every
release is signed with one release key (see below). Debug builds use per-machine debug keys and
cannot update a release install; uninstall first.

## Branches
- **`develop`** – every change lands here first, from a feature branch (branch off `develop`,
  merge back into it). Each merge publishes a **preview**: it bumps `versionCode` and the preview
  number in `versionName` (`0.7.0-beta.1`, then `0.7.0-beta.2`, …), and pushing `develop` releases
  that as a GitHub pre-release. So `develop` always has a preview release.
- **`main`** – full releases only. It only takes merges from `develop`.

## Releases
### Previews, from `develop`
1. In the merge into `develop`, bump `versionCode` and the preview number in `versionName` in
   `app/build.gradle.kts`. After a full release, start the next one's previews at `-beta.1`
   (e.g. `0.8.0-beta.1` after `0.7.0`).
2. Push `develop`. `.github/workflows/release.yml` publishes it as `v0.7.0-beta.1`, a
   pre-release. If that tag is taken (the bump was forgotten), the run fails and says so.

Any version with a `-` is published as a GitHub pre-release. The app only offers pre-releases
with **Settings › Updates › Beta updates** on; then it offers the highest version among recent
releases, so `0.7.0-beta.2` follows `0.7.0-beta.1` and `0.7.0` follows both. Turning beta off
keeps the installed beta until a newer full release is out.

### Full releases, on `main`
1. On `develop`, set the full `versionName` (`0.7.0-beta.3` → `0.7.0`) and bump `versionCode`.
   Pushing `develop` with a full version publishes nothing; the run leaves a note instead.
2. Merge `develop` into `main`.
3. Start the release, either way:
   - **Run workflow**: Actions › Release › Run workflow, on `main`. It tags the commit it builds
     `v` + `versionName` (e.g. `v0.7.0`) and stops if that tag already exists. Claude Code cloud
     sessions cannot push tags, so they release this way.
   - **Push a tag** matching `versionName`: `git tag v0.7.0 && git push origin v0.7.0`.
4. Back on `develop`, the next merge starts the next previews (`0.8.0-beta.1`).

Either way the workflow runs the tests, builds a signed `assembleRelease` and publishes it as
`nothing-is-free-v0.7.0.apk` on the release. **Run workflow** on `develop` works too, for a
preview version only.

### Signing key
The release key is `~/.android/nothing-is-free-release.jks` (alias `nothing-is-free`). Its
password is in `keystore.properties` at the repo root, which is gitignored and read by Gradle
for local release builds. **Back up both.** Without them, installed copies can never be updated
again.

CI reads the key from these repository secrets (Settings › Secrets and variables › Actions):

| Secret | Value |
| --- | --- |
| `NIF_KEYSTORE_BASE64` | `base64 -w0 ~/.android/nothing-is-free-release.jks` |
| `NIF_KEYSTORE_PASSWORD` | `storePassword` from `keystore.properties` |
| `NIF_KEY_ALIAS` | `nothing-is-free` |
| `NIF_KEY_PASSWORD` | `keyPassword` from `keystore.properties` |

## Layout
```
app/src/main/kotlin/io/github/hassanm0301/nothingisfree/
  ui/theme/        colours, type (Ndot / NType 82 / Lettera Mono), shapes, spacing, theme mode
  ui/components/   the Nothing component kit
  ui/              AppScreen (tab bar + tabs) and the shared AppViewModel
  dotmatrix/       DotMatrix frame model, dot fonts (5x7, 3x5, minimal), Compose renderer
  core/device/     Nothing device detection
  core/battery/    the phone's battery level, for the battery level addon
  core/notifications/  notification listener feeding the Glyph pulse
  core/update/     self-update from GitHub releases (check, download, PackageInstaller)
  glyph/           GlyphCapability + GlyphController (in-app frames, still a no-op)
  glyph/toy/       the Glyph Toys (Digital clock, Binary clock) on a shared GlyphToyService
  glyph/addon/     addons drawn on any toy (Notification pulse, Battery level) and their settings
  ui/glyph/        Glyph tab: toy and addon lists, their settings pages, the preview sheet
  ui/widgets/      Widgets tab and the Precise volume page
  ui/settings/     Settings tab
  ui/update/       the update card
  volume/          Precise volume: the accessibility service, finding the bar, label placement
  widget/          empty; Glance widgets go here
docs/              DESIGN.md, GLYPH.md, WIDGETS.md
```

## Fonts
Bundled from <https://github.com/xeji01/nothingfont>. These are Nothing's own typefaces and are
not openly licensed; keep that in mind before distributing builds.
