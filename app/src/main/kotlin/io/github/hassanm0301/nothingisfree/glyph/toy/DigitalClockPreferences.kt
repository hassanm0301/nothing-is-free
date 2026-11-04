package io.github.hassanm0301.nothingisfree.glyph.toy

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

/** Options for the Digital clock toy. */
data class DigitalClockSettings(
    val font: ClockFont = ClockFont.STANDARD,
)

/**
 * Persisted [DigitalClockSettings], shared by the app UI and the toy service (same process).
 * [init] runs from `NothingIsFreeApp.onCreate`.
 */
object DigitalClockPreferences {
    private const val KEY_FONT = "font"

    private var prefs: SharedPreferences? = null
    private val _settings = MutableStateFlow(DigitalClockSettings())
    val settings: StateFlow<DigitalClockSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("glyph_digital_clock", Context.MODE_PRIVATE)
        prefs = p
        val d = DigitalClockSettings()
        _settings.value = DigitalClockSettings(
            font = p.getString(KEY_FONT, null)?.let { s -> ClockFont.entries.firstOrNull { it.name == s } } ?: d.font,
        )
    }

    fun update(transform: (DigitalClockSettings) -> DigitalClockSettings) {
        val s = _settings.updateAndGet(transform)
        prefs?.edit { putString(KEY_FONT, s.font.name) }
    }
}
