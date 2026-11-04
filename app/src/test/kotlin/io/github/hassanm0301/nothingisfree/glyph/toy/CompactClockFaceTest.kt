package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotFont
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class CompactClockFaceTest {

    private val face = CompactClockFace(13)

    @Test
    fun `shows the hour's first digit and the minute's second`() {
        assertEquals('0' to '1', CompactClockFace.digits(LocalTime.of(9, 41), is24Hour = true))
        assertEquals('2' to '8', CompactClockFace.digits(LocalTime.of(23, 58), is24Hour = true))
        assertEquals('1' to '0', CompactClockFace.digits(LocalTime.of(10, 0), is24Hour = true))
        assertEquals('0' to '5', CompactClockFace.digits(LocalTime.of(0, 5), is24Hour = true))
    }

    @Test
    fun `12 hour clock uses the 12 hour hour`() {
        assertEquals('1' to '5', CompactClockFace.digits(LocalTime.of(0, 5), is24Hour = false))
        assertEquals('0' to '0', CompactClockFace.digits(LocalTime.of(13, 0), is24Hour = false))
        assertEquals('1' to '9', CompactClockFace.digits(LocalTime.of(23, 59), is24Hour = false))
    }

    @Test
    fun `13x13 draws two 5x7 digits centred`() {
        val expected = DotMatrix(13, 13).apply { text("01", x = 1, y = 3, font = DotFont) }
        assertEquals(expected.toFrame().toList(), face.render(LocalTime.of(9, 41), true).toFrame().toList())
    }

    @Test
    fun `every digit pair fits the round matrix and leaves the edge free`() {
        for (size in listOf(13, 25)) {
            val f = CompactClockFace(size)
            for (h in 0..23) for (m in 0..9) {
                f.render(LocalTime.of(h, m), true).forEach { x, y, v ->
                    if (v > 0) {
                        assertTrue("$size: ($x,$y) is off the matrix", f.isLed(x, y))
                        assertTrue("$size: ($x,$y) is outside the face", f.occupies(x, y))
                    }
                }
            }
            assertFalse(f.occupies(size / 2, 0))
            assertFalse(f.occupies(0, size / 2))
        }
    }

    @Test
    fun `25x25 draws each dot as a 2x2 block`() {
        val frame = CompactClockFace(25).render(LocalTime.of(9, 41), true)
        // '0' at left = 2, top = 5: its top row is .###., so (4..9, 5..6) are lit and (2..3, 5) dark.
        for (x in 4..9) for (y in 5..6) assertEquals(255, frame[x, y])
        assertEquals(0, frame[2, 5])
        assertEquals(0, frame[3, 6])
    }
}
