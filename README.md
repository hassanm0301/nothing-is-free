# Nothing is Free

An Android app in the style of Nothing OS. Jetpack Compose, Kotlin, min Android 14.
Currently a styled base: theme, type, component kit and a gallery screen, plus a first Glyph
Toy for the Phone (4a) Pro: **Pulse clock**, a digital clock whose ring breathes after a
notification (see `docs/GLYPH.md`). Home-screen widgets are next.

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
  core/notifications/  notification listener feeding the Glyph pulse
  glyph/           GlyphCapability + GlyphController (in-app frames, still a no-op)
  glyph/toy/       the Pulse clock Glyph Toy: face, pulse, service, settings
  ui/glyph/        the Pulse clock card (live preview + settings)
  widget/          empty; Glance widgets go here
docs/              DESIGN.md, GLYPH.md, WIDGETS.md
```

## Fonts
Bundled from <https://github.com/xeji01/nothingfont>. These are Nothing's own typefaces and are
not openly licensed; keep that in mind before distributing builds.
