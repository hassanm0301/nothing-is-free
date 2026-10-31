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
        "The outer ring pulses a few times after a notification",
    ),
}

/**
 * How the notification pulse looks and when it runs. One setting for every toy.
 *
 * @property riseMs how long each breath takes to go from dark to [maxBrightness]; it takes as long
 *   again to fade back, so a breath lasts twice this.
 * @property maxBrightness the peak of each breath, 1..255 on the `DotMatrix` scale.
 * @property count how many breaths ("pulses" in the UI) follow a notification.
 * @property batterySaver look for notifications once a minute, with the clock's own repaint,
 *   instead of waking for each one as it arrives (see [MinuteCheck]).
 * @property repeatWhileShowing with [batterySaver], pulse every minute while a notification is still
 *   in the shade, not only when a new one has arrived.
 */
data class PulseSettings(
    val riseMs: Long = 1_000,
    val maxBrightness: Int = 255,
    val count: Int = 15,
    val batterySaver: Boolean = false,
    val repeatWhileShowing: Boolean = false,
) {
    val durationMs: Long get() = PulseWave.duration(riseMs, count)

    /** Ring brightness [elapsedMs] into a pulse. */
    fun level(elapsedMs: Long): Int = PulseWave.level(elapsedMs, riseMs, maxBrightness)
}

/**
 * Addon settings, shared by the app UI and the toy services (same process). [init] runs from
 * `NothingIsFreeApp.onCreate`.
 */
object AddonPreferences {
    /** Choices offered in the UI for [PulseSettings.riseMs]. */
    val riseChoices = listOf(500L, 1_000L, 1_500L, 2_000L, 3_000L)

    /** Choices offered in the UI for [PulseSettings.count]. */
    val countChoices = listOf(1, 3, 5, 10, 15, 30, 60)

    /** Where each addon starts out on: the pulse on the Digital clock, as before addons existed. */
    private val defaults = mapOf(AddonId.NOTIFICATION_PULSE to setOf(ToyId.DIGITAL_CLOCK))

    private const val KEY_RISE = "pulse_rise_ms"
    private const val KEY_MAX_BRIGHTNESS = "pulse_max_brightness"
    private const val KEY_COUNT = "pulse_count"
    private const val KEY_BATTERY_SAVER = "pulse_battery_saver"
    private const val KEY_REPEAT = "pulse_repeat_while_showing"

    /** The old "pulse for" window in seconds, read once to pick the first [KEY_COUNT]. */
    private const val KEY_LEGACY_WINDOW = "pulse_window_seconds"
    private const val LEGACY_PERIOD_MS = 2_000L

    private fun keyToys(addon: AddonId) = "toys_${addon.name.lowercase()}"

    private var prefs: SharedPreferences? = null
    private val _enabled = MutableStateFlow(defaults)
    private val _pulse = MutableStateFlow(PulseSettings())

    /** For each addon, the toys it is switched on for. */
    val enabled: StateFlow<Map<AddonId, Set<ToyId>>> = _enabled.asStateFlow()

    /** The notification pulse's settings. */
    val pulse: StateFlow<PulseSettings> = _pulse.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("glyph_addons", Context.MODE_PRIVATE)
        prefs = p
        _enabled.value = AddonId.entries.associateWith { addon ->
            p.getStringSet(keyToys(addon), null)
                ?.mapNotNull { name -> ToyId.entries.firstOrNull { it.name == name } }?.toSet()
                ?: defaults[addon].orEmpty()
        }
        val d = PulseSettings()
        _pulse.value = PulseSettings(
            riseMs = p.getLong(KEY_RISE, d.riseMs).coerceIn(riseChoices.first(), riseChoices.last()),
            maxBrightness = p.getInt(KEY_MAX_BRIGHTNESS, d.maxBrightness).coerceIn(1, 255),
            count = if (p.contains(KEY_COUNT)) {
                p.getInt(KEY_COUNT, d.count).coerceAtLeast(1)
            } else {
                legacyCount(context, p) ?: d.count
            },
            batterySaver = p.getBoolean(KEY_BATTERY_SAVER, d.batterySaver),
            repeatWhileShowing = p.getBoolean(KEY_REPEAT, d.repeatWhileShowing),
        )
    }

    /**
     * Breaths that fill the old "pulse for" window, which ran 2 s breaths for that many seconds
     * rounded up, capped at the largest choice. It was set here, or before addons on the Pulse
     * clock toy (that file is never deleted). Null if neither was ever set.
     */
    private fun legacyCount(context: Context, p: SharedPreferences): Int? {
        val legacy = context.getSharedPreferences("glyph_clock_toy", Context.MODE_PRIVATE)
        val seconds = listOf(p, legacy).firstOrNull { it.contains(KEY_LEGACY_WINDOW) }
            ?.getInt(KEY_LEGACY_WINDOW, 0) ?: return null
        val count = (seconds * 1_000L + LEGACY_PERIOD_MS - 1) / LEGACY_PERIOD_MS
        return count.toInt().coerceIn(1, countChoices.last())
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

    fun updatePulse(transform: (PulseSettings) -> PulseSettings) {
        val s = _pulse.updateAndGet(transform)
        prefs?.edit {
            putLong(KEY_RISE, s.riseMs)
            putInt(KEY_MAX_BRIGHTNESS, s.maxBrightness.coerceIn(1, 255))
            putInt(KEY_COUNT, s.count)
            putBoolean(KEY_BATTERY_SAVER, s.batterySaver)
            putBoolean(KEY_REPEAT, s.repeatWhileShowing)
        }
    }
}
