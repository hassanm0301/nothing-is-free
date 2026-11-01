package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.dotmatrix.Brightness
import kotlin.math.PI
import kotlin.math.cos

/** The notification ring's brightness over time. */
object PulseWave {

    /**
     * Ring level (0..255) [elapsedMs] into a pulse that plays [steps] over and over. Each run
     * starts dark; each step eases from where the one before left off to its own brightness over
     * its time, and the last one always goes back to 0, so every run ends dark.
     */
    fun level(steps: List<PulseStep>, elapsedMs: Long): Int {
        val cycle = steps.sumOf { it.durationMs }
        if (cycle <= 0 || elapsedMs < 0) return 0
        var t = elapsedMs % cycle
        var from = 0.0
        steps.forEachIndexed { i, step ->
            val to = if (i == steps.lastIndex) 0.0 else step.brightness.toDouble()
            if (t < step.durationMs) {
                val eased = (1 - cos(PI * t / step.durationMs)) / 2
                return Brightness.level(from + (to - from) * eased)
            }
            t -= step.durationMs
            from = to
        }
        return 0
    }

    /** How far into a pulse started at [startAt] and lasting [durationMs] we are at [now], or null if none is running. */
    fun elapsed(startAt: Long?, durationMs: Long, now: Long): Long? =
        startAt?.let { now - it }?.takeIf { it in 0 until durationMs }
}

/**
 * Battery saver for the notification pulse: instead of waking for each notification, look once a
 * minute, on the repaint that moves the clock on. [update] is called on every repaint; the first
 * one in each minute is the check. A check starts a pulse if a notification arrived since the
 * previous check, or, with `repeat`, whenever one is still showing.
 *
 * "Since the previous check" looks back at most [LOOKBACK_MS], so the first check after a gap (the
 * toy was away, or battery saver was just switched on) doesn't pulse for old news.
 */
class MinuteCheck {
    private var checkedAt: Long? = null
    private var pulseAt: Long? = null

    /** When the latest battery-saver pulse started (it may have ended since), or null for none. */
    fun update(now: Long, lastAlertAt: Long?, repeat: Boolean): Long? {
        val last = checkedAt
        if (last == null || now / MINUTE_MS != last / MINUTE_MS) {
            val since = maxOf(last ?: Long.MIN_VALUE, now - LOOKBACK_MS)
            if (repeat || (lastAlertAt != null && lastAlertAt > since)) pulseAt = now
            checkedAt = now
        }
        return pulseAt
    }

    companion object {
        const val MINUTE_MS = 60_000L
        const val LOOKBACK_MS = 2 * MINUTE_MS
    }
}
