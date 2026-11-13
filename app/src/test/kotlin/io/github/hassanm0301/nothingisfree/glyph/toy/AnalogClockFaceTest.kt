package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.addon.BatteryBar
import io.github.hassanm0301.nothingisfree.glyph.addon.BatterySettings
import io.github.hassanm0301.nothingisfree.glyph.addon.BinaryBattery
import io.github.hassanm0301.nothingisfree.glyph.addon.BinaryBatterySettings
import io.github.hassanm0301.nothingisfree.glyph.addon.NotificationPulse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import kotlin.math.abs

class AnalogClockFaceTest {

    private class BlankFace(override val size: Int) : ToyFace {
        override fun render(time: LocalTime, is24Hour: Boolean) = DotMatrix(size, size)
        override fun occupies(x: Int, y: Int) = false
    }

    private val face = AnalogClockFace(13)

    /** The lit dots of [face] at [hour]:[minute]. */
    private fun lit(hour: Int, minute: Int, face: AnalogClockFace = this.face): Set<Pair<Int, Int>> = buildSet {
        face.render(LocalTime.of(hour, minute), true).forEach { x, y, v -> if (v > 0) add(x to y) }
    }

    @Test
    fun `13x13 has a minute hand of 5 and an hour hand of 3`() {
        assertEquals(5, face.minuteLength)
        assertEquals(3, face.hourLength)
        assertEquals(11, AnalogClockFace(25).minuteLength)
        assertEquals(7, AnalogClockFace(25).hourLength)
    }

    @Test
    fun `hands point straight on the quarters`() {
        assertEquals((1..6).map { 6 to it }, face.minuteHand(0).reversed())
        assertEquals((6..11).map { it to 6 }, face.minuteHand(15))
        assertEquals((6..11).map { 6 to it }, face.minuteHand(30))
        assertEquals((1..6).map { it to 6 }, face.minuteHand(45).reversed())

        assertEquals((3..6).map { 6 to it }, face.hourHand(12, 0).reversed())
        assertEquals((6..9).map { it to 6 }, face.hourHand(3, 0))
        assertEquals((6..9).map { 6 to it }, face.hourHand(6, 0))
        assertEquals((3..6).map { it to 6 }, face.hourHand(9, 0).reversed())
    }

    @Test
    fun `ten past ten`() {
        // Hour hand up and to the left, minute hand up and to the right, from the centre dot.
        assertEquals(listOf(6 to 6, 5 to 5, 4 to 5), face.hourHand(10, 10))
        assertEquals(listOf(6 to 6, 7 to 5, 8 to 5, 9 to 4, 10 to 4), face.minuteHand(10))
        assertEquals((face.hourHand(10, 10) + face.minuteHand(10)).toSet(), lit(10, 10))
    }

    @Test
    fun `six o'clock is one line, longer above the centre`() {
        assertEquals((1..9).map { 6 to it }.toSet(), lit(6, 0))
    }

    @Test
    fun `the hour hand is always the shorter one`() {
        for (size in listOf(13, 25)) {
            val f = AnalogClockFace(size)
            val shortestMinute = (0 until 60).minOf { f.minuteHand(it).size }
            for (h in 0..11) for (m in 0 until 60) {
                assertTrue("$size $h:$m", f.hourHand(h, m).size < shortestMinute)
            }
        }
    }

    @Test
    fun `the hour hand moves with the minutes`() {
        assertNotEquals(face.hourHand(3, 0), face.hourHand(3, 30))
        assertEquals(face.hourHand(4, 0), face.hourHand(3, 60))
        // Half past one is exactly on the diagonal.
        assertEquals(listOf(6 to 6, 7 to 5, 8 to 4), face.hourHand(1, 30))
    }

    @Test
    fun `a dial is the same morning and afternoon, 12 or 24 hour`() {
        for (m in listOf(0, 17, 59)) {
            val morning = face.render(LocalTime.of(3, m), true).toFrame().toList()
            assertEquals(morning, face.render(LocalTime.of(15, m), true).toFrame().toList())
            assertEquals(morning, face.render(LocalTime.of(15, m), false).toFrame().toList())
        }
        assertEquals(lit(0, 0), lit(12, 0))
    }

