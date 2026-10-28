package io.github.hassanm0301.nothingisfree.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hassanm0301.nothingisfree.core.notifications.RecentNotifications
import io.github.hassanm0301.nothingisfree.glyph.toy.ClockToyPreferences
import io.github.hassanm0301.nothingisfree.ui.theme.ThemeMode
import io.github.hassanm0301.nothingisfree.ui.theme.ThemePreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime

class GalleryViewModel : ViewModel() {

    /** Wall-clock, ticking once a second while something is collecting it. */
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(1_000L - (System.currentTimeMillis() % 1_000L))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDateTime.now())

    val themeMode: StateFlow<ThemeMode> = ThemePreferences.mode

    fun setThemeMode(mode: ThemeMode) = ThemePreferences.set(mode)

    /** Last alerting notification, as seen by the Glyph clock toy. */
    val lastAlertAt: StateFlow<Long?> = RecentNotifications.lastAlertAt

    val pulseWindowSeconds: StateFlow<Int> = ClockToyPreferences.pulseWindowSeconds

    fun setPulseWindowSeconds(seconds: Int) = ClockToyPreferences.setPulseWindowSeconds(seconds)

    /** Pretend a notification just arrived, to try the pulse on the toy and in the preview. */
    fun testPulse() = RecentNotifications.record(System.currentTimeMillis())
}
