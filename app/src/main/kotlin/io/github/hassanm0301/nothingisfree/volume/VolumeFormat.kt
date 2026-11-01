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
    private val shown = setOf(
        AudioManager.STREAM_MUSIC,
        AudioManager.STREAM_RING,
        AudioManager.STREAM_NOTIFICATION,
        AudioManager.STREAM_ALARM,
        AudioManager.STREAM_VOICE_CALL,
    )

    fun isShown(stream: Int): Boolean = stream in shown
}
