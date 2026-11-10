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

    private var prefs: SharedPreferences? = null
    private val _font = MutableStateFlow(ClockWidgetFont.NDOT)

    val font: StateFlow<ClockWidgetFont> = _font.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("clock_widget", Context.MODE_PRIVATE)
        prefs = p
        _font.value = p.getString(KEY_FONT, null)
            ?.let { name -> ClockWidgetFont.entries.firstOrNull { it.name == name } }
            ?: ClockWidgetFont.NDOT
    }

    fun setFont(font: ClockWidgetFont) {
        _font.value = font
        prefs?.edit { putString(KEY_FONT, font.name) }
    }
}
