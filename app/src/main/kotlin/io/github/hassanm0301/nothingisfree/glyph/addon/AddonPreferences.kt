package io.github.hassanm0301.nothingisfree.glyph.addon

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.hassanm0301.nothingisfree.dotmatrix.Brightness
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet

/**
 * What an addon shows. A toy runs at most one addon of each category, in the order the Glyph tab
 * lists them.
 */
enum class AddonCategory(val title: String) {
    NOTIFICATION("Notification"),
    BATTERY("Battery"),
}

/**
 * Addons draw on top of any Glyph Toy. Each is switched on per toy, at most one per [category].
 * Persisted by [name], so don't rename entries.
 */
enum class AddonId(val title: String, val summary: String, val category: AddonCategory) {
    NOTIFICATION_PULSE(
        "Notification pulse",
        "The outer ring pulses in a pattern you set after a notification",
        AddonCategory.NOTIFICATION,
    ),
    BATTERY_LEVEL(
        "Battery level",
        "Five dots along one edge show how much charge is left",
        AddonCategory.BATTERY,
    ),
    BINARY_BATTERY(
        "Binary battery",
        "The exact percentage in binary-coded decimal on a 5x2 block, with charging and battery saver",
        AddonCategory.BATTERY,
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

/** The edge of the matrix a battery addon sits on, from its outermost line of LEDs inwards. */
enum class BatteryEdge(val label: String) {
    TOP("Top"),
    BOTTOM("Bottom"),
    LEFT("Left"),
    RIGHT("Right"),
    ;

    /** Whether the addon runs across the matrix (top or bottom) rather than up it. */
    val horizontal: Boolean get() = this == TOP || this == BOTTOM
}

/**
 * Which way a horizontal battery bar empties. [RIGHT_TO_LEFT]: the right-hand dot goes out first
 * and the left-hand one last. Vertical bars always empty from the top down.
 */
enum class BatteryDrain(val label: String) {
    RIGHT_TO_LEFT("Right to left"),
    LEFT_TO_RIGHT("Left to right"),
}

/**
 * How the battery level addon looks. One setting for every toy. Brightness values are 0..100 %.
 *
 * @property drain which way the bar empties when [edge] is the top or bottom; ignored on the sides.
 * @property brightness the dots still holding charge.
 * @property emptyBrightness the dots already used up; 0 leaves them dark, so only the charge shows.
 * @property fadeHighestDot dim the highest lit dot by how much of its share is left: from
 *   [brightness] with its share full down towards [emptyBrightness] as it runs out. At 70% the
 *   fourth of five dots holds half its 20%, so it sits halfway between the two.
 */
data class BatterySettings(
    override val edge: BatteryEdge = BatteryEdge.TOP,
    val drain: BatteryDrain = BatteryDrain.RIGHT_TO_LEFT,
    val brightness: Int = 100,
    val emptyBrightness: Int = 10,
    val fadeHighestDot: Boolean = false,
) : BatteryLook

/**
 * How the binary battery addon looks. One setting for every toy. Brightness values are 0..100 %.
 *
 * @property edge where the 5x2 block goes, if it fits beside the toy's face (see [BinaryBattery]).
 * @property activeBrightness set bits, and the status dots when on.
 * @property inactiveBrightness clear bits, and the status dots when off; 0 leaves them dark.
 */
data class BinaryBatterySettings(
    override val edge: BatteryEdge = BatteryEdge.LEFT,
    val activeBrightness: Int = 100,
    val inactiveBrightness: Int = 10,
) : BatteryLook

/**
 * The settings of a [AddonCategory.BATTERY] addon: what it looks like, and so where it goes. A toy
 * shows at most one.
 */
sealed interface BatteryLook {
    val edge: BatteryEdge
}

/**
 * Addon settings, shared by the app UI and the toy services (same process). [init] runs from
 * `NothingIsFreeApp.onCreate`.
 */
object AddonPreferences {
    /**
     * Where each addon starts out on: the pulse on the Digital clock, as before addons existed.
     * Others start off everywhere.
     */
    private val defaults = mapOf(AddonId.NOTIFICATION_PULSE to setOf(ToyId.DIGITAL_CLOCK))

    private const val KEY_STEPS = "pulse_steps"
    private const val KEY_REPEAT_COUNT = "pulse_repeat"
    private const val KEY_BATTERY_SAVER = "pulse_battery_saver"
    private const val KEY_WHILE_SHOWING = "pulse_repeat_while_showing"
    private const val KEY_BATTERY_EDGE = "battery_edge"
    private const val KEY_BATTERY_DRAIN = "battery_drain"
    private const val KEY_BATTERY_BRIGHTNESS = "battery_brightness_percent"
    private const val KEY_BATTERY_EMPTY_BRIGHTNESS = "battery_empty_brightness_percent"
    private const val KEY_BATTERY_FADE = "battery_fade_highest_dot"
    private const val KEY_BINARY_EDGE = "binary_battery_edge"
    private const val KEY_BINARY_ACTIVE = "binary_battery_active_percent"
    private const val KEY_BINARY_INACTIVE = "binary_battery_inactive_percent"

    /** Battery brightness saved on the old 0..255 scale, converted the first time it is read. */
    private const val KEY_LEGACY_BATTERY_BRIGHTNESS = "battery_brightness"
    private const val KEY_LEGACY_BATTERY_EMPTY_BRIGHTNESS = "battery_empty_brightness"

    /** The old "pulse for" window in seconds, read once to pick the first [KEY_REPEAT_COUNT]. */
    private const val KEY_LEGACY_WINDOW = "pulse_window_seconds"
    private const val LEGACY_PERIOD_MS = 2_000L

    private fun keyToys(addon: AddonId) = "toys_${addon.name.lowercase()}"

    private var prefs: SharedPreferences? = null
    private val _enabled = MutableStateFlow(defaults)
    private val _pulse = MutableStateFlow(PulseSettings())
    private val _battery = MutableStateFlow(BatterySettings())
    private val _binaryBattery = MutableStateFlow(BinaryBatterySettings())

    /** For each addon, the toys it is switched on for. A toy is in at most one per category. */
    val enabled: StateFlow<Map<AddonId, Set<ToyId>>> = _enabled.asStateFlow()

    /** The notification pulse's settings. */
    val pulse: StateFlow<PulseSettings> = _pulse.asStateFlow()

    /** The battery level addon's settings. */
    val battery: StateFlow<BatterySettings> = _battery.asStateFlow()

    /** The binary battery addon's settings. */
    val binaryBattery: StateFlow<BinaryBatterySettings> = _binaryBattery.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("glyph_addons", Context.MODE_PRIVATE)
        prefs = p
        _enabled.value = onePerCategory(
            AddonId.entries.associateWith { addon ->
                p.getStringSet(keyToys(addon), null)
                    ?.mapNotNull { name -> ToyId.entries.firstOrNull { it.name == name } }?.toSet()
                    ?: defaults[addon].orEmpty()
            },
        )
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
        val b = BatterySettings()
        _battery.value = BatterySettings(
            edge = p.getString(KEY_BATTERY_EDGE, null)
                ?.let { s -> BatteryEdge.entries.firstOrNull { it.name == s } } ?: b.edge,
            drain = p.getString(KEY_BATTERY_DRAIN, null)
                ?.let { s -> BatteryDrain.entries.firstOrNull { it.name == s } } ?: b.drain,
            brightness = percent(p, KEY_BATTERY_BRIGHTNESS, KEY_LEGACY_BATTERY_BRIGHTNESS, b.brightness)
                .coerceAtLeast(1),
            emptyBrightness = percent(p, KEY_BATTERY_EMPTY_BRIGHTNESS, KEY_LEGACY_BATTERY_EMPTY_BRIGHTNESS, b.emptyBrightness),
            fadeHighestDot = p.getBoolean(KEY_BATTERY_FADE, b.fadeHighestDot),
        )
        val bb = BinaryBatterySettings()
        _binaryBattery.value = BinaryBatterySettings(
            edge = p.getString(KEY_BINARY_EDGE, null)
                ?.let { s -> BatteryEdge.entries.firstOrNull { it.name == s } } ?: bb.edge,
            activeBrightness = p.getInt(KEY_BINARY_ACTIVE, bb.activeBrightness).coerceIn(1, 100),
            inactiveBrightness = p.getInt(KEY_BINARY_INACTIVE, bb.inactiveBrightness).coerceIn(0, 100),
        )
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

    /**
     * Switch [addon] on or off for [toy]. Switching it on switches off the other addon of its
     * category on that toy, if one was on: a toy runs one addon per category.
     */
    fun setEnabled(addon: AddonId, toy: ToyId, on: Boolean) {
        val all = _enabled.updateAndGet { current -> withAddon(current, addon, toy, on) }
        prefs?.edit {
            AddonId.entries.forEach { a -> putStringSet(keyToys(a), all[a].orEmpty().map { it.name }.toSet()) }
        }
    }

    /**
     * The battery addon on for [toy], as its settings (null with none), as either changes. The
     * toy services draw it; see [BatteryLook].
     */
    fun batteryLook(toy: ToyId): Flow<BatteryLook?> =
        combine(enabled, battery, binaryBattery) { on, bar, binary -> batteryLook(on, toy, bar, binary) }
            .distinctUntilChanged()

    /** The settings of the battery addon that [on] has switched on for [toy], or null for none. */
    fun batteryLook(
        on: Map<AddonId, Set<ToyId>>,
        toy: ToyId,
        bar: BatterySettings,
        binary: BinaryBatterySettings,
    ): BatteryLook? = when (active(on, AddonCategory.BATTERY, toy)) {
        AddonId.BATTERY_LEVEL -> bar
        AddonId.BINARY_BATTERY -> binary
        else -> null
    }

    /** The addon of [category] that [on] has switched on for [toy], or null for none. */
    fun active(on: Map<AddonId, Set<ToyId>>, category: AddonCategory, toy: ToyId): AddonId? =
        AddonId.entries.firstOrNull { it.category == category && toy in on[it].orEmpty() }

    /** [current] with [addon] switched on or off for [toy], and the rest of its category off if on. */
    internal fun withAddon(
        current: Map<AddonId, Set<ToyId>>,
        addon: AddonId,
        toy: ToyId,
        on: Boolean,
    ): Map<AddonId, Set<ToyId>> = AddonId.entries.associateWith { a ->
        val toys = current[a].orEmpty()
        when {
            a == addon -> if (on) toys + toy else toys - toy
            on && a.category == addon.category -> toys - toy
            else -> toys
        }
    }

    /**
     * [on] with at most one addon per category on each toy: the first in [AddonId] order stays.
     * Only matters for settings saved before this rule.
     */
    internal fun onePerCategory(on: Map<AddonId, Set<ToyId>>): Map<AddonId, Set<ToyId>> =
        AddonId.entries.associateWith { addon ->
            on[addon].orEmpty().filterTo(mutableSetOf()) { toy -> active(on, addon.category, toy) == addon }
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

    fun updateBattery(transform: (BatterySettings) -> BatterySettings) {
        val s = _battery.updateAndGet(transform)
        prefs?.edit {
            putString(KEY_BATTERY_EDGE, s.edge.name)
            putString(KEY_BATTERY_DRAIN, s.drain.name)
            putInt(KEY_BATTERY_BRIGHTNESS, s.brightness.coerceIn(1, 100))
            putInt(KEY_BATTERY_EMPTY_BRIGHTNESS, s.emptyBrightness.coerceIn(0, 100))
            putBoolean(KEY_BATTERY_FADE, s.fadeHighestDot)
        }
    }

    fun updateBinaryBattery(transform: (BinaryBatterySettings) -> BinaryBatterySettings) {
        val s = _binaryBattery.updateAndGet(transform)
        prefs?.edit {
            putString(KEY_BINARY_EDGE, s.edge.name)
            putInt(KEY_BINARY_ACTIVE, s.activeBrightness.coerceIn(1, 100))
            putInt(KEY_BINARY_INACTIVE, s.inactiveBrightness.coerceIn(0, 100))
        }
    }

    /** A 0..100 % setting at [key], or converted from the old 0..255 one at [legacyKey]. */
    private fun percent(p: SharedPreferences, key: String, legacyKey: String, default: Int): Int = when {
        p.contains(key) -> p.getInt(key, default).coerceIn(0, 100)
        p.contains(legacyKey) -> Brightness.percent(p.getInt(legacyKey, 0))
        else -> default
    }
}
