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
        "The outer ring pulses in a pattern you set after a notification",
    ),
}

/**
 * One step of the notification pulse: ease from wherever the step before left off (dark, for the
 * first) to [brightness] (0..100 %) over [durationMs].
 */
data class PulseStep(val brightness: Int, val durationMs: Long)

/**
 * How the notification pulse looks and when it runs. One setting for every toy.
 *
 * @property steps played in order, [repeat] times. Between 2 and [MAX_STEPS]; the last one always
 *   fades back to 0 %, so each run ends dark (its brightness is ignored and kept at 0).
 * @property repeat how many times the steps play after a notification.
 * @property batterySaver look for notifications once a minute, with the clock's own repaint,
 *   instead of waking for each one as it arrives (see [MinuteCheck]).
 * @property repeatWhileShowing with [batterySaver], pulse every minute while a notification is still
 *   in the shade, not only when a new one has arrived.
 */
data class PulseSettings(
    val steps: List<PulseStep> = DEFAULT_STEPS,
    val repeat: Int = 15,
    val batterySaver: Boolean = false,
    val repeatWhileShowing: Boolean = false,
) {
    /** One run through the steps. */
    val cycleMs: Long get() = steps.sumOf { it.durationMs }

    /** The whole pulse: [repeat] runs. */
    val durationMs: Long get() = cycleMs * repeat

    /** Ring level (0..255) [elapsedMs] into a pulse. */
    fun level(elapsedMs: Long): Int = PulseWave.level(steps, elapsedMs)

    /**
     * Brought within limits: 2 to [MAX_STEPS] steps with the last at 0 %, brightness 0..100,
     * times rounded to [TIME_STEP_MS] between [MIN_STEP_MS] and [MAX_STEP_MS], and [repeat]
     * between 1 and [MAX_REPEAT].
     */
    fun normalized(): PulseSettings {
        val clean = steps.map { PulseStep(it.brightness.coerceIn(0, 100), roundTime(it.durationMs)) }
        val end = clean.lastOrNull()?.copy(brightness = 0) ?: DEFAULT_STEPS.last()
        val body = clean.dropLast(1).take(MAX_STEPS - 1).ifEmpty { listOf(DEFAULT_STEPS.first()) }
        return copy(steps = body + end, repeat = repeat.coerceIn(1, MAX_REPEAT))
    }

    /** With step [index] changed by [transform]. */
    fun withStep(index: Int, transform: (PulseStep) -> PulseStep): PulseSettings =
        copy(steps = steps.mapIndexed { i, step -> if (i == index) transform(step) else step }).normalized()

    /** With a new step before the final fade out, unless there are [MAX_STEPS] already. */
    fun addStep(): PulseSettings =
        if (steps.size >= MAX_STEPS) this else copy(steps = steps.dropLast(1) + NEW_STEP + steps.last()).normalized()

    /** Without step [index]. The final fade out stays, and so does at least one step before it. */
    fun removeStep(index: Int): PulseSettings =
        if (index == steps.lastIndex || steps.size <= 2) this else copy(steps = steps.filterIndexed { i, _ -> i != index })

    companion object {
        const val MAX_STEPS = 8
        const val MAX_REPEAT = 100
        const val TIME_STEP_MS = 100L
        const val MIN_STEP_MS = TIME_STEP_MS
        const val MAX_STEP_MS = 10_000L

        /** Up to full brightness in 1 s and back down in 1 s: the breath the pulse always had. */
        val DEFAULT_STEPS = listOf(PulseStep(100, 1_000), PulseStep(0, 1_000))
        private val NEW_STEP = PulseStep(100, 500)

        private fun roundTime(ms: Long): Long =
            ((ms + TIME_STEP_MS / 2) / TIME_STEP_MS * TIME_STEP_MS).coerceIn(MIN_STEP_MS, MAX_STEP_MS)

        /** "100@1000;0@1000": brightness@millis for each step. */
        fun encode(steps: List<PulseStep>): String = steps.joinToString(";") { "${it.brightness}@${it.durationMs}" }

        /** The steps [encode] wrote, or null if [text] doesn't read as any. */
        fun decode(text: String): List<PulseStep>? = text.split(';').mapNotNull { part ->
            val (b, ms) = part.split('@').takeIf { it.size == 2 } ?: return@mapNotNull null
            PulseStep(b.toIntOrNull() ?: return@mapNotNull null, ms.toLongOrNull() ?: return@mapNotNull null)
        }.ifEmpty { null }
    }
}

/**
 * Addon settings, shared by the app UI and the toy services (same process). [init] runs from
 * `NothingIsFreeApp.onCreate`.
 */
object AddonPreferences {
    /** Where each addon starts out on: the pulse on the Digital clock, as before addons existed. */
    private val defaults = mapOf(AddonId.NOTIFICATION_PULSE to setOf(ToyId.DIGITAL_CLOCK))

    private const val KEY_STEPS = "pulse_steps"
    private const val KEY_REPEAT_COUNT = "pulse_repeat"
    private const val KEY_BATTERY_SAVER = "pulse_battery_saver"
    private const val KEY_WHILE_SHOWING = "pulse_repeat_while_showing"

    /** The old "pulse for" window in seconds, read once to pick the first [KEY_REPEAT_COUNT]. */
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
            steps = p.getString(KEY_STEPS, null)?.let(PulseSettings::decode) ?: d.steps,
            repeat = if (p.contains(KEY_REPEAT_COUNT)) {
                p.getInt(KEY_REPEAT_COUNT, d.repeat)
            } else {
                legacyRepeat(context, p) ?: d.repeat
            },
            batterySaver = p.getBoolean(KEY_BATTERY_SAVER, d.batterySaver),
            repeatWhileShowing = p.getBoolean(KEY_WHILE_SHOWING, d.repeatWhileShowing),
        ).normalized()
    }

    /**
     * Runs of the default steps (one 2 s breath) that fill the old "pulse for" window, which
     * breathed for that many seconds rounded up to whole breaths. It was set here, or before addons
     * on the Pulse clock toy (that file is never deleted). Null if neither was ever set.
     */
    private fun legacyRepeat(context: Context, p: SharedPreferences): Int? {
        val legacy = context.getSharedPreferences("glyph_clock_toy", Context.MODE_PRIVATE)
        val seconds = listOf(p, legacy).firstOrNull { it.contains(KEY_LEGACY_WINDOW) }
            ?.getInt(KEY_LEGACY_WINDOW, 0) ?: return null
        val count = (seconds * 1_000L + LEGACY_PERIOD_MS - 1) / LEGACY_PERIOD_MS
        return count.toInt()
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
        val s = _pulse.updateAndGet { transform(it).normalized() }
        prefs?.edit {
            putString(KEY_STEPS, PulseSettings.encode(s.steps))
            putInt(KEY_REPEAT_COUNT, s.repeat)
            putBoolean(KEY_BATTERY_SAVER, s.batterySaver)
            putBoolean(KEY_WHILE_SHOWING, s.repeatWhileShowing)
        }
    }
}
