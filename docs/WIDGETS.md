# Widget groundwork

Widgets will use Jetpack Glance (`androidx.glance:glance-appwidget` + `glance-material3`), already
pinned in `gradle/libs.versions.toml` but not yet added to `:app`.

## Plan
1. Add the Glance dependencies to `app/build.gradle.kts`.
2. Create `widget/<Name>Widget.kt` (`GlanceAppWidget`) and `<Name>WidgetReceiver.kt`.
3. Register the receiver in the manifest with `android.appwidget.action.APPWIDGET_UPDATE` and an
   `res/xml/<name>_widget_info.xml` (`targetCellWidth/Height`, `previewLayout`, `resizeMode`).
4. Reuse the theme: Glance has its own `GlanceTheme`; map `NothingColors` into a `ColorProviders`
   and use Ndot via `FontFamily` on `TextStyle` where supported, otherwise pre-render dot-matrix
   text with `DotMatrix` into a bitmap.
5. Keep every widget to the same visual rules as the app (see `docs/DESIGN.md`): flat card, 28dp
   radius, one accent.

## Candidate first widgets
- Dot-matrix clock (reuses `DotMatrixClock`'s frame).
- Battery ring (`NothingProgressRing` look, rendered as a bitmap).

## Precise volume
Writes the volume level as text ("47%" or "7/15") beside the system volume bar while the volume
changes. The bar itself is left alone. Lives in `volume/`; its page is Widgets › Precise volume.

### How it works
- `PreciseVolumeService` is an accessibility service, the only kind of app component that can both
  see where the system's volume popup is and draw above it: `TYPE_ACCESSIBILITY_OVERLAY` is layered
  over `TYPE_VOLUME_OVERLAY`, while a plain app overlay sits under it.
- Changes come from `android.media.VOLUME_CHANGED_ACTION`, hidden in `AudioManager` but sent by
  AudioService on every change. It is a protected broadcast, so only the system can send it.
  Only media, ring, notification, alarm and call volume count (`VolumeStreams`); streams that
  follow one of them send their own broadcasts with the same press.
- While a label is due, the service looks every 100 ms for the bar: system windows titled as the
  volume controls (AOSP: "Media volume controls") or, failing that, ones small enough not to be
  the shade or lock screen; in them, the first node with a range (a slider). `VolumeBar` picks
  the window and places the label on the side facing the middle of the screen, clear of the
  popup's edge. Nothing else is looked at: it takes no accessibility events and filters no keys.
- `LabelTimeline` decides when the label shows: with the bar, hidden soon after the bar goes.
  Once the bar has been found, a change without it (an app setting the volume quietly) shows
  nothing. Until then, the label stands in at the right edge for 3 s after the last change.

### Limits
- Nothing shows with the screen off: Android doesn't show the popup then either.
- Nothing OS's own popup hasn't been checked on a phone yet. If its slider isn't exposed to
  accessibility and its window isn't titled with "volume", the label falls back to the right edge.
- Sideloaded installs may need App info › ⋮ › Allow restricted settings before the service can
  be switched on. On Android 17, Advanced Protection mode turns off accessibility services that
  aren't accessibility tools, this one included.

### Checking it on the phone
1. Widgets › Precise volume › **Turn on**, then switch Precise volume on in Accessibility settings.
2. Press a volume key: the label should appear beside the bar, then go with it.
3. If it shows at the right edge instead, the popup wasn't recognised. While it is up,
   `adb shell dumpsys window windows | grep -i volume` shows what its window is called and where.
