package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotFontSmall
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ClockFaceTest {

    private val face = ClockFace(13)

    /** The digits block alone, drawn the same way, to compare against. */
    private fun digits(hh: String, mm: String) = DotMatrix(13, 13).apply {
        text(hh, x = 3, y = 1, font = DotFontSmall)
        text(mm, x = 3, y = 7, font = DotFontSmall)
    }

    @Test
    fun `13x13 stacks hours over minutes in 3x5 digits`() {
        val frame = face.render(LocalTime.of(9, 41), is24Hour = true)
        assertEquals(digits("09", "41").toFrame().toList(), frame.toFrame().toList())
    }

    @Test
    fun `12 hour clock shows 12 at midnight and 01 after noon`() {
        assertEquals(digits("12", "05").toFrame().toList(), face.render(LocalTime.of(0, 5), false).toFrame().toList())
        assertEquals(digits("01", "00").toFrame().toList(), face.render(LocalTime.of(13, 0), false).toFrame().toList())
        assertEquals(digits("13", "00").toFrame().toList(), face.render(LocalTime.of(13, 0), true).toFrame().toList())
    }

    @Test
    fun `ring hugs the circle edge without touching the digits`() {
        val digitCells = digits("88", "88")
        assertEquals(32, face.ring.size)
        face.ring.forEach { (x, y) ->
            assertTrue("($x,$y) is off the matrix", face.inCircle(x, y))
            assertEquals("($x,$y) overlaps a digit", 0, digitCells[x, y])
        }
        assertTrue(6 to 0 in face.ring)
        assertTrue(0 to 6 in face.ring)
        assertFalse(0 to 0 in face.ring)
    }

    @Test
    fun `ring takes the given level and stays dark at zero`() {
        val time = LocalTime.of(12, 34)
        val lit = face.render(time, true, ringLevel = 128)
        val dark = face.render(time, true, ringLevel = 0)
        face.ring.forEach { (x, y) ->
            assertEquals(128, lit[x, y])
            assertEquals(0, dark[x, y])
        }
    }

    @Test
    fun `corners of the round matrix are not LEDs`() {
        assertFalse(face.inCircle(0, 0))
        assertFalse(face.inCircle(12, 12))
        assertTrue(face.inCircle(6, 6))
        assertTrue(face.inCircle(4, 0))
        assertFalse(face.inCircle(3, 0))
    }

    @Test
    fun `25x25 uses the big font and still fits`() {
        val big = ClockFace(25)
        val frame = big.render(LocalTime.of(23, 59), true, ringLevel = 255)
        frame.forEach { x, y, v -> if (v > 0) assertTrue("($x,$y) is off the matrix", big.inCircle(x, y)) }
        assertTrue(big.ring.isNotEmpty())
    }
}
