package io.github.hassanm0301.nothingisfree.glyph.addon

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet

/**
 * Addons draw on top of any Glyph Toy. Each is switched on per toy. Persisted by [name], so
 * don't rename entries.
 */
enum class AddonId(val title: String, val summary: String) {
    NOTIFICATION_PULSE(
        "Notification pulse",
        "The outer ring breathes for a while after a notification",
    ),
}

/**
 * Addon settings, shared by the app UI and the toy services (same process). [init] runs from
 * `NothingIsFreeApp.onCreate`.
 */
object AddonPreferences {
    /** Choices offered in the UI for how long after a notification the ring keeps pulsing. */
    val pulseWindowChoices = listOf(10, 30, 60, 120, 300)
    const val DEFAULT_PULSE_WINDOW_SECONDS = 30

    /** Where each addon starts out on: the pulse on the Digital clock, as before addons existed. */
    private val defaults = mapOf(AddonId.NOTIFICATION_PULSE to setOf(ToyId.DIGITAL_CLOCK))

    private const val KEY_PULSE_WINDOW = "pulse_window_seconds"
    private fun keyToys(addon: AddonId) = "toys_${addon.name.lowercase()}"

    private var prefs: SharedPreferences? = null
    private val _enabled = MutableStateFlow(defaults)
    private val _pulseWindowSeconds = MutableStateFlow(DEFAULT_PULSE_WINDOW_SECONDS)

    /** For each addon, the toys it is switched on for. */
    val enabled: StateFlow<Map<AddonId, Set<ToyId>>> = _enabled.asStateFlow()

    /** Pulse when a notification arrived within the last this-many seconds. */
    val pulseWindowSeconds: StateFlow<Int> = _pulseWindowSeconds.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("glyph_addons", Context.MODE_PRIVATE)
        prefs = p
        _enabled.value = AddonId.entries.associateWith { addon ->
            p.getStringSet(keyToys(addon), null)
                ?.mapNotNull { name -> ToyId.entries.firstOrNull { it.name == name } }?.toSet()
                ?: defaults[addon].orEmpty()
        }
        // Before addons, the window belonged to the Pulse clock toy. Until a window is picked here,
        // keep using that one (the old prefs file is never deleted).
        val legacy = context.getSharedPreferences("glyph_clock_toy", Context.MODE_PRIVATE)
        _pulseWindowSeconds.value = when {
            p.contains(KEY_PULSE_WINDOW) -> p.getInt(KEY_PULSE_WINDOW, DEFAULT_PULSE_WINDOW_SECONDS)
            else -> legacy.getInt(KEY_PULSE_WINDOW, DEFAULT_PULSE_WINDOW_SECONDS)
        }
    }

    fun isEnabled(addon: AddonId, toy: ToyId): Boolean = toy in _enabled.value[addon].orEmpty()

    /** Whether [addon] is on for [toy], as it changes. */
    fun enabledFlow(addon: AddonId, toy: ToyId): Flow<Boolean> =
        enabled.map { toy in it[addon].orEmpty() }.distinctUntilChanged()

    fun setEnabled(addon: AddonId, toy: ToyId, on: Boolean) {
        val all = _enabled.updateAndGet { current ->
            val toys = current[addon].orEmpty()
            current + (addon to if (on) toys + toy else toys - toy)
        }
        prefs?.edit { putStringSet(keyToys(addon), all[addon].orEmpty().map { it.name }.toSet()) }
    }

    fun setPulseWindowSeconds(seconds: Int) {
        _pulseWindowSeconds.value = seconds
        prefs?.edit { putInt(KEY_PULSE_WINDOW, seconds) }
    }
}
