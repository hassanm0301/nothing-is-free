package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.dotmatrix.DotFontSmall
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.toy.BinaryClockFace
import io.github.hassanm0301.nothingisfree.glyph.toy.BinaryClockSettings
import io.github.hassanm0301.nothingisfree.glyph.toy.BitStyle
import io.github.hassanm0301.nothingisfree.glyph.toy.ClockFace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class NotificationPulseTest {

    private val digital = ClockFace(13)

    @Test
    fun `ring hugs the circle edge without touching the digital clock digits`() {
        val digitCells = DotMatrix(13, 13).apply {
            text("88", x = 3, y = 1, font = DotFontSmall)
            text("88", x = 3, y = 7, font = DotFontSmall)
        }
        val ring = NotificationPulse.ring(digital)
        assertEquals(32, ring.size)
        ring.forEach { (x, y) ->
            assertTrue("($x,$y) is off the matrix", digital.isLed(x, y))
            assertEquals("($x,$y) overlaps a digit", 0, digitCells[x, y])
        }
        assertTrue(6 to 0 in ring)
        assertTrue(0 to 6 in ring)
        assertFalse(0 to 0 in ring)
    }

    @Test
    fun `ring takes the given level, stays dark at zero, and leaves the face alone`() {
        val time = LocalTime.of(12, 34)
        val ring = NotificationPulse.ring(digital)
        val lit = digital.frame(time, true, pulseLevel = 128)
        val dark = digital.frame(time, true, pulseLevel = 0)
        ring.forEach { (x, y) ->
            assertEquals(128, lit[x, y])
            assertEquals(0, dark[x, y])
        }
        assertEquals(digital.render(time, true).toFrame().toList(), dark.toFrame().toList())
    }

    @Test
    fun `ring works on the binary clock in every layout and stays clear of its bits`() {
        val binary = BinaryClockFace(13)
        BitStyle.entries.forEach { style ->
            listOf(true, false).forEach { gap ->
                val face = binary.with(BinaryClockSettings(style = style, dotGap = gap, showUnusedBits = true))
                val ring = NotificationPulse.ring(face)
                assertTrue("$style gap=$gap has no ring", ring.size >= 32)
                val bits = face.render(LocalTime.of(23, 59), true)
                ring.forEach { (x, y) -> assertEquals("$style gap=$gap ($x,$y) overlaps a bit", 0, bits[x, y]) }
            }
        }
    }

    @Test
    fun `25x25 has a ring too`() {
        assertTrue(NotificationPulse.ring(ClockFace(25)).isNotEmpty())
    }
}
