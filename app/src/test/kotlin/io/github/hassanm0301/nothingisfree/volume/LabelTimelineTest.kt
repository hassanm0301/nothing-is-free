package io.github.hassanm0301.nothingisfree.volume

import io.github.hassanm0301.nothingisfree.volume.LabelTimeline.Companion.BAR_GONE_TICKS
import io.github.hassanm0301.nothingisfree.volume.LabelTimeline.Companion.FALLBACK_MS
import io.github.hassanm0301.nothingisfree.volume.LabelTimeline.Companion.MAX_MS
import io.github.hassanm0301.nothingisfree.volume.LabelTimeline.Companion.TICK_MS
import io.github.hassanm0301.nothingisfree.volume.LabelTimeline.Companion.WAIT_FOR_BAR_MS
import io.github.hassanm0301.nothingisfree.volume.LabelTimeline.Step.BESIDE_BAR
import io.github.hassanm0301.nothingisfree.volume.LabelTimeline.Step.FALLBACK
import io.github.hassanm0301.nothingisfree.volume.LabelTimeline.Step.HIDE
import io.github.hassanm0301.nothingisfree.volume.LabelTimeline.Step.KEEP
import io.github.hassanm0301.nothingisfree.volume.LabelTimeline.Step.WAIT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelTimelineTest {

    private val t0 = 1_000_000L

    /** A timeline that has found the bar once and hidden again since. */
    private fun barSeen() = LabelTimeline().apply {
        changed(0)
        tick(0, barFound = true)
        repeat(BAR_GONE_TICKS) { tick((it + 1) * TICK_MS, barFound = false) }
    }

    @Test
    fun `without a change there is nothing to show`() {
        val t = LabelTimeline()
        assertEquals(HIDE, t.tick(t0, barFound = true))
        assertFalse(t.barSeen)
    }

    @Test
    fun `until the bar is first found, the label stands in, then moves beside it`() {
        val t = LabelTimeline()
        t.changed(t0)
        assertEquals(FALLBACK, t.tick(t0, barFound = false))
        assertEquals(BESIDE_BAR, t.tick(t0 + TICK_MS, barFound = true))
        assertTrue(t.barSeen)
    }

    @Test
    fun `once the bar has been found, a change waits for it instead of standing in`() {
        val t = barSeen()
        t.changed(t0)
        assertEquals(WAIT, t.tick(t0, barFound = false))
        assertEquals(WAIT, t.tick(t0 + WAIT_FOR_BAR_MS - 1, barFound = false))
        assertEquals(BESIDE_BAR, t.tick(t0 + WAIT_FOR_BAR_MS - 1, barFound = true))
    }

    @Test
    fun `a change the bar never comes for shows nothing`() {
        val t = barSeen()
        t.changed(t0)
        assertEquals(WAIT, t.tick(t0, barFound = false))
        assertEquals(HIDE, t.tick(t0 + WAIT_FOR_BAR_MS, barFound = false))
        // Hidden for good: later ticks don't bring it back without a new change.
        assertEquals(HIDE, t.tick(t0 + WAIT_FOR_BAR_MS + TICK_MS, barFound = true))
    }

    @Test
    fun `the label goes soon after the bar does, not at the first missed tick`() {
        val t = LabelTimeline()
        t.changed(t0)
        assertEquals(BESIDE_BAR, t.tick(t0, barFound = true))
        assertEquals(KEEP, t.tick(t0 + TICK_MS, barFound = false))
        assertEquals(HIDE, t.tick(t0 + 2 * TICK_MS, barFound = false))
    }

    @Test
    fun `the bar coming back starts the missed ticks over`() {
        val t = LabelTimeline()
        t.changed(t0)
        val steps = listOf(true, false, true, false, false).mapIndexed { i, found ->
            t.tick(t0 + i * TICK_MS, barFound = found)
        }
        assertEquals(listOf(BESIDE_BAR, KEEP, BESIDE_BAR, KEEP, HIDE), steps)
    }

    @Test
    fun `beside the bar it stays while the bar does, up to a cap after the last change`() {
        val t = LabelTimeline()
        t.changed(t0)
        assertEquals(BESIDE_BAR, t.tick(t0, barFound = true))
        assertEquals(BESIDE_BAR, t.tick(t0 + MAX_MS - 1, barFound = true))
        assertEquals(HIDE, t.tick(t0 + MAX_MS, barFound = true))
    }

    @Test
    fun `each change moves the cap`() {
        val t = LabelTimeline()
        t.changed(t0)
        t.tick(t0, barFound = true)
        t.changed(t0 + 9_000)
        assertEquals(BESIDE_BAR, t.tick(t0 + MAX_MS, barFound = true))
    }

    @Test
    fun `the stand-in stays for a while after the last change`() {
        val t = LabelTimeline()
        t.changed(t0)
        assertEquals(FALLBACK, t.tick(t0, barFound = false))
        t.changed(t0 + 2_000)
        assertEquals(FALLBACK, t.tick(t0 + FALLBACK_MS, barFound = false))
        assertEquals(FALLBACK, t.tick(t0 + 2_000 + FALLBACK_MS - 1, barFound = false))
        assertEquals(HIDE, t.tick(t0 + 2_000 + FALLBACK_MS, barFound = false))
    }

    @Test
    fun `a window opening shows the label once the bar is found, even before it was ever seen`() {
        val t = LabelTimeline()
        t.appeared(t0)
        assertEquals(WAIT, t.tick(t0, barFound = false))
        assertEquals(BESIDE_BAR, t.tick(t0 + TICK_MS, barFound = true))
        assertTrue(t.barSeen)
    }

    @Test
    fun `a window opening that isn't the popup shows nothing, not even the stand-in`() {
        val t = LabelTimeline()
        t.appeared(t0)
        assertEquals(WAIT, t.tick(t0, barFound = false))
        assertEquals(WAIT, t.tick(t0 + WAIT_FOR_BAR_MS - 1, barFound = false))
        assertEquals(HIDE, t.tick(t0 + WAIT_FOR_BAR_MS, barFound = false))
    }

    @Test
    fun `the label that came with the popup goes with it`() {
        val t = LabelTimeline()
        t.appeared(t0)
        val steps = listOf(true, true, false, false).mapIndexed { i, found ->
            t.tick(t0 + i * TICK_MS, barFound = found)
        }
        assertEquals(listOf(BESIDE_BAR, BESIDE_BAR, KEEP, HIDE), steps)
    }

    @Test
    fun `a window opening doesn't disturb a label already due`() {
        val t = LabelTimeline()
        t.changed(t0)
        assertEquals(FALLBACK, t.tick(t0, barFound = false))
        t.appeared(t0 + 50)
        assertEquals(FALLBACK, t.tick(t0 + TICK_MS, barFound = false))
        // Its time still runs from the change.
        assertEquals(HIDE, t.tick(t0 + FALLBACK_MS, barFound = false))
    }

    @Test
    fun `a change after a window opening may stand in again`() {
        val t = LabelTimeline()
        t.appeared(t0)
        assertEquals(WAIT, t.tick(t0, barFound = false))
        t.changed(t0 + TICK_MS)
        assertEquals(FALLBACK, t.tick(t0 + TICK_MS, barFound = false))
    }

    @Test
    fun `cancel hides and forgets the change`() {
        val t = LabelTimeline()
        t.changed(t0)
        assertEquals(FALLBACK, t.tick(t0, barFound = false))
        assertEquals(HIDE, t.cancel())
        assertEquals(HIDE, t.tick(t0 + TICK_MS, barFound = true))
    }
}
