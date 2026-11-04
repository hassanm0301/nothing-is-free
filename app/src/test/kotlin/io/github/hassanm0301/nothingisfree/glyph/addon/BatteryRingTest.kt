package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.toy.BinaryClockFace
import io.github.hassanm0301.nothingisfree.glyph.toy.BinaryClockSettings
import io.github.hassanm0301.nothingisfree.glyph.toy.BitStyle
import io.github.hassanm0301.nothingisfree.glyph.toy.ClockFace
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyFace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class BatteryRingTest {

    /** A face that draws nothing, so the ring is the whole edge. */
    private class Blank(override val size: Int) : ToyFace {
        override fun render(time: LocalTime, is24Hour: Boolean) = DotMatrix(size, size)
        override fun occupies(x: Int, y: Int) = false
    }

    private val blank = Blank(13)
    private val digital = ClockFace(13)
    private val time = LocalTime.of(10, 10)

    private val clockwise = BatteryRingSettings(direction = RingDirection.CLOCKWISE, emptyBrightness = 0)
    private val anticlockwise = BatteryRingSettings(direction = RingDirection.ANTICLOCKWISE, emptyBrightness = 0)

    /** The order the ring empties in: the reverse of the fill order [BatteryRing.cells] gives. */
    private fun drainOrder(settings: BatteryRingSettings, face: ToyFace = blank) =
        BatteryRing.cells(face, settings).reversed()

    private val faces: List<ToyFace> = listOf(blank, digital) + BitStyle.entries.flatMap { style ->
        listOf(true, false).map { gap ->
            BinaryClockFace(13).with(BinaryClockSettings(style = style, dotGap = gap, showUnusedBits = true))
        }
    }

    @Test
    fun `the ring is the whole edge, a single line all around`() {
        val ring = BatteryRing.cells(blank, clockwise)
        assertEquals(NotificationPulse.ring(blank).toSet(), ring.toSet())
        assertEquals(36, ring.size)
        assertEquals(ring.size, ring.toSet().size)
        // Each dot touches the next, diagonals included, all the way round and back to the start.
        (ring + ring.first()).zipWithNext().forEach { (a, b) ->
            assertTrue("$a to $b", maxOf(Math.abs(a.first - b.first), Math.abs(a.second - b.second)) == 1)
        }
    }

    @Test
    fun `empties from the top, clockwise or anticlockwise`() {
        val cw = drainOrder(clockwise)
        assertEquals(listOf(6 to 0, 7 to 0, 8 to 0, 9 to 1), cw.take(4))
        // A quarter of the way round, on the right; the last dot is just left of the top.
        assertTrue(cw.indexOf(12 to 6) in 8..10)
        assertEquals(5 to 0, cw.last())

        val acw = drainOrder(anticlockwise)
        assertEquals(listOf(6 to 0, 5 to 0, 4 to 0, 3 to 1), acw.take(4))
        assertTrue(acw.indexOf(0 to 6) in 8..10)
        assertEquals(7 to 0, acw.last())
        // The same dots, mirrored.
        assertEquals(cw.map { (x, y) -> 12 - x to y }, acw)
    }

    @Test
    fun `full lights the whole ring, and the charge drains from the top`() {
        val full = blank.frame(time, true, ring = RingGauge(clockwise, 100))
        BatteryRing.cells(blank, clockwise).forEach { (x, y) -> assertEquals("($x,$y)", 255, full[x, y]) }

        val half = blank.frame(time, true, ring = RingGauge(clockwise, 50))
        val cw = drainOrder(clockwise)
        // 18 of 36 used up: the right half, from the top round to the bottom.
        cw.take(18).forEach { (x, y) -> assertEquals("used ($x,$y)", 0, half[x, y]) }
        cw.drop(18).forEach { (x, y) -> assertEquals("lit ($x,$y)", 255, half[x, y]) }
        assertEquals(0, half[12, 6])
        assertEquals(255, half[0, 6])
    }

    @Test
    fun `each dot is a share, lit until it is used up`() {
        assertEquals(36, BatteryRing.lit(100, 36))
        assertEquals(36, BatteryRing.lit(98, 36))
        assertEquals(18, BatteryRing.lit(50, 36))
        assertEquals(1, BatteryRing.lit(1, 36))
        assertEquals(0, BatteryRing.lit(0, 36))
    }

    @Test
    fun `used-up dots take their own brightness`() {
        val settings = BatteryRingSettings(brightness = 80, emptyBrightness = 4)
        val frame = blank.frame(time, true, ring = RingGauge(settings, 50))
        val cw = drainOrder(settings)
        // 80 % and 4 % as matrix levels.
        assertEquals(10, frame[cw.first().first, cw.first().second])
        assertEquals(204, frame[cw.last().first, cw.last().second])
    }

    @Test
    fun `stays clear of every face and leaves it intact`() {
        faces.forEach { face ->
            val ring = BatteryRing.cells(face, clockwise)
            assertTrue(ring.isNotEmpty())
            ring.forEach { (x, y) -> assertTrue("($x,$y)", face.isLed(x, y) && !face.occupies(x, y)) }
            val plain = face.render(time, true)
            val full = face.frame(time, true, ring = RingGauge(clockwise, 100))
            for (y in 0 until 13) for (x in 0 until 13) {
                val expected = if (x to y in ring) 255 else plain[x, y]
                assertEquals("($x,$y)", expected, full[x, y])
            }
        }
    }

    @Test
    fun `goes around the battery bar, and the pulse covers it while it plays`() {
        val bar = BatterySettings(edge = BatteryEdge.TOP, emptyBrightness = 0)
        val layout = AddonLayout(blank, bar, clockwise)
        assertEquals(NotificationPulse.ring(blank).toSet() - layout.bar.toSet(), layout.batteryRing.toSet())
        // With the top row taken by the bar, the ring starts just right of it.
        assertEquals(9 to 1, layout.batteryRing.last())

        val frame = blank.frame(
            time, true, pulseLevel = 90,
            battery = BatteryGauge(bar, 40), ring = RingGauge(clockwise, 40), layout = layout,
        )
        assertEquals(listOf(255, 255, 0, 0, 0), (4..8).map { frame[it, 0] })
        layout.batteryRing.forEach { (x, y) -> assertEquals("($x,$y)", 90, frame[x, y]) }
    }

    @Test
    fun `with the addon off the layout has no ring`() {
        assertTrue(AddonLayout(digital, null).batteryRing.isEmpty())
        assertTrue(AddonLayout(digital, null, null).batteryRing.isEmpty())
    }
}
