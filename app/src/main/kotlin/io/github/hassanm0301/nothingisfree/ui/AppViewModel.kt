package io.github.hassanm0301.nothingisfree.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hassanm0301.nothingisfree.NothingIsFreeApp
import io.github.hassanm0301.nothingisfree.core.notifications.RecentNotifications
import io.github.hassanm0301.nothingisfree.core.update.AppUpdater
import io.github.hassanm0301.nothingisfree.core.update.Release
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonId
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonPreferences
import io.github.hassanm0301.nothingisfree.glyph.toy.BinaryClockPreferences
import io.github.hassanm0301.nothingisfree.glyph.toy.BinaryClockSettings
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyId
import io.github.hassanm0301.nothingisfree.ui.theme.ThemeMode
import io.github.hassanm0301.nothingisfree.ui.theme.ThemePreferences
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

    /** For each addon, the toys it is on for. */
    val addons: StateFlow<Map<AddonId, Set<ToyId>>> = AddonPreferences.enabled

    fun setAddon(addon: AddonId, toy: ToyId, on: Boolean) = AddonPreferences.setEnabled(addon, toy, on)

    val pulseWindowSeconds: StateFlow<Int> = AddonPreferences.pulseWindowSeconds

    fun setPulseWindowSeconds(seconds: Int) = AddonPreferences.setPulseWindowSeconds(seconds)

    /** Pretend a notification just arrived, so the pulse runs on the phone's matrix. */
    fun testPulseOnPhone() = RecentNotifications.record(System.currentTimeMillis())

    /** Binary clock toy options; the toy redraws as soon as they change. */
    val binaryClock: StateFlow<BinaryClockSettings> = BinaryClockPreferences.settings

    fun updateBinaryClock(transform: (BinaryClockSettings) -> BinaryClockSettings) =
        BinaryClockPreferences.update(transform)

    /** Self-update from GitHub releases; checked once each time the view model is created. */
    val update: StateFlow<AppUpdater.State> = AppUpdater.state

    init {
        checkForUpdate()
    }

    fun checkForUpdate() = AppUpdater.check(NothingIsFreeApp.instance)

    fun installUpdate(release: Release) = AppUpdater.install(NothingIsFreeApp.instance, release)
}
