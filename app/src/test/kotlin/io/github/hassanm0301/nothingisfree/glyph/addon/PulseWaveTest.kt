package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.glyph.addon.MinuteCheck.Companion.MINUTE_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseWaveTest {

    private val rise = 1_000L
    private val period = 2 * rise

    @Test
    fun `each breath starts dark and peaks after the rise time`() {
        assertEquals(0, PulseWave.level(0, rise, 255))
        assertEquals(255, PulseWave.level(rise, rise, 255))
        assertEquals(0, PulseWave.level(period, rise, 255))
        assertEquals(PulseWave.level(300, rise, 255), PulseWave.level(period * 7 + 300, rise, 255))
        assertEquals(255, PulseWave.level(3_000, 3_000, 255))
    }

    @Test
    fun `level rises then falls`() {
        val rising = (0..rise step 100).map { PulseWave.level(it, rise, 255) }
        val falling = (rise..period step 100).map { PulseWave.level(it, rise, 255) }
        assertEquals(rising.sorted(), rising)
        assertEquals(falling.sortedDescending(), falling)
        assertTrue((0..period).all { PulseWave.level(it, rise, 255) in 0..255 })
    }

    @Test
    fun `max brightness caps the peak`() {
        assertEquals(100, PulseWave.level(rise, rise, 100))
        assertTrue((0..period).all { PulseWave.level(it, rise, 100) in 0..100 })
    }

    @Test
    fun `duration is the number of breaths`() {
        assertEquals(30_000L, PulseWave.duration(1_000, 15))
        assertEquals(1_000L, PulseWave.duration(500, 1))
        assertEquals(30_000L, PulseSettings().durationMs)
    }

    @Test
    fun `elapsed is only set while the pulse runs`() {
        val at = 1_000_000L
        assertNull(PulseWave.elapsed(null, 30_000, at))
        assertEquals(0L, PulseWave.elapsed(at, 30_000, at))
        assertEquals(29_999L, PulseWave.elapsed(at, 30_000, at + 29_999))
        assertNull(PulseWave.elapsed(at, 30_000, at + 30_000))
        assertNull(PulseWave.elapsed(at, 30_000, at - 1))
    }
}

class MinuteCheckTest {

    /** On the minute, like the toy's own clock repaint. */
    private val noon = 1_800_000_000_000L / MINUTE_MS * MINUTE_MS
    private val check = MinuteCheck()

    @Test
    fun `a notification during the minute pulses at the next minute, not before`() {
        assertNull(check.update(noon, lastAlertAt = null, repeat = false))
        val alert = noon + 20_000
        // Repaints later in the same minute (a face change, the AOD tick) don't look again.
        assertNull(check.update(alert + 5_000, alert, repeat = false))
        assertEquals(noon + MINUTE_MS, check.update(noon + MINUTE_MS, alert, repeat = false))
        // A second repaint in that minute keeps the same pulse rather than starting over.
        assertEquals(noon + MINUTE_MS, check.update(noon + MINUTE_MS + 300, alert, repeat = false))
    }

    @Test
    fun `an old notification pulses once only`() {
        val alert = noon - 10_000
        assertEquals(noon, check.update(noon, alert, repeat = false))
        assertEquals(noon, check.update(noon + MINUTE_MS, alert, repeat = false))
        assertEquals(noon, check.update(noon + 2 * MINUTE_MS, alert, repeat = false))
    }

    @Test
    fun `repeat pulses every minute while one is showing`() {
        val alert = noon - 10 * MINUTE_MS
        assertEquals(noon, check.update(noon, alert, repeat = true))
        assertEquals(noon, check.update(noon + 30_000, alert, repeat = true))
        assertEquals(noon + MINUTE_MS, check.update(noon + MINUTE_MS, alert, repeat = true))
        // Cleared: the last pulse stays the last.
        assertEquals(noon + MINUTE_MS, check.update(noon + 2 * MINUTE_MS, alert, repeat = false))
    }

    @Test
    fun `the first check doesn't pulse for old news`() {
        assertNull(check.update(noon, lastAlertAt = noon - 3 * MINUTE_MS, repeat = false))
        // Nor does one after a long gap without repaints.
        val alert = noon + 10_000
        assertEquals(noon + MINUTE_MS, check.update(noon + MINUTE_MS, alert, repeat = false))
        assertEquals(noon + MINUTE_MS, check.update(noon + 30 * MINUTE_MS, alert, repeat = false))
    }

    @Test
    fun `a clock set back still checks, without counting what came before`() {
        val alert = noon + 10_000
        assertEquals(noon + MINUTE_MS, check.update(noon + MINUTE_MS, alert, repeat = false))
        assertEquals(noon + MINUTE_MS, check.update(noon - 60 * MINUTE_MS, alert, repeat = false))
        val later = noon - 60 * MINUTE_MS + 5_000
        assertEquals(noon - 59 * MINUTE_MS, check.update(noon - 59 * MINUTE_MS, later, repeat = false))
    }
}
