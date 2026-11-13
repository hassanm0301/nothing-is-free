package io.github.hassanm0301.nothingisfree.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hassanm0301.nothingisfree.NothingIsFreeApp
import io.github.hassanm0301.nothingisfree.core.battery.BatteryLevel
import io.github.hassanm0301.nothingisfree.core.battery.BatteryState
import io.github.hassanm0301.nothingisfree.core.notifications.RecentNotifications
import io.github.hassanm0301.nothingisfree.core.update.AppUpdater
import io.github.hassanm0301.nothingisfree.core.update.Release
import io.github.hassanm0301.nothingisfree.core.update.UpdatePreferences
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonId
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonPreferences
import io.github.hassanm0301.nothingisfree.glyph.addon.BatteryRingSettings
import io.github.hassanm0301.nothingisfree.glyph.addon.BatterySettings
import io.github.hassanm0301.nothingisfree.glyph.addon.BinaryBatterySettings
import io.github.hassanm0301.nothingisfree.glyph.addon.PulseSettings
import io.github.hassanm0301.nothingisfree.glyph.toy.AnalogClockPreferences
import io.github.hassanm0301.nothingisfree.glyph.toy.AnalogClockSettings
import io.github.hassanm0301.nothingisfree.glyph.toy.BinaryClockPreferences
import io.github.hassanm0301.nothingisfree.glyph.toy.BinaryClockSettings
import io.github.hassanm0301.nothingisfree.glyph.toy.CompactClockPreferences
import io.github.hassanm0301.nothingisfree.glyph.toy.CompactClockSettings
import io.github.hassanm0301.nothingisfree.glyph.toy.DigitalClockPreferences
import io.github.hassanm0301.nothingisfree.glyph.toy.DigitalClockSettings
import io.github.hassanm0301.nothingisfree.ui.theme.ThemeMode
import io.github.hassanm0301.nothingisfree.ui.theme.ThemePreferences
import io.github.hassanm0301.nothingisfree.volume.PreciseVolumePreferences
import io.github.hassanm0301.nothingisfree.volume.VolumeFormat
import io.github.hassanm0301.nothingisfree.widget.ClockWidget
import io.github.hassanm0301.nothingisfree.widget.ClockWidgetFont
import io.github.hassanm0301.nothingisfree.widget.ClockWidgetPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime

/** State shared by the three tabs. Settings live in their own process-wide stores; this exposes them. */
class AppViewModel : ViewModel() {

