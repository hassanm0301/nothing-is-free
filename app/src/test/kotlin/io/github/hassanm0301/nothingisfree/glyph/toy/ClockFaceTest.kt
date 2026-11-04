package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotFontMinimal
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
        val frame = big.render(LocalTime.of(23, 59), true)
        frame.forEach { x, y, v -> if (v > 0) assertTrue("($x,$y) is off the matrix", big.inCircle(x, y)) }
    }

    private fun lit(m: DotMatrix): Int {
        var n = 0
        m.forEach { _, _, v -> if (v > 0) n++ }
        return n
    }

    @Test
    fun `minimal font centres each line and keeps the standard box`() {
        val minimal = ClockFace(13, ClockFont.MINIMAL)
        val expected = DotMatrix(13, 13).apply {
            text("11", x = 5, y = 1, font = DotFontMinimal) // 3 wide, centred in the 7-wide block
            text("09", x = 3, y = 7, font = DotFontMinimal)
        }
        assertEquals(expected.toFrame().toList(), minimal.render(LocalTime.of(11, 9), true).toFrame().toList())
        for (y in 0 until 13) for (x in 0 until 13) assertEquals("($x,$y)", face.occupies(x, y), minimal.occupies(x, y))
    }

    @Test
    fun `minimal font lights fewer LEDs than the standard one at every minute`() {
        val minimal = ClockFace(13, ClockFont.MINIMAL)
        for (m in 0 until 24 * 60) {
            val t = LocalTime.of(m / 60, m % 60)
            assertTrue("$t", lit(minimal.render(t, true)) < lit(face.render(t, true)))
        }
    }

    @Test
    fun `every font fits inside its box and the round matrix at both sizes`() {
        for (font in ClockFont.entries) for (size in listOf(13, 25)) {
            val f = ClockFace(size, font)
            for (m in 0 until 24 * 60) {
                f.render(LocalTime.of(m / 60, m % 60), true).forEach { x, y, v ->
                    if (v > 0) assertTrue("$font $size ($x,$y)", f.occupies(x, y) && f.inCircle(x, y))
                }
            }
        }
    }

    @Test
    fun `minimal digits are all distinct`() {
        val shapes = ('0'..'9').map { c ->
            val g = DotFontMinimal[c]
            (0 until g.height).joinToString("/") { y -> (0 until g.width).joinToString("") { x -> if (g[x, y]) "#" else "." } }
        }
        assertEquals(10, shapes.toSet().size)
    }
}
