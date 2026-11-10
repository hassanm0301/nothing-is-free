package io.github.hassanm0301.nothingisfree.glyph.toy

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

/**
 * Options for the Compact digital clock toy.
 *
 * @property font the digits are drawn in; see [CompactClockFont].
 */
data class CompactClockSettings(val font: CompactClockFont = CompactClockFont.LARGE)

/**
 * Persisted [CompactClockSettings], shared by the app UI and the toy service (same process).
 * [init] runs from `NothingIsFreeApp.onCreate`.
 */
object CompactClockPreferences {
    private const val KEY_FONT = "font"

    private var prefs: SharedPreferences? = null
    private val _settings = MutableStateFlow(CompactClockSettings())
    val settings: StateFlow<CompactClockSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("glyph_compact_clock", Context.MODE_PRIVATE)
        prefs = p
        val d = CompactClockSettings()
        _settings.value = CompactClockSettings(
            font = p.getString(KEY_FONT, null)?.let { s -> CompactClockFont.entries.firstOrNull { it.name == s } } ?: d.font,
        )
    }

    fun update(transform: (CompactClockSettings) -> CompactClockSettings) {
        val s = _settings.updateAndGet(transform)
        prefs?.edit { putString(KEY_FONT, s.font.name) }
    }
}