    /** Wall-clock, ticking once a second while something is collecting it. */
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(1_000L - (System.currentTimeMillis() % 1_000L))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDateTime.now())

    val themeMode: StateFlow<ThemeMode> = ThemePreferences.mode

    fun setThemeMode(mode: ThemeMode) = ThemePreferences.set(mode)

    /** Last alerting notification, as seen by the notification pulse addon. */
    val lastAlertAt: StateFlow<Long?> = RecentNotifications.lastAlertAt

    /** Whether a notification the pulse counts is still in the shade. */
    val notificationsShowing: StateFlow<Boolean> = RecentNotifications.anyShowing

    /** The addons switched on, at most one per category. Each draws on whichever toy is showing. */
    val addons: StateFlow<Set<AddonId>> = AddonPreferences.enabled

    /** Switch [addon] on or off for every toy; on switches off the other of its category. */
    fun setAddon(addon: AddonId, on: Boolean) = AddonPreferences.setEnabled(addon, on)

    /** Notification pulse settings, shared by every toy. */
    val pulse: StateFlow<PulseSettings> = AddonPreferences.pulse

    fun updatePulse(transform: (PulseSettings) -> PulseSettings) = AddonPreferences.updatePulse(transform)

    /** Pretend a notification just arrived, so the pulse runs on the phone's matrix. */
    fun testPulseOnPhone() = RecentNotifications.record(System.currentTimeMillis())

    /** Battery level addon settings, shared by every toy. */
    val battery: StateFlow<BatterySettings> = AddonPreferences.battery

    fun updateBattery(transform: (BatterySettings) -> BatterySettings) = AddonPreferences.updateBattery(transform)

    /** Binary battery addon settings, shared by every toy. */
    val binaryBattery: StateFlow<BinaryBatterySettings> = AddonPreferences.binaryBattery

    fun updateBinaryBattery(transform: (BinaryBatterySettings) -> BinaryBatterySettings) =
        AddonPreferences.updateBinaryBattery(transform)

    /** Battery ring addon settings, shared by every toy. */
    val batteryRing: StateFlow<BatteryRingSettings> = AddonPreferences.batteryRing

    fun updateBatteryRing(transform: (BatteryRingSettings) -> BatteryRingSettings) =
        AddonPreferences.updateBatteryRing(transform)

    /** The phone's battery (null if unknown), followed while a screen shows it. */
    val batteryState: StateFlow<BatteryState?> = BatteryLevel.changes(NothingIsFreeApp.instance)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BatteryLevel.read(NothingIsFreeApp.instance))

    /** Digital clock toy options; the toy redraws as soon as they change. */
    val digitalClock: StateFlow<DigitalClockSettings> = DigitalClockPreferences.settings

    fun updateDigitalClock(transform: (DigitalClockSettings) -> DigitalClockSettings) =
        DigitalClockPreferences.update(transform)

    /** Binary clock toy options; the toy redraws as soon as they change. */
    val binaryClock: StateFlow<BinaryClockSettings> = BinaryClockPreferences.settings

    fun updateBinaryClock(transform: (BinaryClockSettings) -> BinaryClockSettings) =
        BinaryClockPreferences.update(transform)

    /** Compact digital clock toy options; the toy redraws as soon as they change. */
    val compactClock: StateFlow<CompactClockSettings> = CompactClockPreferences.settings

    fun updateCompactClock(transform: (CompactClockSettings) -> CompactClockSettings) =
        CompactClockPreferences.update(transform)

    /** Analog clock toy options; the toy redraws as soon as they change. */
    val analogClock: StateFlow<AnalogClockSettings> = AnalogClockPreferences.settings

    fun updateAnalogClock(transform: (AnalogClockSettings) -> AnalogClockSettings) =
        AnalogClockPreferences.update(transform)

    /** How Precise volume writes the level beside the volume bar. */
    val volumeFormat: StateFlow<VolumeFormat> = PreciseVolumePreferences.format

    fun setVolumeFormat(format: VolumeFormat) = PreciseVolumePreferences.setFormat(format)

    /** The font the home-screen clock widget is drawn in. */
    val clockWidgetFont: StateFlow<ClockWidgetFont> = ClockWidgetPreferences.font

    /** Change the clock widget's font and redraw the ones on the home screen. */
    fun setClockWidgetFont(font: ClockWidgetFont) {
        ClockWidgetPreferences.setFont(font)
        ClockWidget.update(NothingIsFreeApp.instance)
    }

    /** The clock widget's text colour, a grey from 0 (black) to 100 (white). */
    val clockWidgetShade: StateFlow<Int> = ClockWidgetPreferences.shade

    /** Change the clock widget's colour and redraw the ones on the home screen. */
    fun setClockWidgetShade(shade: Int) {
        ClockWidgetPreferences.setShade(shade)
        ClockWidget.update(NothingIsFreeApp.instance)
    }

    /** Self-update from GitHub releases; checked once each time the view model is created. */
    val update: StateFlow<AppUpdater.State> = AppUpdater.state

    init {
        checkForUpdate()
        // Sets the clock widget's minute tick again if a force stop took it.
        ClockWidget.update(NothingIsFreeApp.instance)
    }

    fun checkForUpdate() = AppUpdater.check(NothingIsFreeApp.instance)

    fun installUpdate(release: Release) = AppUpdater.install(NothingIsFreeApp.instance, release)

    /** Whether updates include GitHub pre-releases. */
    val betaUpdates: StateFlow<Boolean> = UpdatePreferences.beta

    /** Switch the update channel and look again on the new one. */
    fun setBetaUpdates(on: Boolean) {
        UpdatePreferences.setBeta(on)
        checkForUpdate()
    }
}
