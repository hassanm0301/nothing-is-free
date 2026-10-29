package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.glyph.addon.PulseWave.PERIOD_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseWaveTest {

    @Test
    fun `each period starts dark and peaks halfway`() {
        assertEquals(0, PulseWave.level(0))
        assertEquals(255, PulseWave.level(PERIOD_MS / 2))
        assertEquals(0, PulseWave.level(PERIOD_MS))
        assertEquals(PulseWave.level(300), PulseWave.level(PERIOD_MS * 7 + 300))
    }

    @Test
    fun `level rises then falls`() {
        val rising = (0..PERIOD_MS / 2 step 100).map(PulseWave::level)
        val falling = (PERIOD_MS / 2..PERIOD_MS step 100).map(PulseWave::level)
        assertEquals(rising.sorted(), rising)
        assertEquals(falling.sortedDescending(), falling)
        assertTrue((0..PERIOD_MS).all { PulseWave.level(it) in 0..255 })
    }

    @Test
    fun `duration rounds the window up to whole periods`() {
        assertEquals(30_000L, PulseWave.duration(30_000))
        assertEquals(12_000L, PulseWave.duration(10_001))
        assertEquals(PERIOD_MS, PulseWave.duration(1))
    }

    @Test
    fun `elapsed is only set inside the window`() {
        val at = 1_000_000L
        assertNull(PulseWave.elapsed(null, 30_000, at))
        assertEquals(0L, PulseWave.elapsed(at, 30_000, at))
        assertEquals(29_999L, PulseWave.elapsed(at, 30_000, at + 29_999))
        assertNull(PulseWave.elapsed(at, 30_000, at + 30_000))
        assertNull(PulseWave.elapsed(at, 30_000, at - 1))
    }
}
