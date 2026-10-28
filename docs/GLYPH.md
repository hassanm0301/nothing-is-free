# Glyph groundwork

Target device: **Phone (4a) Pro**. It has a circular 13-wide Glyph Matrix (no light strips), driven
by the GlyphMatrix SDK. Only always-on-display (AOD) toys are supported on it and there is no
Glyph Touch. Device identifier: `Glyph.DEVICE_25111p`.

Reference: <https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit>
(the same AAR also serves the strip devices; the older Glyph Developer Kit README lists their ids).

## What exists now
- `app/libs/glyph-matrix-sdk-2.0.aar` – the SDK, wired in through `fileTree("libs")`, plus the
  `com.nothing.ketchum.permission.ENABLE` permission. Its EULA (section 2.1c) restricts
  redistribution; keep that in mind if the repo is public.
- `glyph/GlyphCapability` – what the phone can do (None / Strips / Matrix), derived from
  `core/device/NothingDevice`.
- `glyph/GlyphController` – the app-facing interface for in-app frames, still `NoOpGlyphController`.
- `dotmatrix/DotMatrix` – the frame model. `toFrame(max = 4095)` yields the `int[]` the SDK takes.
- `glyph/toy/` – the **Pulse clock** AOD toy (below).

## SDK facts (read from the 2.0 AAR)
- `Glyph.DEVICE_*` are `Build.MODEL` strings, not codenames: Phone (4a) Pro is `A069P`
  (`DEVICE_25111p`), Phone (3) is `A024` (`DEVICE_23112`). `register()` takes these.
- Frame values are **0..4095**. The SDK's own bitmap path scales 0..255 grey by `* 4095 / 255`;
  raw 0..255 frames come out 16x too dim.
- `GlyphMatrixManager.getInstance()` is a process-wide singleton.
- `GlyphToy`: `MSG_GLYPH_TOY = 1`, bundle key `"data"`, events `"aod"`, `"change"`,
  `"action_down"`, `"action_up"`.
- No `NothingKey` meta-data is needed for the matrix calls.

## Pulse clock toy
Hours over minutes in 3x5 digits on the 13x13 matrix (5x7 on a 25x25). When a notification
arrives, the outer ring breathes (2 s per breath) for the chosen window, rounded up to whole
breaths, then the clock goes back to steady.

- `glyph/toy/ClockFace` – the frame: digits, the ring, the round-matrix mask.
- `glyph/toy/PulseWave` – the breathing curve and window maths.
- `glyph/toy/ClockToy` – the loop: one frame a minute, 25 fps while pulsing, redraw on demand.
- `glyph/toy/ClockToyService` – the toy service. Binds `GlyphMatrixManager`, pushes frames with
  `setMatrixFrame`, redraws on `EVENT_AOD`, and holds a partial wake lock for the pulse window
  only, so the animation keeps running with the screen off.
- `glyph/toy/ClockToyPreferences` – the pulse window (10 s to 5 min, default 30 s), persisted.
- `core/notifications/GlyphNotificationListener` – stamps `RecentNotifications` when an
  alerting notification arrives. Skipped: our own, ongoing (media, downloads), silent channels,
  ones Do Not Disturb blocks, and `FLAG_ONLY_ALERT_ONCE` updates to one already showing.
- The gallery's **Glyph toy** card shows the same frame live, sets the window, opens
  notification access, and has a **Test pulse** button.

### Using it on the Phone (4a) Pro
1. Install, open the app, tap **Grant access** and allow notification access. On a sideloaded
   build Android may block this as a restricted setting: App info > ⋮ > Allow restricted
   settings, then try again.
2. Settings > Glyph Interface > Flip to Glyph > Always-on Glyph Toy > **Pulse clock**.
3. Flip the phone. **Test pulse** in the app starts a pulse without waiting for a notification.

## Next steps
- `GlyphMatrixController : GlyphController` for in-app frames (`setAppMatrixFrame`, needs
  system version 20250801+; `closeAppMatrix()` to release). Mind the shared singleton with the toy.
- Debug on device: `adb shell settings put global nt_glyph_interface_debug_enable 1`
  (auto-disables after 48h).

## Open questions
- The matrix mask in `ClockFace.inCircle` assumes the LEDs fill the circle inscribed in the
  13x13 square. If the ring sits a dot off on hardware, adjust it there.
- The (3a) Pro (`A059P`) and (4b) model strings are not in the SDK; confirm them from the
  gallery's Device card.
