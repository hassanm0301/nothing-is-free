package io.github.hassanm0301.nothingisfree.glyph.toy

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

/**
 * Options for the Digital clock toy.
 *
 * @property brightness of the digits, in [BRIGHTNESS_RANGE] %; the face turns it into a
 *   `DotMatrix` level with `Brightness.level`. Addons keep their own brightness settings. Never 0:
 *   the clock would vanish.
 */
data class DigitalClockSettings(val brightness: Int = 100) {

    /** These settings brought within their limits. */
    fun normalized(): DigitalClockSettings = copy(brightness = brightness.coerceIn(BRIGHTNESS_RANGE))

    companion object {
        val BRIGHTNESS_RANGE = 1..100
    }
}

/**
 * Persisted [DigitalClockSettings], shared by the app UI and the toy service (same process).
 * [init] runs from `NothingIsFreeApp.onCreate`.
 */
object DigitalClockPreferences {
    private const val KEY_BRIGHTNESS = "brightness_percent"

    private var prefs: SharedPreferences? = null
    private val _settings = MutableStateFlow(DigitalClockSettings())
    val settings: StateFlow<DigitalClockSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("glyph_digital_clock", Context.MODE_PRIVATE)
        prefs = p
        val d = DigitalClockSettings()
        _settings.value = DigitalClockSettings(
            brightness = p.getInt(KEY_BRIGHTNESS, d.brightness),
        ).normalized()
    }

    fun update(transform: (DigitalClockSettings) -> DigitalClockSettings) {
        val s = _settings.updateAndGet { transform(it).normalized() }
        prefs?.edit { putInt(KEY_BRIGHTNESS, s.brightness) }
    }
}