    @Test
    fun `hands start at the centre and their dots touch`() {
        for (size in listOf(13, 25)) {
            val f = AnalogClockFace(size)
            val hands = (0 until 60).map { f.minuteHand(it) } +
                (0..11).flatMap { h -> (0 until 60).map { m -> f.hourHand(h, m) } }
            for (hand in hands) {
                assertEquals(size / 2 to size / 2, hand.first())
                hand.zipWithNext { (ax, ay), (bx, by) ->
                    assertTrue("$size: $hand", abs(ax - bx) <= 1 && abs(ay - by) <= 1 && (ax != bx || ay != by))
                }
            }
        }
    }

    @Test
    fun `mirrored times draw mirror images`() {
        for (size in listOf(13, 25)) {
            val f = AnalogClockFace(size)
            fun List<Pair<Int, Int>>.mirrored() = map { (x, y) -> size - 1 - x to y }
            for (m in 1 until 60) assertEquals("$size :$m", f.minuteHand(m).mirrored(), f.minuteHand(60 - m))
            for (h in 0..11) for (m in 0 until 60) {
                val back = 12 * 60 - (h * 60 + m)
                assertEquals("$size $h:$m", f.hourHand(h, m).mirrored(), f.hourHand(back / 60, back % 60))
            }
        }
    }

    @Test
    fun `nearly every minute has a minute hand of its own`() {
        // On 13x13 only 7 and 8 past and their three mirror images share a hand (the diagonal).
        val byHand = (0 until 60).groupBy { face.minuteHand(it) }.values
        assertEquals(56, byHand.size)
        val shared = setOf(listOf(7, 8), listOf(22, 23), listOf(37, 38), listOf(52, 53))
        assertEquals(shared, byHand.filter { it.size > 1 }.toSet())
        assertEquals(60, (0 until 60).map { AnalogClockFace(25).minuteHand(it) }.distinct().size)
    }

    @Test
    fun `every frame stays on the face and off the edge`() {
        for (size in listOf(13, 25)) {
            val f = AnalogClockFace(size)
            val edge = NotificationPulse.ring(BlankFace(size)).toSet()
            for (h in 0..11) for (m in 0 until 60) {
                for ((x, y) in lit(h, m, f)) {
                    assertTrue("$size $h:$m: ($x,$y) is off the matrix", f.isLed(x, y))
                    assertTrue("$size $h:$m: ($x,$y) is outside the face", f.occupies(x, y))
                    assertFalse("$size $h:$m: ($x,$y) is on the edge", x to y in edge)
                }
            }
            // So the pulse ring and the battery ring keep the whole edge.
            assertEquals(edge, NotificationPulse.ring(f).toSet())
        }
    }

    @Test
    fun `13x13 keeps every dot but the edge`() {
        val edge = NotificationPulse.ring(BlankFace(13)).toSet()
        for (y in 0 until 13) for (x in 0 until 13) {
            assertEquals("($x,$y)", face.isLed(x, y) && x to y !in edge, face.occupies(x, y))
        }
        assertFalse(face.occupies(-1, 6))
        assertFalse(face.occupies(13, 6))
    }

    @Test
    fun `the battery bar fits on the edge, the binary battery nowhere`() {
        assertEquals(BatteryBar.DOTS, BatteryBar.cells(face, BatterySettings()).size)
        assertNull(BinaryBattery.placement(face, BinaryBatterySettings()))
    }

    @Test
    fun `each hand has its own brightness and the brighter shows where they overlap`() {
        val f = AnalogClockFace(13, AnalogClockSettings(hourBrightness = 40, minuteBrightness = 100))
        val three = f.render(LocalTime.of(3, 0), true)
        assertEquals(102, three[9, 6]) // hour hand, 40 %
        assertEquals(255, three[6, 1]) // minute hand
        assertEquals(255, three[6, 6]) // the centre belongs to both

        // At noon the hour hand lies under the minute hand.
        val dimMinute = AnalogClockFace(13, AnalogClockSettings(hourBrightness = 100, minuteBrightness = 40))
        val noon = dimMinute.render(LocalTime.of(12, 0), true)
        assertEquals(255, noon[6, 3])
        assertEquals(102, noon[6, 2])
    }

    @Test
    fun `brightness is kept within 1 to 100`() {
        assertEquals(AnalogClockSettings(1, 100), AnalogClockSettings(0, 250).normalized())
        assertEquals(AnalogClockSettings(100, 100), AnalogClockSettings())
        val f = AnalogClockFace(13, AnalogClockSettings(hourBrightness = 0, minuteBrightness = -5))
        assertTrue(f.render(LocalTime.of(3, 0), true)[9, 6] > 0)
    }
}
