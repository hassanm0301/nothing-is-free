package io.github.hassanm0301.nothingisfree.glyph.toy

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.hassanm0301.nothingisfree.dotmatrix.Brightness
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

/** How each bit of the binary clock is drawn on the matrix. */
enum class BitStyle {
    /** One LED per bit. */
    DOT,

    /** A 2x2 block of LEDs per bit, blocks touching. */
    SQUARE,
}

/**
 * Options for the binary clock toy. Brightness values are 0..100 %; the face turns them into
 * `DotMatrix` levels with `Brightness.level`.
 *
 * @property dotGap in [BitStyle.DOT], leave one dark LED between bits. Ignored for squares, which
 *   always touch: with gaps they no longer fit the round 13x13 matrix.
 * @property showUnusedBits light BCD bits a digit can never use (the 4 and 8 of the hours tens,
 *   the 8 of the minutes tens) at [inactiveBrightness], instead of leaving them dark.
 */
data class BinaryClockSettings(
    val style: BitStyle = BitStyle.SQUARE,
    val dotGap: Boolean = true,
    val showUnusedBits: Boolean = false,
    val activeBrightness: Int = 100,
    val inactiveBrightness: Int = 10,
)

/**
 * Persisted [BinaryClockSettings], shared by the app UI and the toy service (same process).
 * [init] runs from `NothingIsFreeApp.onCreate`.
 */
object BinaryClockPreferences {
    private const val KEY_STYLE = "style"
    private const val KEY_DOT_GAP = "dot_gap"
    private const val KEY_SHOW_UNUSED = "show_unused_bits"
    private const val KEY_ACTIVE = "active_brightness_percent"
    private const val KEY_INACTIVE = "inactive_brightness_percent"

    /** Brightness saved on the old 0..255 scale, converted the first time these are read. */
    private const val KEY_LEGACY_ACTIVE = "active_brightness"
    private const val KEY_LEGACY_INACTIVE = "inactive_brightness"

    private var prefs: SharedPreferences? = null
    private val _settings = MutableStateFlow(BinaryClockSettings())
    val settings: StateFlow<BinaryClockSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("glyph_binary_clock", Context.MODE_PRIVATE)
        prefs = p
        val d = BinaryClockSettings()
        _settings.value = BinaryClockSettings(
            style = p.getString(KEY_STYLE, null)?.let { s -> BitStyle.entries.firstOrNull { it.name == s } } ?: d.style,
            dotGap = p.getBoolean(KEY_DOT_GAP, d.dotGap),
            showUnusedBits = p.getBoolean(KEY_SHOW_UNUSED, d.showUnusedBits),
            activeBrightness = percent(p, KEY_ACTIVE, KEY_LEGACY_ACTIVE, d.activeBrightness),
            inactiveBrightness = percent(p, KEY_INACTIVE, KEY_LEGACY_INACTIVE, d.inactiveBrightness),
        )
    }

    fun update(transform: (BinaryClockSettings) -> BinaryClockSettings) {
        val s = _settings.updateAndGet(transform)
        prefs?.edit {
            putString(KEY_STYLE, s.style.name)
            putBoolean(KEY_DOT_GAP, s.dotGap)
            putBoolean(KEY_SHOW_UNUSED, s.showUnusedBits)
            putInt(KEY_ACTIVE, s.activeBrightness.coerceIn(0, 100))
            putInt(KEY_INACTIVE, s.inactiveBrightness.coerceIn(0, 100))
        }
    }

    private fun percent(p: SharedPreferences, key: String, legacyKey: String, default: Int): Int = when {
        p.contains(key) -> p.getInt(key, default).coerceIn(0, 100)
        p.contains(legacyKey) -> Brightness.percent(p.getInt(legacyKey, 0))
        else -> default
    }
}
