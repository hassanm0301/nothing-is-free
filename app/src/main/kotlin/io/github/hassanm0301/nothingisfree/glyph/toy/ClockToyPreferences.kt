package io.github.hassanm0301.nothingisfree.glyph.toy

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Settings for the Glyph clock toy. Persisted, because the toy service can be bound long after
 * the app UI that changed them has gone. [init] runs from `NothingIsFreeApp.onCreate`.
 */
object ClockToyPreferences {
    /** Choices offered in the UI for how long after a notification the ring keeps pulsing. */
    val pulseWindowChoices = listOf(10, 30, 60, 120, 300)
    const val DEFAULT_PULSE_WINDOW_SECONDS = 30

    private const val KEY_PULSE_WINDOW = "pulse_window_seconds"

    private var prefs: SharedPreferences? = null
    private val _pulseWindowSeconds = MutableStateFlow(DEFAULT_PULSE_WINDOW_SECONDS)

    /** Pulse when a notification arrived within the last this-many seconds. */
    val pulseWindowSeconds: StateFlow<Int> = _pulseWindowSeconds.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("glyph_clock_toy", Context.MODE_PRIVATE)
        prefs = p
        _pulseWindowSeconds.value = p.getInt(KEY_PULSE_WINDOW, DEFAULT_PULSE_WINDOW_SECONDS)
    }

    fun setPulseWindowSeconds(seconds: Int) {
        _pulseWindowSeconds.value = seconds
        prefs?.edit { putInt(KEY_PULSE_WINDOW, seconds) }
    }
}
