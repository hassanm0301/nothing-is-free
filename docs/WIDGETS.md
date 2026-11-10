# Widgets

## Clock
A home-screen widget: the day and date, the time and the next alarm, centred, with no
background. In Ndot (the default) or NType, and in any grey from black to white (white by
default), both chosen on Widgets › Clock. Lives in `widget/`.

### How it works
- **The app draws it.** A widget's layout is inflated by the launcher, in a context that refuses
  an app's own fonts (`RemoteViews.RemoteViewsContextWrapper.isRestricted`; `TextView` then skips
  `@font` resources). A `TextClock` would tick for free but only in a system font. So
  `ClockWidgetRenderer` draws the three lines into a bitmap and the layout (`clock_widget.xml`) is
  one `ImageView`. This is also why the widgets don't use Glance: its text can't carry the fonts either.
- **Size.** The bitmap is drawn to fit the widget's portrait size, from the launcher's widget
  options, and the `ImageView` scales it for anything else. The fit is worked out as if an alarm
  were set, so the time doesn't change size when one comes or goes.
- **The minute tick.** Drawing it ourselves means redrawing it each minute. `ClockWidget.update`
  sets an exact `AlarmManager.RTC` alarm for the start of the next minute, each time it draws.
  `RTC`, not `RTC_WAKEUP`: it never wakes the phone. Asleep, nothing runs; the tick is delivered as
  the phone wakes, so the widget can be a moment behind right after unlocking. With no widget
  placed there is no tick. `USE_EXACT_ALARM` lets it be exact without asking the user.
- **The next alarm** is `AlarmManager.getNextAlarmClock()`: free, no permission. It is the next
  alarm any app set with `setAlarmClock`, usually the clock app's, though a calendar or automation
  app can set one too. `ClockWidgetProvider` takes `NEXT_ALARM_CLOCK_CHANGED`, `TIME_SET`,
  `TIMEZONE_CHANGED` and `LOCALE_CHANGED` from the manifest (all exempt from the implicit broadcast
  limits) and redraws at once.
- **Restarting.** The tick dies with a reboot, an app update or a force stop. After the first two
  the system asks every widget to update, which sets it again. After a force stop the widget stays
  as it was until the app is opened (`AppViewModel` redraws it) or the next alarm changes.
- **A tap** opens the clock app's alarms (`AlarmClock.ACTION_SHOW_ALARMS`).
- `ClockWidgetText` (the wording, and when the next minute starts) is plain Kotlin and unit tested.
  The picker's preview is `drawable-nodpi/clock_widget_preview.png`.

### Limits
- The text is one grey, set by hand: it doesn't follow the wallpaper.
- Not checked on a phone yet.

### Checking it on the phone
1. Widgets › Clock › **Add to home screen**, or add it from the launcher's widget list.
2. Watch it change on the minute; set or switch off an alarm and see the bottom line follow.
3. Lock the phone for a few minutes, unlock: it should show the right time within a second.

### More widgets
Same recipe: an `AppWidgetProvider`, a renderer that draws with the bundled fonts or `DotMatrix`
into a bitmap, an `ImageView` layout, and `res/xml/<name>_widget_info.xml`. Keep to the look in
`docs/DESIGN.md`. Anything that doesn't change by the minute needs no tick.

## Precise volume
Writes the volume level as text ("47%" or "7/15") beside the system volume bar whenever the
volume popup is up. The bar itself is left alone. Lives in `volume/`; its page is Widgets › Precise volume.

### How it works
- `PreciseVolumeService` is an accessibility service, the only kind of app component that can both
  see where the system's volume popup is and draw above it: `TYPE_ACCESSIBILITY_OVERLAY` is layered
  over `TYPE_VOLUME_OVERLAY`, while a plain app overlay sits under it.
- Changes come from `android.media.VOLUME_CHANGED_ACTION`, hidden in `AudioManager` but sent by
  AudioService on every change. It is a protected broadcast, so only the system can send it.
  Only media, ring, notification, alarm and call volume count (`VolumeStreams`); streams that
  follow one of them send their own broadcasts with the same press.
- The first press often changes nothing. With nothing playing, AudioService only opens the popup
  and drops the adjustment (`VolumeController.suppressAdjustment`), so no broadcast comes. The
  service therefore also takes window-change events: when a system window that could be the popup
  opens, it looks for the bar for 600 ms. Which stream the popup shows is then read off the bar:
  the stream whose level matches how full the slider is (media first, or call during a call).
- While a label is due, the service looks every 100 ms for the bar: system windows titled as the
  volume controls (AOSP: "Media volume controls") or, failing that, ones small enough not to be
  the shade or lock screen; in them, the first node with a range (a slider). `VolumeBar` picks
  the window and places the label on the side facing the middle of the screen, clear of the
  popup's edge. It filters no keys, and the only events it takes are window changes.
- `LabelTimeline` decides when the label shows: with the bar, hidden soon after the bar goes.
  Once the bar has been found, a change without it (an app setting the volume quietly) shows
  nothing. Until then, the label stands in at the right edge for 3 s after the last change; a
  window opening never brings the stand-in.

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
