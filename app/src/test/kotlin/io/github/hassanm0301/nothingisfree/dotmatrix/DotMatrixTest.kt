package io.github.hassanm0301.nothingisfree.dotmatrix

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DotMatrixTest {

    @Test
    fun `set and get clamp to range and ignore out of bounds`() {
        val m = DotMatrix(3, 3)
        m[1, 1] = 999
        m[-1, 0] = 255
        m[3, 3] = 255
        assertEquals(255, m[1, 1])
        assertEquals(0, m[-1, 0])
        assertEquals(0, m[3, 3])
    }

    @Test
    fun `toFrame is row major and a copy`() {
        val m = DotMatrix(2, 2)
        m[1, 0] = 10
        m[0, 1] = 20
        val frame = m.toFrame()
        assertArrayEquals(intArrayOf(0, 10, 20, 0), frame)
        frame[0] = 99
        assertEquals(0, m[0, 0])
    }

    @Test
    fun `text lays glyphs out with a one dot gap`() {
        val text = "10"
        val width = DotMatrix.measure(text)
        assertEquals(5 + 1 + 5, width)
        val m = DotMatrix(width, 7)
        assertEquals(width, m.text(text))
        // '1' column 2 top row is lit; the gap column between glyphs stays dark.
        assertEquals(255, m[2, 0])
        for (y in 0 until 7) assertEquals(0, m[5, y])
    }

    @Test
    fun `a 13x13 frame matches the Phone 4a Pro matrix length`() {
        val m = DotMatrix(13, 13)
        m.text("4A", x = 1, y = 3)
        assertEquals(169, m.toFrame().size)
        assertTrue(m.toFrame().any { it > 0 })
    }
}
