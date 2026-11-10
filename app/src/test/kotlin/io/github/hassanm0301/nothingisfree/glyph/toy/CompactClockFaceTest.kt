package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotFont
import io.github.hassanm0301.nothingisfree.dotmatrix.DotFontMinimal
import io.github.hassanm0301.nothingisfree.dotmatrix.DotFontSmall
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class CompactClockFaceTest {

    private val face = CompactClockFace(13)

    @Test
    fun `shows the hour's second digit and the minute's first`() {
        assertEquals('2' to '4', CompactClockFace.digits(LocalTime.of(12, 46), is24Hour = true))
        assertEquals('9' to '4', CompactClockFace.digits(LocalTime.of(9, 41), is24Hour = true))
        assertEquals('3' to '5', CompactClockFace.digits(LocalTime.of(23, 58), is24Hour = true))
        assertEquals('0' to '0', CompactClockFace.digits(LocalTime.of(10, 5), is24Hour = true))
        assertEquals('0' to '0', CompactClockFace.digits(LocalTime.of(0, 5), is24Hour = true))
    }

    @Test
    fun `12 hour clock uses the 12 hour hour`() {
        assertEquals('2' to '4', CompactClockFace.digits(LocalTime.of(12, 46), is24Hour = false))
        assertEquals('2' to '0', CompactClockFace.digits(LocalTime.of(0, 5), is24Hour = false))
        assertEquals('1' to '0', CompactClockFace.digits(LocalTime.of(13, 5), is24Hour = false))
        assertEquals('3' to '0', CompactClockFace.digits(LocalTime.of(13, 5), is24Hour = true))
        assertEquals('1' to '5', CompactClockFace.digits(LocalTime.of(23, 59), is24Hour = false))
    }

    @Test
    fun `13x13 draws two 5x7 digits centred`() {
        val expected = DotMatrix(13, 13).apply { text("94", x = 1, y = 3, font = DotFont) }
        assertEquals(expected.toFrame().toList(), face.render(LocalTime.of(9, 41), true).toFrame().toList())
    }

    @Test
    fun `every digit pair fits the round matrix and leaves the edge free`() {
        for (size in listOf(13, 25)) for (font in CompactClockFont.entries) {
            val f = CompactClockFace(size, CompactClockSettings(font))
            for (h in 0..23) for (m in 0..9) {
                f.render(LocalTime.of(h, m), true).forEach { x, y, v ->
                    if (v > 0) {
                        assertTrue("$size $font: ($x,$y) is off the matrix", f.isLed(x, y))
                        assertTrue("$size $font: ($x,$y) is outside the face", f.occupies(x, y))
                    }
                }
            }
            assertFalse(f.occupies(size / 2, 0))
            assertFalse(f.occupies(0, size / 2))
        }
    }

    @Test
    fun `large font by default`() {
        assertEquals(CompactClockFont.LARGE, CompactClockSettings().font)
    }

    @Test
    fun `standard font draws the Digital clock's 3x5 digits centred`() {
        val standard = CompactClockFace(13, CompactClockSettings(CompactClockFont.STANDARD))
        val expected = DotMatrix(13, 13).apply { text("94", x = 3, y = 4, font = DotFontSmall) }
        assertEquals(expected.toFrame().toList(), standard.render(LocalTime.of(9, 41), true).toFrame().toList())
    }

    @Test
    fun `standard font is the Digital clock's at every size, undoubled`() {
        for (size in listOf(13, 25)) {
            assertEquals(ClockFont.STANDARD.typeface(size), CompactClockFont.STANDARD.typeface(size))
            assertEquals(1, CompactClockFont.STANDARD.scale(size))
        }
        val big = CompactClockFace(25, CompactClockSettings(CompactClockFont.STANDARD))
        val expected = DotMatrix(25, 25).apply { text("94", x = 7, y = 9, font = DotFont) }
        assertEquals(expected.toFrame().toList(), big.render(LocalTime.of(9, 41), true).toFrame().toList())
    }

    @Test
    fun `minimal font draws the minimal digits and centres a narrow 1`() {
        val minimal = CompactClockFace(13, CompactClockSettings(CompactClockFont.MINIMAL))
        val wide = DotMatrix(13, 13).apply { text("94", x = 3, y = 4, font = DotFontMinimal) }
        assertEquals(wide.toFrame().toList(), minimal.render(LocalTime.of(9, 41), true).toFrame().toList())
        // "11" is 3 wide in a 7-wide box.
        val narrow = DotMatrix(13, 13).apply { text("11", x = 5, y = 4, font = DotFontMinimal) }
        assertEquals(narrow.toFrame().toList(), minimal.render(LocalTime.of(11, 10), true).toFrame().toList())
    }

    @Test
    fun `small fonts occupy the same 7x5 box whatever the time`() {
        for (font in listOf(CompactClockFont.STANDARD, CompactClockFont.MINIMAL)) {
            val f = CompactClockFace(13, CompactClockSettings(font))
            for (x in 0 until 13) for (y in 0 until 13) {
                assertEquals("$font ($x,$y)", x in 3..9 && y in 4..8, f.occupies(x, y))
            }
        }
    }

    @Test
    fun `25x25 draws each dot as a 2x2 block`() {
        val frame = CompactClockFace(25).render(LocalTime.of(9, 41), true)
        // '9' at left = 2, top = 5: its top row is .###., so (4..9, 5..6) are lit and (2..3, 5) dark.
        for (x in 4..9) for (y in 5..6) assertEquals(255, frame[x, y])
        assertEquals(0, frame[2, 5])
        assertEquals(0, frame[3, 6])
    }
}
