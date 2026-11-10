package io.github.hassanm0301.nothingisfree.volume

import android.media.AudioManager

/**
 * How Precise volume writes the level beside the volume bar. Persisted by [name], so don't
 * rename entries.
 */
enum class VolumeFormat(val title: String) {
    PERCENT("Percent"),
    STEPS("Steps");

    /** Step [index] of [max] (Android's volume steps) as the label shows it: "47%" or "7/15". */
    fun text(index: Int, max: Int): String {
        val top = max.coerceAtLeast(0)
        val i = index.coerceIn(0, top)
        return when (this) {
            PERCENT -> "${percent(i, top)}%"
            STEPS -> "$i/$top"
        }
    }

    /** The longest text this format writes for [max] steps, so the label can keep one width. */
    fun widest(max: Int): String = text(max, max)

    companion object {
        /** Step [index] of [max] as a whole percentage, rounded to the nearest; 0 without steps. */
        fun percent(index: Int, max: Int): Int =
            if (max <= 0) 0 else ((index * 200 + max) / (2 * max)).coerceIn(0, 100)
    }
}

/**
 * The streams the label is shown for: the ones the volume bar shows. The rest follow one of these
 * on phones (system sounds follow the ringer; accessibility and text-to-speech follow media), and
 * their own broadcasts, sent with the same key press, would make the label flicker between values.
 */
object VolumeStreams {
    /**
     * Which stream the popup most likely shows when it opens without a change, most likely first.
     * Media is Android's default when nothing is playing; the others follow.
     */
    val ORDER = listOf(
        AudioManager.STREAM_MUSIC,
        AudioManager.STREAM_RING,
        AudioManager.STREAM_NOTIFICATION,
        AudioManager.STREAM_ALARM,
        AudioManager.STREAM_VOICE_CALL,
    )

    /** As [ORDER], during a call, when the volume keys change the call volume. */
    val IN_CALL_ORDER = listOf(AudioManager.STREAM_VOICE_CALL) + (ORDER - AudioManager.STREAM_VOICE_CALL)

    fun isShown(stream: Int): Boolean = stream in ORDER

    /**
     * The stream whose level the bar shows: of [levels], most likely first, the first whose level
     * is within half a step of [fill] (how full the bar is, 0..1). The bar may count from the
     * stream's lowest step or from zero, so both are tried. The most likely one if none matches
     * or the bar doesn't say how full it is.
     */
    fun pick(fill: Float?, levels: List<StreamLevel>): Int {
        val first = levels.firstOrNull()?.stream ?: AudioManager.STREAM_MUSIC
        if (fill == null) return first
        return levels.firstOrNull { it.matches(fill) }?.stream ?: first
    }
}

/** A stream's volume: step [index] between [min] and [max]. */
data class StreamLevel(val stream: Int, val index: Int, val min: Int, val max: Int) {
    /** Whether a bar [fill] full (0..1) shows this level, counted from zero or from [min]. */
    fun matches(fill: Float): Boolean {
        fun near(from: Int): Boolean {
            val steps = max - from
            if (steps <= 0) return false
            val level = (index - from).toFloat() / steps
            return kotlin.math.abs(fill - level) <= 0.5f / steps
        }
        return near(0) || (min > 0 && near(min))
    }
}
