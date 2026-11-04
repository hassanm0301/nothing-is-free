package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.dotmatrix.DotTypeface
import java.time.LocalTime

/**
 * The Digital clock toy's frame for a round [size]x[size] Glyph Matrix: hours stacked over minutes
 * in the middle, in [font] (see [ClockFont]). The edge is left free for addons such as the
 * notification pulse.
 *
 * Each line is centred on its own, so a font with narrow digits (the minimal `1`) stays centred.
 * [occupies] is the box the widest two digits need, the same every minute.
 */
class ClockFace(override val size: Int, val font: ClockFont = ClockFont.STANDARD) : ToyFace {
    private val typeface: DotTypeface = font.typeface(size)
    private val blockWidth = 2 * ('0'..'9').maxOf { typeface[it].width } + 1
    private val digitHeight = typeface['0'].height
    private val blockHeight = digitHeight * 2 + 1
    private val left = (size - blockWidth) / 2
    private val top = (size - blockHeight) / 2

    override fun occupies(x: Int, y: Int): Boolean =
        x in left until left + blockWidth && y in top until top + blockHeight

    /** Whether ([x], [y]) is a physical LED, assuming the matrix is the circle inscribed in the square. */
    fun inCircle(x: Int, y: Int): Boolean = RoundMatrix.isLed(size, x, y)

    override fun render(time: LocalTime, is24Hour: Boolean): DotMatrix {
        val hour = if (is24Hour) time.hour else (time.hour + 11) % 12 + 1
        return DotMatrix(size, size).apply {
            line(hour, top)
            line(time.minute, top + digitHeight + 1)
        }
    }

    private fun DotMatrix.line(value: Int, y: Int) {
        val digits = value.toString().padStart(2, '0')
        // Rounds down, as the block itself does, so full-width lines land exactly on `left`.
        val x = left + (blockWidth - DotMatrix.measure(digits, font = typeface)) / 2
        text(digits, x = x, y = y, font = typeface)
    }
}
