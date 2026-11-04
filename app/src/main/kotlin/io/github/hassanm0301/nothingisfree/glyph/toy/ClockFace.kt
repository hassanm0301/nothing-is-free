package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.Brightness
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.dotmatrix.DotTypeface
import java.time.LocalTime

/**
 * The Digital clock toy's frame for a round [size]x[size] Glyph Matrix: hours stacked over minutes
 * in the middle, in the [settings]' font (see [ClockFont]) and brightness. The edge is left free
 * for addons such as the notification pulse.
 *
 * Each line is centred on its own, so a font with narrow digits (the minimal `1`) stays centred.
 * [occupies] is the box the widest two digits need, the same every minute.
 */
class ClockFace(
    override val size: Int,
    val settings: DigitalClockSettings = DigitalClockSettings(),
) : ToyFace {
    private val font: DotTypeface = settings.font.typeface(size)
    private val blockWidth = 2 * ('0'..'9').maxOf { font[it].width } + 1
    private val digitHeight = font['0'].height
    private val blockHeight = digitHeight * 2 + 1
    private val left = (size - blockWidth) / 2
    private val top = (size - blockHeight) / 2
    private val level = Brightness.level(settings.normalized().brightness)

    override fun occupies(x: Int, y: Int): Boolean =
        x in left until left + blockWidth && y in top until top + blockHeight

    /** [value] as two digits, centred in the block, at row [y]. */
    private fun DotMatrix.digits(value: Int, y: Int) {
        val pair = value.toString().padStart(2, '0')
        // Rounds down, as the block itself does, so full-width lines land exactly on `left`.
        val x = left + (blockWidth - DotMatrix.measure(pair, font = font)) / 2
        text(pair, x = x, y = y, brightness = level, font = font)
    }

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
