package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import java.time.LocalTime

/**
 * The binary clock toy's frame: HH:MM in BCD, one column per digit, bits 8-4-2-1 from top to
 * bottom, centred on a round [size]x[size] Glyph Matrix. Set bits light at the active
 * brightness, clear bits at the inactive one.
 *
 * On a 25-wide matrix (Phone (3)) every bit is drawn twice as large.
 */
class BinaryClockFace(val size: Int) {
    private val scale = if (size >= 21) 2 else 1

    fun inCircle(x: Int, y: Int): Boolean = RoundMatrix.isLed(size, x, y)

    fun render(time: LocalTime, is24Hour: Boolean, settings: BinaryClockSettings): DotMatrix {
        val hour = if (is24Hour) time.hour else (time.hour + 11) % 12 + 1
        val digits = intArrayOf(hour / 10, hour % 10, time.minute / 10, time.minute % 10)
        // Bits each digit can need: hours tens 0..2 (0..1 on a 12-hour clock), hours units 0..9,
        // minutes tens 0..5, minutes units 0..9.
        val bitsPerDigit = intArrayOf(if (is24Hour) 2 else 1, 4, 3, 4)

        val (bit, pitch, origin) = layout(settings)

        return DotMatrix(size, size).apply {
            digits.forEachIndexed { col, value ->
                for (row in 0 until ROWS) {
                    val weight = ROWS - 1 - row
                    val level = when {
                        weight >= bitsPerDigit[col] ->
                            if (settings.showUnusedBits) settings.inactiveBrightness else 0
                        (value shr weight) and 1 == 1 -> settings.activeBrightness
                        else -> settings.inactiveBrightness
                    }
                    if (level == 0) continue
                    val x0 = origin + col * pitch
                    val y0 = origin + row * pitch
                    for (dy in 0 until bit) for (dx in 0 until bit) this[x0 + dx, y0 + dy] = level
                }
            }
        }
    }

    /** Whether ([x], [y]) is inside the bit grid for [settings]. */
    fun occupies(x: Int, y: Int, settings: BinaryClockSettings): Boolean {
        val (bit, pitch, origin) = layout(settings)
        val end = origin + (ROWS - 1) * pitch + bit
        return x in origin until end && y in origin until end
    }

    /** This face with fixed [settings], as a [ToyFace]. */
    fun with(settings: BinaryClockSettings): ToyFace = object : ToyFace {
        override val size: Int get() = this@BinaryClockFace.size
        override fun render(time: LocalTime, is24Hour: Boolean) = render(time, is24Hour, settings)
        override fun occupies(x: Int, y: Int) = occupies(x, y, settings)
    }

    /** Bit size, bit pitch and grid origin (same on both axes). */
    private fun layout(settings: BinaryClockSettings): Triple<Int, Int, Int> {
        val bit = scale * if (settings.style == BitStyle.SQUARE) 2 else 1
        val gap = if (settings.style == BitStyle.DOT && settings.dotGap) scale else 0
        val block = ROWS * bit + (ROWS - 1) * gap
        // Rounds up, so an even-sized block sits half a dot right and down of centre.
        return Triple(bit, bit + gap, (size - block + 1) / 2)
    }

    private companion object {
        const val ROWS = 4
    }
}
