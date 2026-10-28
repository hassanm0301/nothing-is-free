package io.github.hassanm0301.nothingisfree.glyph.toy

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

/** The breathing curve for the notification ring. */
object PulseWave {
    const val PERIOD_MS = 2_000L

    /**
     * Ring brightness (0..255) [elapsedMs] into a pulse. Each period starts dark, peaks halfway
     * and returns to dark; squared so the fade looks even on the LEDs rather than lingering bright.
     */
    fun level(elapsedMs: Long): Int {
        val phase = (elapsedMs % PERIOD_MS).toDouble() / PERIOD_MS
        val x = (1 - cos(2 * PI * phase)) / 2
        return (x * x * 255).roundToInt()
    }

    /** [windowMs] rounded up to whole periods, so a pulse always fades out instead of cutting off. */
    fun duration(windowMs: Long): Long = (windowMs + PERIOD_MS - 1) / PERIOD_MS * PERIOD_MS

    /** How far into a pulse we are at [now], or null if there is none running. */
    fun elapsed(alertAt: Long?, windowMs: Long, now: Long): Long? =
        alertAt?.let { now - it }?.takeIf { it in 0 until duration(windowMs) }
}
