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
  - `ToyId` – the toy registry, in the order the Glyph tab lists them.
    `face(size, digital, binary)` builds a toy's face with its current options. Its names are in
    the old per-toy addon settings, still read once (see [addons](#addons)), so don't rename
    entries.
  - `ToyLoop` – the one SDK-free loop every toy runs: a frame each minute and on `refresh()`, one
    at once when the face, an addon switch, the last alert (unless on battery saver) or an addon's
    settings change, and 25 fps while a notification pulse is running. Draws through `frame()` (see
    [addons](#addons)), says how long a pulse has left so the service can keep the CPU up, and
    reads the battery level for the battery addon on repaints that aren't pulse frames.
  - `GlyphToyService` – the base service. Binds `GlyphMatrixManager` and registers the device,
    runs `ToyLoop`, pushes frames with `setMatrixFrame` (dropped frames are logged), forwards
    `EVENT_AOD` to `refresh()`, holds the pulse wake lock as `ToyLoop` asks, and gives it
    `BatteryLevel.read` for the battery level. Each toy only
    gives its `faces(size)`, a flow that emits again when the toy's options change.
  - `MatrixArbiter` – shares the SDK's singleton manager between toys: `init` for the first bound
    toy, `turnOff` + `unInit` after the last, and only the most recently bound toy draws. The
    Glyph service may bind a newly picked toy before unbinding the old one; without this the new
    toy stayed blank and the old one's `unInit` tore down the shared connection.
  - `RoundMatrix` – the round LED mask every face draws inside.
- `glyph/addon/` – the [addons](#addons), drawn on top of any toy and switched on for all of them:
  - `AddonPreferences` – the addons (`AddonId`) and their categories (`AddonCategory`), which are
    on (one per category), and the pulse and battery settings (`PulseSettings`:
    steps, repeat, battery saver; `BatterySettings`; `BinaryBatterySettings`), persisted.
  - `NotificationPulse` – the pulse ring, and `ToyFace.frame(time, is24Hour, pulseLevel, battery, …)`,
    the full frame a toy shows: face plus addons. The toy services and the in-app previews both
    draw through it, so a preview is exactly what the matrix shows.
  - `PulseWave` – the ring level over a run of `PulseStep`s; `MinuteCheck`, battery saver's
    once-a-minute look at the notifications.
  - `BatteryBar` – the battery level's dots; `BinaryBattery` – the binary battery's 5x2 block.
  - `AddonLayout` – where each addon draws on a face (the battery addon, and the pulse ring
    around it), worked out once per face rather than per frame; `BatteryGauge`, a battery addon's
    settings with the battery to show; `EdgeBlock`, the edge lines both battery addons sit on.
- `core/battery/BatteryLevel` – the phone's battery (`BatteryState`: charge, charging, battery
  saver) from the sticky `ACTION_BATTERY_CHANGED` broadcast and `PowerManager`: `read()` for the
  toys, `changes()` for the app's screens.

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
shows every [addon](#addons) that is switched on. In the app, the **Glyph** tab's **Toys** sub-tab
lists them; tapping one opens its settings, with a **Preview** button.

To use one on the Phone (4a) Pro: Settings > Glyph Interface > Flip to Glyph > Always-on Glyph
Toy > the toy's name, then flip the phone. Changes to a toy's settings or addons in the app show
on the matrix straight away.

### Digital clock
Hours over minutes, each line centred on its own. The edge is left free for addons. Before addons this was the "Pulse clock", with the notification pulse built in; the pulse
is now an addon, on by default.

Options (`DigitalClockSettings`), applied to the matrix as soon as they change:
- **Font** (`ClockFont`, saved as `font`, the entry name):
  - `STANDARD` (default) – full 3x5 digits (`DotFontSmall`) on the 13x13 matrix, 5x7 (`DotFont`)
    on a 25x25.
  - `MINIMAL` – `DotFontMinimal` at every size: 3x5 strokes with the corners left open and a
    one-column `1`, so it lights as few LEDs as it can (about a third fewer than `STANDARD`;
    `11:11` is 20 LEDs instead of 32) and is narrower when there are 1s.

  Either way the face occupies the same box (the widest two digits, 7x11 on the 13x13), so addons
  don't move when the font or the time changes.

  To add a font: a `DotTypeface` in `dotmatrix/DotFont.kt`, then an entry at the end of
  `ClockFont` (label, description, `typeface(size)`). The options page lists every entry, and the
  face sizes its box from the font, so nothing else needs to change. Don't rename entries.
- **Brightness** – of the digits, 1..100 % (default 100 %, the full brightness the clock always
  had), turned into a matrix level by `dotmatrix/Brightness` and rescaled to the hardware's
  0..4095. It never goes to 0, which would leave only the addons. Addons keep their own brightness
  settings (the pulse steps, the battery bar's charge and used-up brightness). Saved as
  `brightness_percent` and always brought within 1..100 (`DigitalClockSettings.normalized`).

- `glyph/toy/ClockFace` – the frame.
- `glyph/toy/ClockFont` – the fonts the clock can use.
- `glyph/toy/ClockToyService` – the toy service. It keeps its old class name because the phone
  remembers the chosen toy by component name; renaming it would quietly unselect the toy. Its face
  flow emits again whenever the settings change.
- `glyph/toy/DigitalClockPreferences` – the settings, persisted.

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
- **Active / inactive brightness** – 0..100 % (defaults 100 % and 10 %), turned into matrix
  levels by `dotmatrix/Brightness` and rescaled to the hardware's 0..4095. Inactive 0 leaves clear
  bits dark, so only the set bits show. Values saved on the old 0..255 scale are converted once.

- `glyph/toy/BinaryClockFace` – the frame.
- `glyph/toy/BinaryClockToyService` – the toy service. Its face flow emits again whenever the
  settings change.
- `glyph/toy/BinaryClockPreferences` – the settings, persisted.

No notification access is needed unless the notification pulse is on.

## Addons
Addons draw on top of whichever toy is showing, only outside the area its face occupies. Each is
switched on or off once, for every toy: there is no per-toy choice, so picking another toy on the
phone keeps the same addons. In the app, the **Glyph** tab's **Addons** sub-tab lists them by
category, each card with its switch; tapping one opens its settings, with the same switch at the
top (and, for the pulse, a **Test on phone** button). A toy's page lists the addons as on or off
and opens their pages, but has no switches of its own.

Each addon belongs to a category (`AddonCategory`), and at most one addon of each is on: switching
one on switches the other of its category off (`AddonPreferences.setEnabled`). More categories can
be added to the enum; the lists follow it.

Saved as `on_<addon>` (e.g. `on_binary_battery`), every addon's switch at once. Before this, each
toy had its own switches (`toys_<addon>`, the names of the toys it was on for). Until addons are
switched again, that is read as on if it was on for any toy, so nothing that showed before
disappears; where that gives two of a category (say Battery level on one toy and Binary battery on
the other), the first in `AddonId` order stays on.

| Category | Addons |
| --- | --- |
| Notification | Notification pulse |
| Battery | Battery level, Binary battery |

### Notification pulse
When an alerting notification arrives, the outer ring of the matrix (the edge LEDs the face leaves
free) plays a pulse pattern you build from steps, a set number of times. Then the toy goes back to
steady. The settings are shared by every toy. Every brightness in the app is 0..100 %, and every
time goes in steps of 0.1 s.

- **Switch** – on by default, as the old Pulse clock had it built in. Switching it off mid-pulse
  stops the pulse at once.
- **Steps** – up to 8 (`PulseSettings.MAX_STEPS`). Each has a brightness and a time to reach it,
  0.1–10 s; it eases (cosine in-out) from the brightness the step before reached, dark for the
  first. The last step is fixed at 0 % with only its time to set, so every run ends dark. Add steps
  (they go in before the fade out) or remove any but the last two. A graph above shows one run.
  Default: 100 % in 1 s, back to 0 % in 1 s, the breath the pulse always had.
- **Repeat** – how many times the steps play, 1–100 (default 15). The old "pulse for" window
  carries over as the number of 2 s default breaths that filled it.
- Saved as `pulse_steps` ("brightness@millis;…") and `pulse_repeat`, and always brought within the
  limits above (`PulseSettings.normalized`).
- **Battery saver** – off by default. On, a notification no longer starts the pulse by itself:
  the toy only looks at notifications on the first repaint of each minute (the clock update, from
  the loop's own minute timer or `EVENT_AOD`), and pulses then if one arrived since the last look
  (at most two minutes back, so a first look after a gap doesn't pulse for old news). The pulse
  runs alongside the clock update instead of waking the phone up a second time.
- **Pulse for old ones too** – needs battery saver. Pulses every minute while a counted
  notification (not ours, not ongoing, not silent, not held back by Do Not Disturb) is still in the
  shade, new or not, until they are dismissed.
- **Notification access** – the app only learns about notifications through
  `GlyphNotificationListener`, shown in system settings as "Glyph notification pulse".
  Without access the ring never pulses.
- **Wake lock** – while a pulse runs, `GlyphToyService` holds a partial
  wake lock (not reference counted) for what is left of the pulse, as `ToyLoop` reports it, so the
  animation keeps running with the screen off. It is let go when the pulse ends, when the addon is
  switched off, and when the toy stops. With the pulse off, no toy takes it.
- **Test on phone** starts a pulse without waiting for a notification (at the next minute with
  battery saver).
- In the app, the toy pages' live matrix follows the same settings, battery saver included; the
  **Preview** sheet's simulated notification always pulses at once.

- `glyph/addon/NotificationPulse` – the ring: the round matrix's edge minus what the face
  occupies (`ToyLoop` works it out once per face, not per frame). Also `frame()`, which lays it
  over the face.
- `glyph/addon/PulseWave` – the ring level at any point of a run of steps. `MinuteCheck` there is battery
  saver: the first call each minute is the look, and it says when the latest pulse started.
- `glyph/addon/AddonPreferences` – the switch and `PulseSettings`, persisted.
- `glyph/toy/ToyLoop` – animates the ring at 25 fps while a pulse runs.
- `core/notifications/GlyphNotificationListener` – stamps `RecentNotifications` when an
  alerting notification arrives. Skipped: our own, ongoing (media, downloads), silent channels,
  ones Do Not Disturb blocks, and `FLAG_ONLY_ALERT_ONCE` updates to one already showing. Also
  keeps `RecentNotifications.anyShowing`, whether a counted notification is still in the shade.

#### Using it on the Phone (4a) Pro
1. Install and open the app: Glyph > Addons > Notification pulse > **Grant access**, and allow
   notification access. On a sideloaded build Android may block this as a restricted setting:
   App info > ⋮ > Allow restricted settings, then try again.
2. Check the pulse is switched on (Glyph > Addons), and pick one of the app's toys on the phone
   (see [toys](#toys)).
3. Flip the phone. **Test on phone** starts a pulse without waiting for a notification.

### Battery level
A line of five dots on one edge of the matrix shows the phone's charge. Each dot is 20% and stays
lit until its share is used up: 81–100% lights all five, 1–20% one. The settings are shared by
every toy; there is no permission to grant.

- **Switch** – off to begin with. Switching it on switches Binary battery off.
- **Edge** – top (default), bottom, left or right: the outermost line of LEDs on that side. On the
  13x13 matrix that line is exactly five LEDs (e.g. the top row is x 4..8); on a 25x25, the bar
  takes the middle five of it. No face draws on the edge, so the bar never covers one.
- **Drains** – top and bottom only: **right to left** (default; the right-hand dot goes out first,
  the left-hand one last) or **left to right**. The sides always drain from the top down, like a
  battery standing upright.
- **Charge / used-up brightness** – 1..100 % (default 100 %) for the dots still holding charge,
  and 0..100 % (default 10 %) for the used-up ones; 0 % leaves them dark so only the charge shows.
  Values saved on the old 0..255 scale are converted once.
- **Fade the highest dot** – off by default. On, the highest lit dot shows how much of its 20% is
  left: from the charge brightness when its share is full, down towards the used-up brightness as
  it runs out (linear in the brightness setting). At 80% the fourth dot is at full charge
  brightness, at 70% halfway, at 61% nearly used up; at 81% the fifth dot is just above used up.
  With used-up at 0 % that is the plain fraction: 70% puts the fourth dot at 50 %. Starting from
  the used-up brightness rather than 0 keeps a part-charged dot from looking dimmer than an empty
  one. The level is still read once a minute, so the fade moves in 1% steps with the clock.
- **With the notification pulse** – the pulse ring leaves out the bar's dots (`AddonLayout`), so
  a pulse runs around the bar and never hides the level.
- **When it updates** – the toy reads the level (the last sticky battery broadcast, no receiver)
  on each repaint that isn't a pulse frame: the minute's clock update, `EVENT_AOD`, or a settings
  change. So it never wakes the phone for the battery, at the cost of showing a new level up to a
  minute late. During a long pulse it reads once more when the minute changes, not per frame.
- In the app, the addon's page has a preview with a level slider (it only moves the preview), and
  the toy pages and the preview sheet show the bar at the phone's real level.

- `glyph/addon/BatteryBar` – which LEDs make the bar and in what order they light (`cells`), how
  many are lit (`lit`), how full the highest one is (`highestDotCharge`), and `draw`.
- `glyph/addon/AddonLayout` – the bar and the pulse ring for a face.
- `core/battery/BatteryLevel` – `read()` for the toys; `changes()`, a receiver-backed flow, for
  the app's screens while they show.

### Binary battery
The exact percentage in binary-coded decimal on a 5x2 block at one edge, read like the
[Binary clock](#binary-clock), with two status dots. In the Battery category with Battery level, so
one or the other is on. Off to begin with; no permission to grant.

- **Reading it** – one line is the tens (0..10), the other the units (0..9), each as bits worth 8,
  4, 2 and 1, then a status dot. Add up the lit bits of each line. The tens line's status dot is
  lit while charging (charging, or full on the charger), the units line's while battery saver is
  on. 73% charging is `0111 1` over `0011 0`; 100% is `1010` over `0000`.
- **Edge** – left (default), right, top or bottom. At the top or bottom the lines are rows on the
  outermost two lines, tens above units, bits left to right and the status dot on the right; on a
  side they are columns, tens left of units, bits top to bottom and the status dot at the bottom.
  On the 13x13 matrix the block takes the middle five of each line (e.g. columns 0–1, rows 4–8 on
  the left); on a 25x25, the middle five of its outermost line.
- **When it doesn't fit** – the Digital clock's digits take rows 1–11, so there is no room for a
  second row at the top or bottom. On a face like that the block goes on the first edge of left,
  right, top, bottom that fits (`BinaryBattery.placement`), and the toy's page and the addon's page
  say so. The Binary clock leaves room on every edge.
- **Set / clear bit brightness** – 1..100 % (default 100 %) and 0..100 % (default 10 %). The status
  dots use the same two.
- **With the notification pulse** – the ring leaves out the block's dots, as with Battery level.
- **When it updates** – like Battery level: read with the minute's repaint (and `EVENT_AOD` and
  settings changes), never per pulse frame. Charging and battery saver come from the same read,
  so they too show within a minute. `PowerManager.isPowerSaveMode` is read along with the sticky
  battery broadcast.
- In the app, the addon's page has a preview with a level slider and **Charging** / **Battery
  saver** chips (they only change the preview).

- `glyph/addon/BinaryBattery` – where the block goes (`placement`, `cells`), which dots are on
  (`dots`), and `draw`.

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
