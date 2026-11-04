package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.core.battery.BatteryState
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

class BinaryBatteryTest {

    private val digital = ClockFace(13)
    private val binary = BinaryClockFace(13).with(BinaryClockSettings(style = BitStyle.SQUARE, showUnusedBits = true))

    /** Every face layout the app has, on the 13x13 matrix. */
    private val faces: List<ToyFace> = listOf(digital) + BitStyle.entries.flatMap { style ->
        listOf(true, false).map { gap ->
            BinaryClockFace(13).with(BinaryClockSettings(style = style, dotGap = gap, showUnusedBits = true))
        }
    } + CompactClockFace(13)

    private fun cells(edge: BatteryEdge, face: ToyFace) = BinaryBattery.cells(face, BinaryBatterySettings(edge = edge))

    private fun on(vararg bits: Int) = bits.map { it == 1 }

    @Test
    fun `the tens and units of the percentage in BCD, then the status dots`() {
        // 73%: tens 7 = 0111 and charging, units 3 = 0011 and no battery saver.
        assertEquals(on(0, 1, 1, 1, 1, 0, 0, 1, 1, 0), BinaryBattery.dots(BatteryState(73, charging = true)))
        // 100%: tens 10 = 1010, units 0.
        assertEquals(on(1, 0, 1, 0, 0, 0, 0, 0, 0, 1), BinaryBattery.dots(BatteryState(100, powerSave = true)))
        assertEquals(on(0, 0, 0, 0, 0, 1, 0, 0, 1, 0), BinaryBattery.dots(BatteryState(9)))
        assertEquals(on(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), BinaryBattery.dots(BatteryState(0)))
    }

    @Test
    fun `every percentage reads back`() {
        for (p in 0..100) {
            val d = BinaryBattery.dots(BatteryState(p))
            fun digit(from: Int) = (0 until 4).sumOf { i -> if (d[from + i]) 8 shr i else 0 }
            assertEquals("$p%", p, digit(0) * 10 + digit(5))
        }
    }

    @Test
    fun `on a side it is two columns, tens on the left, bits top to bottom`() {
        val rows = 4..8
        assertEquals(rows.map { 0 to it } + rows.map { 1 to it }, cells(BatteryEdge.LEFT, digital))
        assertEquals(rows.map { 11 to it } + rows.map { 12 to it }, cells(BatteryEdge.RIGHT, digital))
    }

    @Test
    fun `at the top or bottom it is two rows, tens above, bits left to right`() {
        val columns = 4..8
        assertEquals(columns.map { it to 0 } + columns.map { it to 1 }, cells(BatteryEdge.TOP, binary))
        assertEquals(columns.map { it to 11 } + columns.map { it to 12 }, cells(BatteryEdge.BOTTOM, binary))
    }

    @Test
    fun `moves to a side where the face leaves no room, and stays put where it fits`() {
        val settings = { edge: BatteryEdge -> BinaryBatterySettings(edge = edge) }
        // The Digital clock's digits take rows 1 to 11.
        assertEquals(BatteryEdge.LEFT, BinaryBattery.placement(digital, settings(BatteryEdge.TOP)))
        assertEquals(BatteryEdge.LEFT, BinaryBattery.placement(digital, settings(BatteryEdge.BOTTOM)))
        assertEquals(BatteryEdge.RIGHT, BinaryBattery.placement(digital, settings(BatteryEdge.RIGHT)))
        BatteryEdge.entries.forEach { edge -> assertEquals(edge, BinaryBattery.placement(binary, settings(edge))) }
    }

    @Test
    fun `stays clear of every face on every edge, ten LEDs each time`() {
        faces.forEach { face ->
            BatteryEdge.entries.forEach { edge ->
                val block = cells(edge, face)
                assertEquals("$edge", 10, block.toSet().size)
                block.forEach { (x, y) ->
                    assertTrue("$edge ($x,$y) is off the matrix", face.isLed(x, y))
                    assertTrue("$edge ($x,$y) is inside the face", !face.occupies(x, y))
                }
            }
        }
    }

    @Test
    fun `draws set bits at active and clear ones at inactive brightness, leaving the face alone`() {
        val look = BinaryBatterySettings(edge = BatteryEdge.LEFT, activeBrightness = 100, inactiveBrightness = 10)
        val time = LocalTime.of(12, 34)
        val frame = digital.frame(time, true, battery = BatteryGauge(look, BatteryState(73, charging = true)))
        // 10 % is matrix level 26.
        assertEquals(listOf(26, 255, 255, 255, 255), (4..8).map { frame[0, it] })
        assertEquals(listOf(26, 26, 255, 255, 26), (4..8).map { frame[1, it] })
        val plain = digital.render(time, true)
        val block = cells(BatteryEdge.LEFT, digital).toSet()
        for (y in 0 until 13) for (x in 0 until 13) {
            if (x to y !in block) assertEquals("($x,$y)", plain[x, y], frame[x, y])
        }
    }

    @Test
    fun `the pulse ring goes round the block`() {
        val layout = AddonLayout(digital, BinaryBatterySettings(edge = BatteryEdge.LEFT))
        val block = layout.battery.toSet()
        assertEquals(NotificationPulse.ring(digital).toSet() - block, layout.ring.toSet())
        assertTrue(layout.ring.none { it in block })
    }

    @Test
    fun `25x25 centres the block on the outermost line`() {
        val rows = 10..14
        assertEquals(rows.map { 0 to it } + rows.map { 1 to it }, cells(BatteryEdge.LEFT, ClockFace(25)))
    }
}
