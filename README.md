# Nothing is Free

An Android app in the style of Nothing OS. Jetpack Compose, Kotlin, min Android 14.
Currently a styled base: theme, type, component kit and a gallery screen. Home-screen widgets and
Glyph (Phone (4a) Pro) are next; the groundwork for both is in place.

## Build
Requires JDK 17+ and an Android SDK with platform 37.

```bash
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`. Install with
`adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Layout
```
app/src/main/kotlin/io/github/hassanm0301/nothingisfree/
  ui/theme/        colours, type (Ndot / NType 82 / Lettera Mono), shapes, spacing, theme mode
  ui/components/   the Nothing component kit
  ui/gallery/      the gallery screen + view model
  dotmatrix/       DotMatrix frame model, 5x7 dot font, Compose renderer
  core/device/     Nothing device detection
  glyph/           GlyphCapability + GlyphController (no-op until the SDK is added)
  widget/          empty; Glance widgets go here
docs/              DESIGN.md, GLYPH.md, WIDGETS.md
```

## Fonts
Bundled from <https://github.com/xeji01/nothingfont>. These are Nothing's own typefaces and are
not openly licensed; keep that in mind before distributing builds.
