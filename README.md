# Nothing is Free

An Android app in the style of Nothing OS. Jetpack Compose, Kotlin, min Android 14.
Three tabs:
- **Glyph** – always-on Glyph Toys for the Phone (4a) Pro, in two lists (see `docs/GLYPH.md`):
  - *Toys*: **Digital clock** and **Binary clock** (HH:MM in BCD, dot or 2x2 square bits,
    adjustable active/inactive brightness). Each opens a settings page with a live matrix and a
    **Preview** sheet: scrub the time of day and simulate a notification.
  - *Addons*: drawn on top of any toy, switched on from each toy's page. **Notification pulse**:
    the outer ring pulses after a notification, with its rise time, max brightness and number of
    pulses adjustable, and a battery saver that only checks once a minute, with the clock.
- **Widgets** – previews of the home-screen widgets to come.
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

## Releases
1. Bump `versionCode` and `versionName` in `app/build.gradle.kts` and commit.
2. Tag and push: `git tag v0.2.0 && git push origin v0.2.0`.

`.github/workflows/release.yml` checks the tag matches `versionName`, runs the tests, builds a
signed `assembleRelease` and publishes it as `nothing-is-free-v0.2.0.apk` on the release.

### Beta releases
For a preview, use a pre-release `versionName` such as `0.6.0-beta.1` (still bump
`versionCode`) and tag `v0.6.0-beta.1`. Any tag with a `-` is published as a GitHub pre-release.
The app only offers pre-releases with **Settings › Updates › Beta updates** on; then it offers
the highest version among recent releases, so `0.6.0-beta.2` follows `0.6.0-beta.1` and `0.6.0`
follows both. Turning beta off keeps the installed beta until a newer full release is out.

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
  dotmatrix/       DotMatrix frame model, 5x7 dot font, Compose renderer
  core/device/     Nothing device detection
  core/notifications/  notification listener feeding the Glyph pulse
  core/update/     self-update from GitHub releases (check, download, PackageInstaller)
  glyph/           GlyphCapability + GlyphController (in-app frames, still a no-op)
  glyph/toy/       the Glyph Toys (Digital clock, Binary clock) on a shared GlyphToyService
  glyph/addon/     addons drawn on any toy (Notification pulse) and their settings
  ui/glyph/        Glyph tab: toy and addon lists, their settings pages, the preview sheet
  ui/widgets/      Widgets tab
  ui/settings/     Settings tab
  ui/update/       the update card
  widget/          empty; Glance widgets go here
docs/              DESIGN.md, GLYPH.md, WIDGETS.md
```

## Fonts
Bundled from <https://github.com/xeji01/nothingfont>. These are Nothing's own typefaces and are
not openly licensed; keep that in mind before distributing builds.
