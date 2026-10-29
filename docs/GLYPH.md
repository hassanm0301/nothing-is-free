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
- `glyph/toy/` – the **Pulse clock** and **Binary clock** AOD toys (below), sharing:
  - `GlyphToyService` – the base service. Binds `GlyphMatrixManager` and registers the device,
    pushes frames with `setMatrixFrame` (dropped frames are logged), and forwards `EVENT_AOD`.
    Each toy only implements `onToyStart` / `onAod` / `onToyStop`.
  - `MatrixArbiter` – shares the SDK's singleton manager between toys: `init` for the first bound
    toy, `turnOff` + `unInit` after the last, and only the most recently bound toy draws. The
    Glyph service may bind a newly picked toy before unbinding the old one; without this the new
    toy stayed blank and the old one's `unInit` tore down the shared connection.
  - `RoundMatrix` – the round LED mask both faces draw inside.

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

- `glyph/toy/ClockFace` – the frame: digits and the ring.
- `glyph/toy/PulseWave` – the breathing curve and window maths.
- `glyph/toy/ClockToy` – the loop: one frame a minute, 25 fps while pulsing, redraw on demand.
- `glyph/toy/ClockToyService` – the toy service, on `GlyphToyService`. Redraws on `EVENT_AOD`
  and holds a partial wake lock for the pulse window only, so the animation keeps running with
  the screen off.
- `glyph/toy/ClockToyPreferences` – the pulse window (10 s to 5 min, default 30 s), persisted.
- `core/notifications/GlyphNotificationListener` – stamps `RecentNotifications` when an
  alerting notification arrives. Skipped: our own, ongoing (media, downloads), silent channels,
  ones Do Not Disturb blocks, and `FLAG_ONLY_ALERT_ONCE` updates to one already showing.
- The **Glyph** tab's Pulse clock card shows the same frame live, sets the window, opens
  notification access, and has a **Test pulse** button.

### Using it on the Phone (4a) Pro
1. Install, open the app, tap **Grant access** and allow notification access. On a sideloaded
   build Android may block this as a restricted setting: App info > ⋮ > Allow restricted
   settings, then try again.
2. Settings > Glyph Interface > Flip to Glyph > Always-on Glyph Toy > **Pulse clock**.
3. Flip the phone. **Test pulse** in the app starts a pulse without waiting for a notification.

## Binary clock toy
HH:MM in binary-coded decimal: four columns (hours tens, hours units, minutes tens, minutes
units), each a stack of bits worth 8, 4, 2 and 1 from top to bottom. Add up a column's set bits
to read its digit. Set bits light at the active brightness, clear bits at the inactive one.
Follows the phone's 12/24-hour setting. On a 25x25 matrix every bit is drawn twice as large.

Options (`BinaryClockSettings`), applied to the matrix as soon as they change:
- **Style** – `SQUARE` (default): a 2x2 block per bit, blocks touching, 8x8 in all. `DOT`: one
  LED per bit.
- **Dot gap** – for `DOT` only: a dark LED between bits (7x7) or none (4x4). Squares always
  touch; with gaps they would not fit the round 13x13 matrix.
- **Unused bits** – the bits a digit never needs (8 and 4 of the hours tens, and its 2 on a
  12-hour clock; 8 of the minutes tens) stay dark, or show at the inactive brightness to
  complete the 4x4 grid.
- **Active / inactive brightness** – 0..255, rescaled to the hardware's 0..4095. Inactive 0 leaves
  clear bits dark, so only the set bits show.

- `glyph/toy/BinaryClockFace` – the frame.
- `glyph/toy/BinaryClockToy` – the loop: one frame a minute, redraw on demand, and again at once
  when the settings change.
- `glyph/toy/BinaryClockToyService` – the toy service, on `GlyphToyService`. Redraws on
  `EVENT_AOD`. No wake lock: the frame only changes once a minute.
- `glyph/toy/BinaryClockPreferences` – the settings, persisted.
- The **Glyph** tab's Binary clock card shows the same frame live (the preview approximates how
  bright a dimmed LED looks) and sets every option, with ±1 steps on the brightness sliders.

### Using it on the Phone (4a) Pro
1. Install. No notification access is needed.
2. Settings > Glyph Interface > Flip to Glyph > Always-on Glyph Toy > **Binary clock**.
3. Flip the phone. Changes to its settings in the app show on the matrix straight away.

## Next steps
- `GlyphMatrixController : GlyphController` for in-app frames (`setAppMatrixFrame`, needs
  system version 20250801+; `closeAppMatrix()` to release). Mind the shared singleton with the toy.
- Debug on device: `adb shell settings put global nt_glyph_interface_debug_enable 1`
  (auto-disables after 48h).

## Open questions
- The matrix mask in `RoundMatrix.isLed` assumes the LEDs fill the circle inscribed in the
  13x13 square. If the ring sits a dot off on hardware, adjust it there; both toys use it.
- The (3a) Pro (`A059P`) and (4b) model strings are not in the SDK; confirm them from the
  Device card in the app's Settings tab.
