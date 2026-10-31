package io.github.hassanm0301.nothingisfree.glyph.addon

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

/** The breathing curve for the notification ring. */
object PulseWave {

    /**
     * Ring brightness (0..[max]) [elapsedMs] into a pulse whose breaths take [riseMs] to go from
     * dark to [max] and the same again back to dark. Squared so the fade looks even on the LEDs
     * rather than lingering bright.
     */
    fun level(elapsedMs: Long, riseMs: Long, max: Int): Int {
        val period = 2 * riseMs
        val phase = (elapsedMs % period).toDouble() / period
        val x = (1 - cos(2 * PI * phase)) / 2
        return (x * x * max).roundToInt()
    }

    /** How long [count] breaths of [riseMs] up and [riseMs] down take. */
    fun duration(riseMs: Long, count: Int): Long = 2 * riseMs * count

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
