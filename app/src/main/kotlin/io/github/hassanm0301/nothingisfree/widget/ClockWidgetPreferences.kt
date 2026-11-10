package io.github.hassanm0301.nothingisfree.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.FontRes
import androidx.core.content.edit
import io.github.hassanm0301.nothingisfree.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The Nothing typeface the clock widget is drawn in: [timeFont] for the time, [lineFont] for the other lines. */
enum class ClockWidgetFont(val title: String, @FontRes val timeFont: Int, @FontRes val lineFont: Int) {
    NDOT("Ndot", R.font.ndot57, R.font.ndot57),
    NTYPE("NType", R.font.ntype82_headline, R.font.ntype82_regular),
}

/**
 * Clock widget settings, shared by the app UI and [ClockWidgetProvider] (same process).
 * [init] runs from `NothingIsFreeApp.onCreate`, so also before the provider draws.
 */
object ClockWidgetPreferences {
    private const val KEY_FONT = "font"
    private const val KEY_SHADE = "shade"

    /** A shade is a grey, from [BLACK] to [WHITE]. */
    const val BLACK = 0
    const val WHITE = 100

    private var prefs: SharedPreferences? = null
    private val _font = MutableStateFlow(ClockWidgetFont.NDOT)

    val font: StateFlow<ClockWidgetFont> = _font.asStateFlow()

    private val _shade = MutableStateFlow(WHITE)

    /** The colour of the widget's text: a grey from [BLACK] to [WHITE]. */
    val shade: StateFlow<Int> = _shade.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("clock_widget", Context.MODE_PRIVATE)
        prefs = p
        _font.value = p.getString(KEY_FONT, null)
            ?.let { name -> ClockWidgetFont.entries.firstOrNull { it.name == name } }
            ?: ClockWidgetFont.NDOT
        _shade.value = p.getInt(KEY_SHADE, WHITE).coerceIn(BLACK, WHITE)
    }

    fun setShade(shade: Int) {
        val s = shade.coerceIn(BLACK, WHITE)
        _shade.value = s
        prefs?.edit { putInt(KEY_SHADE, s) }
    }

    fun setFont(font: ClockWidgetFont) {
        _font.value = font
        prefs?.edit { putString(KEY_FONT, font.name) }
    }
}
