package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.Brightness
import io.github.hassanm0301.nothingisfree.dotmatrix.DotFont
import io.github.hassanm0301.nothingisfree.dotmatrix.DotFontSmall
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.dotmatrix.DotTypeface
import java.time.LocalTime

/**
 * The Digital clock toy's frame for a round [size]x[size] Glyph Matrix: hours stacked over minutes
 * in the middle, lit at the [settings]' brightness. The edge is left free for addons such as the
 * notification pulse.
 *
 * 13 (Phone (4a) Pro) uses the 3x5 [DotFontSmall]; 25 (Phone (3)) has room for the 5x7 [DotFont].
 */
class ClockFace(
    override val size: Int,
    val settings: DigitalClockSettings = DigitalClockSettings(),
) : ToyFace {
    private val font: DotTypeface = if (size >= 21) DotFont else DotFontSmall
    private val blockWidth = DotMatrix.measure("00", font = font)
    private val digitHeight = font['0'].height
    private val blockHeight = digitHeight * 2 + 1
    private val left = (size - blockWidth) / 2
    private val top = (size - blockHeight) / 2
    private val level = Brightness.level(settings.normalized().brightness)

    override fun occupies(x: Int, y: Int): Boolean =
        x in left until left + blockWidth && y in top until top + blockHeight

    /** [value] as two digits, left-aligned to the block, at row [y]. */
    private fun DotMatrix.digits(value: Int, y: Int) =
        text(value.toString().padStart(2, '0'), x = left, y = y, brightness = level, font = font)

    /** Whether ([x], [y]) is a physical LED, assuming the matrix is the circle inscribed in the square. */
    fun inCircle(x: Int, y: Int): Boolean = RoundMatrix.isLed(size, x, y)

    override fun render(time: LocalTime, is24Hour: Boolean): DotMatrix {
        val hour = if (is24Hour) time.hour else (time.hour + 11) % 12 + 1
        return DotMatrix(size, size).apply {
            digits(hour, top)
            digits(time.minute, top + digitHeight + 1)
        }
    }
}
