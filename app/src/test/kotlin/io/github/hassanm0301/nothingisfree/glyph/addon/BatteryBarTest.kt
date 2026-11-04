package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.glyph.toy.BinaryClockFace
import io.github.hassanm0301.nothingisfree.glyph.toy.BinaryClockSettings
import io.github.hassanm0301.nothingisfree.glyph.toy.CompactClockFace
import io.github.hassanm0301.nothingisfree.glyph.toy.BitStyle
import io.github.hassanm0301.nothingisfree.glyph.toy.ClockFace
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyFace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class BatteryBarTest {

    private val digital = ClockFace(13)

    private fun cells(edge: BatteryEdge, drain: BatteryDrain = BatteryDrain.RIGHT_TO_LEFT, face: ToyFace = digital) =
        BatteryBar.cells(face, BatterySettings(edge = edge, drain = drain))

    /** Every face layout the app has, on the 13x13 matrix. */
    private val faces: List<ToyFace> = listOf(digital) + BitStyle.entries.flatMap { style ->
        listOf(true, false).map { gap ->
            BinaryClockFace(13).with(BinaryClockSettings(style = style, dotGap = gap, showUnusedBits = true))
        }
    } + CompactClockFace(13)

    @Test
    fun `top and bottom are the whole outermost row, filling from the end that empties last`() {
        val row = (4..8).toList()
        assertEquals(row.map { it to 0 }, cells(BatteryEdge.TOP))
        assertEquals(row.reversed().map { it to 0 }, cells(BatteryEdge.TOP, BatteryDrain.LEFT_TO_RIGHT))
        assertEquals(row.map { it to 12 }, cells(BatteryEdge.BOTTOM))
        assertEquals(row.reversed().map { it to 12 }, cells(BatteryEdge.BOTTOM, BatteryDrain.LEFT_TO_RIGHT))
    }

    @Test
    fun `the sides fill from the bottom up, whichever way the top and bottom drain`() {
        val bottomUp = (8 downTo 4).toList()
        BatteryDrain.entries.forEach { drain ->
            assertEquals(bottomUp.map { 0 to it }, cells(BatteryEdge.LEFT, drain))
            assertEquals(bottomUp.map { 12 to it }, cells(BatteryEdge.RIGHT, drain))
        }
    }

    @Test
    fun `25x25 takes the middle five of its outermost row`() {
        assertEquals((10..14).map { it to 0 }, cells(BatteryEdge.TOP, face = ClockFace(25)))
        assertEquals((14 downTo 10).map { 24 to it }, cells(BatteryEdge.RIGHT, face = ClockFace(25)))
    }

    @Test
    fun `every dot is a share of the charge, lit until it is used up`() {
        val expected = mapOf(0 to 0, 1 to 1, 20 to 1, 21 to 2, 40 to 2, 41 to 3, 60 to 3, 79 to 4, 80 to 4, 81 to 5, 100 to 5)
        expected.forEach { (percent, lit) -> assertEquals("$percent%", lit, BatteryBar.lit(percent)) }
        assertEquals(0, BatteryBar.lit(-3))
        assertEquals(5, BatteryBar.lit(140))
        assertEquals(2, BatteryBar.lit(50, dots = 3))
    }

    @Test
    fun `draws the charge, then the used-up dots, in fill order`() {
        val settings = BatterySettings(edge = BatteryEdge.TOP, drain = BatteryDrain.LEFT_TO_RIGHT, brightness = 80, emptyBrightness = 4)
        val frame = digital.frame(LocalTime.of(10, 10), true, battery = BatteryGauge(settings, 50))
        // Draining left to right, the right-hand three are what is left. 80 % and 4 % as matrix levels.
        assertEquals(listOf(10, 10, 204, 204, 204), (4..8).map { frame[it, 0] })
    }

    @Test
    fun `the highest dot's charge is what is left of its share`() {
        assertEquals(1.0, BatteryBar.highestDotCharge(80), 1e-9)
        assertEquals(0.5, BatteryBar.highestDotCharge(70), 1e-9)
        assertEquals(0.05, BatteryBar.highestDotCharge(81), 1e-9)
        assertEquals(1.0, BatteryBar.highestDotCharge(100), 1e-9)
        assertEquals(0.05, BatteryBar.highestDotCharge(1), 1e-9)
        assertEquals(0.0, BatteryBar.highestDotCharge(0), 1e-9)
        assertEquals(0.5, BatteryBar.highestDotCharge(50, dots = 3), 1e-9)
    }

    /** The top row at [percent] with [settings] on the top edge. */
    private fun topRow(settings: BatterySettings, percent: Int) =
        digital.frame(LocalTime.NOON, true, battery = BatteryGauge(settings.copy(edge = BatteryEdge.TOP), percent))
            .let { frame -> (4..8).map { frame[it, 0] } }

    @Test
    fun `fading dims the highest dot by what is left of its share`() {
        val settings = BatterySettings(emptyBrightness = 0, fadeHighestDot = true)
        // At 80% the fourth dot is full; at 70% it holds half its 20%, so half as bright.
        assertEquals(listOf(255, 255, 255, 255, 0), topRow(settings, 80))
        assertEquals(listOf(255, 255, 255, 128, 0), topRow(settings, 70))
        // Just into the fifth dot: 5% of its share, 5% bright.
        assertEquals(listOf(255, 255, 255, 255, 13), topRow(settings, 81))
        assertEquals(listOf(255, 255, 255, 255, 255), topRow(settings, 100))
        assertEquals(listOf(0, 0, 0, 0, 0), topRow(settings, 0))
        // Draining the other way, the highest dot is the one on the left.
        assertEquals(listOf(0, 128, 255, 255, 255), topRow(settings.copy(drain = BatteryDrain.LEFT_TO_RIGHT), 70))
    }

    @Test
    fun `a fading dot goes from charge towards used-up brightness, never below it`() {
        val settings = BatterySettings(brightness = 100, emptyBrightness = 10, fadeHighestDot = true)
        // Halfway between 100% and 10% is 55%; used up is 10%.
        assertEquals(listOf(255, 255, 255, 140, 26), topRow(settings, 70))
        assertEquals(listOf(255, 255, 255, 255, 37), topRow(settings, 81))
    }

    @Test
    fun `without fading the highest dot is as bright as the rest`() {
        assertEquals(listOf(255, 255, 255, 255, 0), topRow(BatterySettings(emptyBrightness = 0), 70))
    }

    @Test
    fun `used-up dots at 0 stay dark`() {
        val settings = BatterySettings(edge = BatteryEdge.BOTTOM, emptyBrightness = 0)
        val frame = digital.frame(LocalTime.of(10, 10), true, battery = BatteryGauge(settings, 21))
        assertEquals(listOf(255, 255, 0, 0, 0), (4..8).map { frame[it, 12] })
    }

    @Test
    fun `stays clear of every face and leaves it intact`() {
        val time = LocalTime.of(23, 58)
        faces.forEach { face ->
            BatteryEdge.entries.forEach { edge ->
                val settings = BatterySettings(edge = edge, emptyBrightness = 0)
                val bar = BatteryBar.cells(face, settings)
                assertEquals("$edge", BatteryBar.DOTS, bar.size)
                bar.forEach { (x, y) ->
                    assertTrue("$edge ($x,$y) is off the matrix", face.isLed(x, y))
                    assertTrue("$edge ($x,$y) is inside the face", !face.occupies(x, y))
                }
                val plain = face.render(time, true)
                val full = face.frame(time, true, battery = BatteryGauge(settings, 100))
                for (y in 0 until 13) for (x in 0 until 13) {
                    val expected = if (x to y in bar) 255 else plain[x, y]
                    assertEquals("$edge ($x,$y)", expected, full[x, y])
                }
            }
        }
    }

    @Test
    fun `the pulse ring goes round the bar, so a pulse never covers the reading`() {
        val settings = BatterySettings(edge = BatteryEdge.TOP, emptyBrightness = 0)
        val layout = AddonLayout(digital, settings)
        val bar = layout.battery.toSet()
        assertEquals(NotificationPulse.ring(digital).toSet() - bar, layout.ring.toSet())
        assertEquals(32 - BatteryBar.DOTS, layout.ring.size)

        val frame = digital.frame(LocalTime.of(12, 0), true, pulseLevel = 90, battery = BatteryGauge(settings, 40), layout = layout)
        assertEquals(listOf(255, 255, 0, 0, 0), (4..8).map { frame[it, 0] })
        layout.ring.forEach { (x, y) -> assertEquals("($x,$y)", 90, frame[x, y]) }
    }

    @Test
    fun `with the addon off the layout has no bar and the whole ring`() {
        val layout = AddonLayout(digital, null)
        assertTrue(layout.battery.isEmpty())
        assertEquals(NotificationPulse.ring(digital), layout.ring)
    }
}
