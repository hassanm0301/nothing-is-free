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
- `glyph/toy/` – the [toys](#toys), **Digital clock** and **Binary clock**, sharing:
  - `ToyFace` – what a toy draws on its own: `render(time, is24Hour)`, plus `occupies(x, y)`, the
    area it keeps for itself, so addons can draw around it.
  - `ToyId` – the toy registry, in the order the Glyph tab lists them. `face(size, …)` builds a
    toy's face with its current options. Persisted by name (addon settings), so don't rename
    entries.
  - `ToyLoop` – the one SDK-free loop every toy runs: a frame each minute and on `refresh()`, one
    at once when the face, the addon switch, the last alert or the pulse window changes, and 25 fps
    while a notification pulse is running. Draws through `frame()` (see [addons](#addons)).
  - `GlyphToyService` – the base service. Binds `GlyphMatrixManager` and registers the device,
    runs `ToyLoop`, pushes frames with `setMatrixFrame` (dropped frames are logged), forwards
    `EVENT_AOD` to `refresh()`, and holds the pulse wake lock. Each toy only gives its `toyId` and
    `faces(size)`, a flow that emits again when the toy's options change.
  - `MatrixArbiter` – shares the SDK's singleton manager between toys: `init` for the first bound
    toy, `turnOff` + `unInit` after the last, and only the most recently bound toy draws. The
    Glyph service may bind a newly picked toy before unbinding the old one; without this the new
    toy stayed blank and the old one's `unInit` tore down the shared connection.
  - `RoundMatrix` – the round LED mask every face draws inside.
- `glyph/addon/` – the [addons](#addons), drawn on top of any toy and switched on per toy:
  - `AddonPreferences` – which toys each addon is on for, and the pulse window, persisted.
  - `NotificationPulse` – the pulse ring, and `ToyFace.frame(time, is24Hour, pulseLevel, …)`,
    the full frame a toy shows: face plus pulse. The toy services and the in-app previews both draw
    through it, so a preview is exactly what the matrix shows.
  - `PulseWave` – the breathing curve and window maths.

## SDK facts (read from the 2.0 AAR)
- `Glyph.DEVICE_*` are `Build.MODEL` strings, not codenames: Phone (4a) Pro is `A069P`
  (`DEVICE_25111p`), Phone (3) is `A024` (`DEVICE_23112`). `register()` takes these.
- Frame values are **0..4095**. The SDK's own bitmap path scales 0..255 grey by `* 4095 / 255`;
  raw 0..255 frames come out 16x too dim.
- `GlyphMatrixManager.getInstance()` is a process-wide singleton.
- `GlyphToy`: `MSG_GLYPH_TOY = 1`, bundle key `"data"`, events `"aod"`, `"change"`,
  `"action_down"`, `"action_up"`.
- No `NothingKey` meta-data is needed for the matrix calls.

## Toys
Each toy redraws once a minute and on `EVENT_AOD`, follows the phone's 12/24-hour setting, and
takes any [addons](#addons) switched on for it. In the app, the **Glyph** tab's **Toys** sub-tab
lists them; tapping one opens its settings, with a **Preview** button.

To use one on the Phone (4a) Pro: Settings > Glyph Interface > Flip to Glyph > Always-on Glyph
Toy > the toy's name, then flip the phone. Changes to a toy's settings or addons in the app show
on the matrix straight away.

### Digital clock
Hours over minutes in 3x5 digits on the 13x13 matrix (5x7 on a 25x25). The edge is left free for
addons. Before addons this was the "Pulse clock", with the notification pulse built in; the pulse
is now an addon, on for this toy by default.

- `glyph/toy/ClockFace` – the frame.
- `glyph/toy/ClockToyService` – the toy service. It keeps its old class name because the phone
  remembers the chosen toy by component name; renaming it would quietly unselect the toy.

### Binary clock
HH:MM in binary-coded decimal: four columns (hours tens, hours units, minutes tens, minutes
units), each a stack of bits worth 8, 4, 2 and 1 from top to bottom. Add up a column's set bits
to read its digit. Set bits light at the active brightness, clear bits at the inactive one.
On a 25x25 matrix every bit is drawn twice as large.

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
- `glyph/toy/BinaryClockToyService` – the toy service. Its face flow emits again whenever the
  settings change.
- `glyph/toy/BinaryClockPreferences` – the settings, persisted.

No notification access is needed unless the notification pulse is switched on for it.

## Addons
Addons draw on top of any toy, only outside the area its face occupies, and each is switched on
per toy. In the app, the **Glyph** tab's **Addons** sub-tab lists them; tapping one opens its
settings, with a switch for each toy and a **Test on phone** button.

### Notification pulse
When an alerting notification arrives, the outer ring of the matrix (the edge LEDs the face leaves
free) breathes, 2 s per breath, for the chosen window rounded up to whole breaths. Then the toy
goes back to steady.

- **Per-toy switch** – on for the Digital clock by default, as the old Pulse clock was; off for
  the Binary clock. Switching it off mid-pulse stops the pulse at once.
- **Window** – how long after a notification the ring keeps breathing: 10 s, 30 s (default),
  1 min, 2 min or 5 min. One setting for every toy; the old Pulse clock's choice carries over.
- **Notification access** – the app only learns about notifications through
  `GlyphNotificationListener`, shown in system settings as "Glyph notification pulse".
  Without access the ring never pulses.
- **Wake lock** – while a pulse runs on a toy it is on for, `GlyphToyService` holds a partial
  wake lock (not reference counted) for what is left of the pulse, so the animation keeps
  running with the screen off. It is let go when the pulse ends, when the addon is switched off
  for the toy, and when the toy stops. A toy without the pulse never takes it.
- **Test on phone** starts a pulse without waiting for a notification.

- `glyph/addon/NotificationPulse` – the ring: the round matrix's edge minus what the face
  occupies (`ToyLoop` works it out once per face, not per frame). Also `frame()`, which lays it
  over the face.
- `glyph/addon/PulseWave` – the breathing curve and window maths.
- `glyph/addon/AddonPreferences` – the per-toy switch and the window, persisted.
- `glyph/toy/ToyLoop` – animates the ring at 25 fps while a pulse runs on a toy it is on for.
- `core/notifications/GlyphNotificationListener` – stamps `RecentNotifications` when an
  alerting notification arrives. Skipped: our own, ongoing (media, downloads), silent channels,
  ones Do Not Disturb blocks, and `FLAG_ONLY_ALERT_ONCE` updates to one already showing.

#### Using it on the Phone (4a) Pro
1. Install, open the app and grant notification access. On a sideloaded build Android may block
   this as a restricted setting: App info > ⋮ > Allow restricted settings, then try again.
2. Switch the pulse on for the toy you use, and pick that toy on the phone (see [toys](#toys)).
3. Flip the phone. **Test on phone** starts a pulse without waiting for a notification.

## Next steps
- `GlyphMatrixController : GlyphController` for in-app frames (`setAppMatrixFrame`, needs
  system version 20250801+; `closeAppMatrix()` to release). Mind the shared singleton with the toy.
- Debug on device: `adb shell settings put global nt_glyph_interface_debug_enable 1`
  (auto-disables after 48h).

## Open questions
- The matrix mask in `RoundMatrix.isLed` assumes the LEDs fill the circle inscribed in the
  13x13 square. If the ring sits a dot off on hardware, adjust it there; every face and the pulse
  ring use it.
- The (3a) Pro (`A059P`) and (4b) model strings are not in the SDK; confirm them from the
  Device card in the app's Settings tab.
