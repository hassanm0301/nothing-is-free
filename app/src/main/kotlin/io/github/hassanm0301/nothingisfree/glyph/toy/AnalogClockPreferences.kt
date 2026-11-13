package io.github.hassanm0301.nothingisfree.glyph.toy

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

/**
 * Options for the Analog clock toy.
 *
 * @property hourBrightness of the hour hand, in [BRIGHTNESS_RANGE] %; the face turns it into a
 *   `DotMatrix` level with `Brightness.level`. Never 0: the hand would vanish.
 * @property minuteBrightness of the minute hand, likewise. Setting the two apart tells the hands
 *   apart by more than their length.
 */
data class AnalogClockSettings(
    val hourBrightness: Int = 100,
    val minuteBrightness: Int = 100,
) {

    /** These settings brought within their limits. */
    fun normalized(): AnalogClockSettings = copy(
        hourBrightness = hourBrightness.coerceIn(BRIGHTNESS_RANGE),
        minuteBrightness = minuteBrightness.coerceIn(BRIGHTNESS_RANGE),
    )

    companion object {
        val BRIGHTNESS_RANGE = 1..100
    }
}

/**
 * Persisted [AnalogClockSettings], shared by the app UI and the toy service (same process).
 * [init] runs from `NothingIsFreeApp.onCreate`.
 */
object AnalogClockPreferences {
    private const val KEY_HOUR_BRIGHTNESS = "hour_brightness_percent"
    private const val KEY_MINUTE_BRIGHTNESS = "minute_brightness_percent"

    private var prefs: SharedPreferences? = null
    private val _settings = MutableStateFlow(AnalogClockSettings())
    val settings: StateFlow<AnalogClockSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("glyph_analog_clock", Context.MODE_PRIVATE)
        prefs = p
        val d = AnalogClockSettings()
        _settings.value = AnalogClockSettings(
            hourBrightness = p.getInt(KEY_HOUR_BRIGHTNESS, d.hourBrightness),
            minuteBrightness = p.getInt(KEY_MINUTE_BRIGHTNESS, d.minuteBrightness),
        ).normalized()
    }

    fun update(transform: (AnalogClockSettings) -> AnalogClockSettings) {
        val s = _settings.updateAndGet { transform(it).normalized() }
        prefs?.edit {
            putInt(KEY_HOUR_BRIGHTNESS, s.hourBrightness)
            putInt(KEY_MINUTE_BRIGHTNESS, s.minuteBrightness)
        }
    }
}
