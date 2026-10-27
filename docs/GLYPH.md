# Glyph groundwork

Target device: **Phone (4a) Pro**. It has a circular 13-wide Glyph Matrix (no light strips), driven
by the GlyphMatrix SDK. Only always-on-display (AOD) toys are supported on it and there is no
Glyph Touch. Device identifier: `Glyph.DEVICE_25111p`.

Reference: <https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit>
(the same AAR also serves the strip devices; the older Glyph Developer Kit README lists their ids).

## What exists now
- `glyph/GlyphCapability` – what the phone can do (None / Strips / Matrix), derived from
  `core/device/NothingDevice`.
- `glyph/GlyphController` – the app-facing interface, with `NoOpGlyphController` in use today.
- `dotmatrix/DotMatrix` – the frame model. `toFrame()` yields the `int[]` layout the SDK takes.
- `app/libs/` – empty, where the AAR goes.
- `app/proguard-rules.pro` already keeps `com.nothing.ketchum.**`.

## Steps to light it up
1. Copy `glyph-matrix-sdk-*.aar` into `app/libs/` and uncomment the `fileTree("libs")` dependency
   in `app/build.gradle.kts`.
2. Uncomment `<uses-permission android:name="com.nothing.ketchum.permission.ENABLE" />` in the
   manifest.
3. Implement `GlyphMatrixController : GlyphController`:
   - `GlyphMatrixManager.getInstance(context)`, `init(callback)`; in `onServiceConnected` call
     `register(Glyph.DEVICE_25111p)`.
   - `show(frame)`: `setAppMatrixFrame(frame.toFrame())` (needs system version 20250801+;
     `setMatrixFrame` conflicts with active toys).
   - `clear()`: `closeAppMatrix()`; `disconnect()`: `unInit()`.
   - Build frames with `DotMatrix(13, 13)`; `Common.getDeviceMatrixLength()` confirms the size at runtime.
4. Return it from `GlyphModule.controller()` when `capability is GlyphCapability.Matrix`.
5. For an AOD toy, add a `Service` with `<action android:name="com.nothing.glyph.TOY"/>` and
   meta-data `com.nothing.glyph.toy.name`, `...toy.image`, and `com.nothing.glyph.toy.aod_support`
   = `1` (required on the (4a) Pro). Handle `GlyphToy.EVENT_AOD` (sent each minute) in the
   service's `Messenger` handler.
6. Debug on device: `adb shell settings put global nt_glyph_interface_debug_enable 1`
   (auto-disables after 48h).

## Open questions
- The `Build.MODEL` string for the (4a) Pro is not in `NothingDevice` yet; read it from the
  gallery's Device card and add it to the table.
