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
