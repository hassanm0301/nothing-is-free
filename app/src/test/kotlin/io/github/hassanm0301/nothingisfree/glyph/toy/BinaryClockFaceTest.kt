package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class BinaryClockFaceTest {

    private val face = BinaryClockFace(13)
    private val squares = BinaryClockSettings(style = BitStyle.SQUARE)
    private val dots = BinaryClockSettings(style = BitStyle.DOT, dotGap = true)
    private val packedDots = BinaryClockSettings(style = BitStyle.DOT, dotGap = false)

    /** 21:47 as columns H H M M, rows 8-4-2-1: `#` set, `o` clear, `.` a bit the digit never uses. */
    private val bits2147 = listOf(
        ". o . o",
        ". o # #",
        "# o o #",
        "o # o #",
    )

    /**
     * The frame [rows] should give when bit (col, row) covers [bit]x[bit] dots with its top-left
     * at [origin] + [pitch] * (col, row).
     */
    private fun expected(
        rows: List<String>,
        origin: Int,
        pitch: Int,
        bit: Int,
        size: Int = 13,
        active: Int = 255,
        inactive: Int = 24,
        unused: Int = 0,
    ) = DotMatrix(size, size).apply {
        rows.forEachIndexed { row, line ->
            line.filter { it != ' ' }.forEachIndexed { col, c ->
                val level = when (c) {
                    '#' -> active
                    'o' -> inactive
                    else -> unused
                }
                for (dy in 0 until bit) for (dx in 0 until bit) {
                    this[origin + col * pitch + dx, origin + row * pitch + dy] = level
                }
            }
        }
    }

    private fun DotMatrix.cells() = toFrame().toList()

    private fun DotMatrix.lit(): Set<Pair<Int, Int>> = buildSet {
        forEach { x, y, v -> if (v > 0) add(x to y) }
    }

    private fun square(from: Int, until: Int, step: Int = 1): Set<Pair<Int, Int>> =
        (from until until step step).flatMap { y -> (from until until step step).map { x -> x to y } }.toSet()

    @Test
    fun `21-47 in squares lights 2x2 blocks for the set bits`() {
        val frame = face.render(LocalTime.of(21, 47), is24Hour = true, squares)
        assertEquals(expected(bits2147, origin = 3, pitch = 2, bit = 2).cells(), frame.cells())
    }

    @Test
    fun `21-47 in dots lights one LED per set bit`() {
        val frame = face.render(LocalTime.of(21, 47), is24Hour = true, dots)
        assertEquals(expected(bits2147, origin = 3, pitch = 2, bit = 1).cells(), frame.cells())
    }

    @Test
    fun `12 hour clock shows 12 at midnight and 01 after noon`() {
        // With unused bits shown, the hours "2" bit looks the same either way: clear on a 24-hour
        // clock, unused on a 12-hour one.
        val shown = squares.copy(showUnusedBits = true)
        fun same(twelve: LocalTime, twentyFour: LocalTime) = assertEquals(
            face.render(twentyFour, is24Hour = true, shown).cells(),
            face.render(twelve, is24Hour = false, shown).cells(),
        )
        same(LocalTime.of(21, 47), LocalTime.of(9, 47))
        same(LocalTime.of(0, 5), LocalTime.of(12, 5))
        same(LocalTime.of(13, 0), LocalTime.of(1, 0))
        assertNotEquals(
            face.render(LocalTime.of(13, 0), is24Hour = true, squares).cells(),
            face.render(LocalTime.of(1, 0), is24Hour = true, squares).cells(),
        )
    }

    @Test
    fun `12 hour clock leaves the hours 2 bit dark as unused`() {
        // Squares: origin 3, columns 2 wide; the hours-tens "2" bit is the third row (y 7..8).
        val twentyFour = face.render(LocalTime.of(9, 47), is24Hour = true, squares)
        val twelve = face.render(LocalTime.of(9, 47), is24Hour = false, squares)
        assertEquals(squares.inactiveBrightness, twentyFour[3, 7])
        assertEquals(0, twelve[3, 7])
        assertEquals(squares.inactiveBrightness, twelve[3, 9])
    }

    @Test
    fun `squares touch with no gaps and ignore the dot gap`() {
        val all = squares.copy(showUnusedBits = true)
        val time = LocalTime.of(21, 47)
        val frame = face.render(time, is24Hour = true, all.copy(dotGap = true))
        assertEquals(square(3, 11), frame.lit())
        assertEquals(frame.cells(), face.render(time, is24Hour = true, all.copy(dotGap = false)).cells())
    }

    @Test
    fun `dot gap leaves a dark LED between bits, and packs them when off`() {
        val time = LocalTime.of(21, 47)
        val gapped = face.render(time, is24Hour = true, dots.copy(showUnusedBits = true))
        assertEquals(square(3, 10, step = 2), gapped.lit())
        val packed = face.render(time, is24Hour = true, packedDots.copy(showUnusedBits = true))
        assertEquals(square(5, 9), packed.lit())
        assertEquals(expected(bits2147, origin = 5, pitch = 1, bit = 1, unused = 24).cells(), packed.cells())
    }

    @Test
    fun `unused bits stay dark unless shown, then take the inactive level`() {
        val time = LocalTime.of(21, 47)
        assertEquals(
            expected(bits2147, origin = 3, pitch = 2, bit = 2, unused = 0).cells(),
            face.render(time, is24Hour = true, squares.copy(showUnusedBits = false)).cells(),
        )
        assertEquals(
            expected(bits2147, origin = 3, pitch = 2, bit = 2, unused = 24).cells(),
            face.render(time, is24Hour = true, squares.copy(showUnusedBits = true)).cells(),
        )
    }

    @Test
    fun `brightness passes through and inactive 0 leaves clear bits dark`() {
        val time = LocalTime.of(21, 47)
        val custom = squares.copy(activeBrightness = 200, inactiveBrightness = 60)
        assertEquals(
            expected(bits2147, origin = 3, pitch = 2, bit = 2, active = 200, inactive = 60).cells(),
            face.render(time, is24Hour = true, custom).cells(),
        )
        val off = custom.copy(inactiveBrightness = 0, showUnusedBits = true)
        val frame = face.render(time, is24Hour = true, off)
        assertEquals(
            expected(bits2147, origin = 3, pitch = 2, bit = 2, active = 200, inactive = 0).cells(),
            frame.cells(),
        )
        // Six set bits, four LEDs each, and nothing else.
        assertEquals(6 * 4, frame.lit().size)
    }

    @Test
    fun `every lit LED is on the round 13x13 matrix at every minute`() {
        val combos = BitStyle.entries.flatMap { style ->
            listOf(true, false).flatMap { gap ->
                listOf(true, false).map { unused ->
                    BinaryClockSettings(style = style, dotGap = gap, showUnusedBits = unused)
                }
            }
        }
        for (settings in combos) for (minute in 0 until 24 * 60) {
            val time = LocalTime.of(minute / 60, minute % 60)
            face.render(time, is24Hour = true, settings).forEach { x, y, v ->
                if (v > 0) assertTrue("$settings at $time: ($x,$y) is off the matrix", RoundMatrix.isLed(13, x, y))
            }
        }
    }

    @Test
    fun `25x25 doubles every bit and still fits`() {
        val big = BinaryClockFace(25)
        val time = LocalTime.of(21, 47)
        assertEquals(
            expected(bits2147, origin = 5, pitch = 4, bit = 4, size = 25).cells(),
            big.render(time, is24Hour = true, squares).cells(),
        )
        assertEquals(
            expected(bits2147, origin = 6, pitch = 4, bit = 2, size = 25).cells(),
            big.render(time, is24Hour = true, dots).cells(),
        )
        for (settings in listOf(squares, dots, packedDots)) {
            big.render(time, is24Hour = true, settings.copy(showUnusedBits = true)).forEach { x, y, v ->
                if (v > 0) assertTrue("$settings: ($x,$y) is off the matrix", RoundMatrix.isLed(25, x, y))
            }
        }
    }
}
