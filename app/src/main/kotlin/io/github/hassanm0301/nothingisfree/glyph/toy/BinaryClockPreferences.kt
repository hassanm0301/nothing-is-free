package io.github.hassanm0301.nothingisfree.glyph.toy

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
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
 * Options for the binary clock toy. Brightness values are 0..255, the `DotMatrix` scale; the toy
 * rescales to the hardware's 0..4095.
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
    val activeBrightness: Int = 255,
    val inactiveBrightness: Int = 24,
)

/**
 * Persisted [BinaryClockSettings], shared by the app UI and the toy service (same process).
 * [init] runs from `NothingIsFreeApp.onCreate`.
 */
object BinaryClockPreferences {
    private const val KEY_STYLE = "style"
    private const val KEY_DOT_GAP = "dot_gap"
    private const val KEY_SHOW_UNUSED = "show_unused_bits"
    private const val KEY_ACTIVE = "active_brightness"
    private const val KEY_INACTIVE = "inactive_brightness"

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
            activeBrightness = p.getInt(KEY_ACTIVE, d.activeBrightness).coerceIn(0, 255),
            inactiveBrightness = p.getInt(KEY_INACTIVE, d.inactiveBrightness).coerceIn(0, 255),
        )
    }

    fun update(transform: (BinaryClockSettings) -> BinaryClockSettings) {
        val s = _settings.updateAndGet(transform)
        prefs?.edit {
            putString(KEY_STYLE, s.style.name)
            putBoolean(KEY_DOT_GAP, s.dotGap)
            putBoolean(KEY_SHOW_UNUSED, s.showUnusedBits)
            putInt(KEY_ACTIVE, s.activeBrightness.coerceIn(0, 255))
            putInt(KEY_INACTIVE, s.inactiveBrightness.coerceIn(0, 255))
        }
    }
}
