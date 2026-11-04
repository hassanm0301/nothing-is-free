package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotFont
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import java.time.LocalTime

/**
 * The Compact digital clock toy's frame for a round [size]x[size] Glyph Matrix: just two digits
 * side by side in the 5x7 [DotFont], the first digit of the hour (tens) and the second digit of
 * the minute (units). 09:41 shows `01`, 23:58 shows `28`.
 *
 * The hour is zero-padded like the Digital clock's, so on a 12-hour clock its first digit is 0 or
 * 1. 13 (Phone (4a) Pro) draws the digits 1:1; 25 (Phone (3)) draws each dot as a 2x2 block. The
 * edge is left free for addons.
 */
class CompactClockFace(override val size: Int) : ToyFace {
    private val scale = if (size >= 21) 2 else 1
    private val digitWidth = DotFont['0'].width * scale
    private val blockWidth = digitWidth * 2 + GAP
    private val blockHeight = DotFont['0'].height * scale
    private val left = (size - blockWidth) / 2
    private val top = (size - blockHeight) / 2

    override fun occupies(x: Int, y: Int): Boolean =
        x in left until left + blockWidth && y in top until top + blockHeight

    override fun render(time: LocalTime, is24Hour: Boolean): DotMatrix {
        val (first, second) = digits(time, is24Hour)
        return DotMatrix(size, size).apply {
            digit(first, left)
            digit(second, left + digitWidth + GAP)
        }
    }

    private fun DotMatrix.digit(c: Char, x: Int) {
        val glyph = DotFont[c]
        for (gy in 0 until glyph.height) for (gx in 0 until glyph.width) {
            if (!glyph[gx, gy]) continue
            for (dy in 0 until scale) for (dx in 0 until scale) {
                this[x + gx * scale + dx, top + gy * scale + dy] = 255
            }
        }
    }

    companion object {
        /** Dark columns between the two digits. */
        private const val GAP = 1

        /** The two digits shown at [time]: the hour's tens digit, then the minute's units digit. */
        fun digits(time: LocalTime, is24Hour: Boolean): Pair<Char, Char> {
            val hour = if (is24Hour) time.hour else (time.hour + 11) % 12 + 1
            return hour.toString().padStart(2, '0')[0] to time.minute.toString().padStart(2, '0')[1]
        }
    }
}
