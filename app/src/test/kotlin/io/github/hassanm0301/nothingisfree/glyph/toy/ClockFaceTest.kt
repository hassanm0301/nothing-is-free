package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.Brightness
import io.github.hassanm0301.nothingisfree.dotmatrix.DotFontSmall
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ClockFaceTest {

    private val face = ClockFace(13)

    /** The digits block alone, drawn the same way at [level], to compare against. */
    private fun digits(hh: String, mm: String, level: Int = 255) = DotMatrix(13, 13).apply {
        text(hh, x = 3, y = 1, brightness = level, font = DotFontSmall)
        text(mm, x = 3, y = 7, brightness = level, font = DotFontSmall)
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

    @Test
    fun `digits light at the brightness setting`() {
        val dim = ClockFace(13, DigitalClockSettings(brightness = 40))
        val frame = dim.render(LocalTime.of(9, 41), is24Hour = true)
        assertEquals(digits("09", "41", Brightness.level(40)).toFrame().toList(), frame.toFrame().toList())
    }

    @Test
    fun `full brightness by default`() {
        assertEquals(100, DigitalClockSettings().brightness)
        assertEquals(face.render(LocalTime.of(9, 41), true).toFrame().toList(), digits("09", "41").toFrame().toList())
    }

    @Test
    fun `brightness stays between 1 and 100 percent`() {
        assertEquals(1, DigitalClockSettings(brightness = 0).normalized().brightness)
        assertEquals(1, DigitalClockSettings(brightness = -5).normalized().brightness)
        assertEquals(100, DigitalClockSettings(brightness = 101).normalized().brightness)
        assertEquals(55, DigitalClockSettings(brightness = 55).normalized().brightness)
        // The face never goes dark, even from settings that skipped normalizing.
        val frame = ClockFace(13, DigitalClockSettings(brightness = 0)).render(LocalTime.of(9, 41), true)
        assertEquals(digits("09", "41", Brightness.level(1)).toFrame().toList(), frame.toFrame().toList())
    }

    @Test
    fun `brightness does not move the digits or the area addons keep clear of`() {
        val dim = ClockFace(13, DigitalClockSettings(brightness = 10))
        for (y in 0 until 13) for (x in 0 until 13) assertEquals(face.occupies(x, y), dim.occupies(x, y))
    }
}
