package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.dotmatrix.Brightness
import io.github.hassanm0301.nothingisfree.glyph.addon.MinuteCheck.Companion.MINUTE_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseWaveTest {

    private val breath = PulseSettings.DEFAULT_STEPS

    @Test
    fun `the default breath starts dark, peaks after a second and ends dark`() {
        assertEquals(0, PulseWave.level(breath, 0))
        assertEquals(255, PulseWave.level(breath, 1_000))
        assertEquals(0, PulseWave.level(breath, 2_000))
        assertEquals(PulseWave.level(breath, 300), PulseWave.level(breath, 2_000 * 7 + 300))
    }

    @Test
    fun `level rises then falls`() {
        val rising = (0..1_000L step 100).map { PulseWave.level(breath, it) }
        val falling = (1_000..2_000L step 100).map { PulseWave.level(breath, it) }
        assertEquals(rising.sorted(), rising)
        assertEquals(falling.sortedDescending(), falling)
        assertTrue((0..2_000L).all { PulseWave.level(breath, it) in 0..255 })
    }

    @Test
    fun `each step reaches its brightness after its time, starting from the step before`() {
        val steps = listOf(PulseStep(50, 500), PulseStep(100, 1_500), PulseStep(20, 300), PulseStep(0, 700))
        assertEquals(0, PulseWave.level(steps, 0))
        assertEquals(Brightness.level(50), PulseWave.level(steps, 500))
        // Halfway through the second step: halfway from 50 % to 100 %.
        assertEquals(Brightness.level(75), PulseWave.level(steps, 500 + 750))
        assertEquals(Brightness.level(100), PulseWave.level(steps, 2_000))
        assertEquals(Brightness.level(20), PulseWave.level(steps, 2_300))
        assertEquals(0, PulseWave.level(steps, 3_000))
    }

    @Test
    fun `every run ends dark, even if the last step asks for light`() {
        val steps = listOf(PulseStep(100, 1_000), PulseStep(80, 1_000))
        assertEquals(255, PulseWave.level(steps, 1_000))
        assertTrue(PulseWave.level(steps, 1_990) < 5)
        assertEquals(0, PulseWave.level(steps, 2_000))
    }

    @Test
    fun `duration is the run times the repeats`() {
        assertEquals(30_000L, PulseSettings().durationMs)
        val s = PulseSettings(steps = listOf(PulseStep(60, 300), PulseStep(0, 200)), repeat = 3)
        assertEquals(500L, s.cycleMs)
        assertEquals(1_500L, s.durationMs)
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

class PulseSettingsTest {

    @Test
    fun `normalized keeps within limits and ends at 0 percent`() {
        val wild = PulseSettings(
            steps = listOf(PulseStep(150, 40), PulseStep(-5, 12_345), PulseStep(70, 1_234)),
            repeat = 500,
        ).normalized()
        assertEquals(listOf(PulseStep(100, 100), PulseStep(0, 10_000), PulseStep(0, 1_200)), wild.steps)
        assertEquals(PulseSettings.MAX_REPEAT, wild.repeat)
        assertEquals(1, PulseSettings(repeat = 0).normalized().repeat)
    }

    @Test
    fun `normalized always leaves a step before the fade out, and at most the maximum`() {
        assertEquals(
            listOf(PulseSettings.DEFAULT_STEPS.first(), PulseStep(0, 300)),
            PulseSettings(steps = listOf(PulseStep(40, 300))).normalized().steps,
        )
        assertEquals(PulseSettings.DEFAULT_STEPS, PulseSettings(steps = emptyList()).normalized().steps)
        val many = PulseSettings(steps = List(20) { PulseStep(it * 5, 100) }).normalized()
        assertEquals(PulseSettings.MAX_STEPS, many.steps.size)
        assertEquals(0, many.steps.last().brightness)
    }

    @Test
    fun `steps are added before the fade out, up to the maximum, and removed except the last two`() {
        var s = PulseSettings()
        repeat(20) { s = s.addStep() }
        assertEquals(PulseSettings.MAX_STEPS, s.steps.size)
        assertEquals(PulseSettings.DEFAULT_STEPS.last(), s.steps.last())

        s = s.withStep(1) { it.copy(brightness = 30) }
        assertEquals(30, s.steps[1].brightness)
        s = s.removeStep(1)
        assertEquals(PulseSettings.MAX_STEPS - 1, s.steps.size)
        assertEquals(s, s.removeStep(s.steps.lastIndex))
        while (s.steps.size > 2) s = s.removeStep(0)
        assertEquals(s, s.removeStep(0))
    }

    @Test
    fun `steps survive being saved`() {
        val steps = listOf(PulseStep(35, 400), PulseStep(100, 1_200), PulseStep(0, 900))
        assertEquals(steps, PulseSettings.decode(PulseSettings.encode(steps)))
        assertNull(PulseSettings.decode("nonsense"))
    }
}

class BrightnessTest {

    @Test
    fun `percent maps onto the 0 to 255 matrix levels and back`() {
        assertEquals(0, Brightness.level(0))
        assertEquals(128, Brightness.level(50))
        assertEquals(255, Brightness.level(100))
        assertEquals(255, Brightness.level(140))
        assertEquals(9, Brightness.percent(24))
        assertEquals(100, Brightness.percent(255))
        (0..100).forEach { assertEquals(it, Brightness.percent(Brightness.level(it))) }
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
