package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotFont
import io.github.hassanm0301.nothingisfree.dotmatrix.DotFontSmall
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.dotmatrix.DotTypeface
import java.time.LocalTime

/**
 * The clock toy's frame for a round [size]x[size] Glyph Matrix: hours stacked over minutes in the
 * middle, and an outer ring that carries the notification pulse.
 *
 * 13 (Phone (4a) Pro) uses the 3x5 [DotFontSmall]; 25 (Phone (3)) has room for the 5x7 [DotFont].
 */
class ClockFace(val size: Int) {
    private val font: DotTypeface = if (size >= 21) DotFont else DotFontSmall
    private val blockWidth = DotMatrix.measure("00", font = font)
    private val digitHeight = font['0'].height
    private val blockHeight = digitHeight * 2 + 1
    private val left = (size - blockWidth) / 2
    private val top = (size - blockHeight) / 2

    /** Edge dots of the round matrix, minus any that would touch the digits. */
    val ring: List<Pair<Int, Int>> = (0 until size)
        .flatMap { y -> (0 until size).map { x -> x to y } }
        .filter { (x, y) ->
            val edge = inCircle(x, y) && listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1)
                .any { (nx, ny) -> !inCircle(nx, ny) }
            val inDigits = x in left until left + blockWidth && y in top until top + blockHeight
            edge && !inDigits
        }

    /** Whether ([x], [y]) is a physical LED, assuming the matrix is the circle inscribed in the square. */
    fun inCircle(x: Int, y: Int): Boolean = RoundMatrix.isLed(size, x, y)

    /** Draw [time] with the ring at [ringLevel] (0..255, 0 = off). */
    fun render(time: LocalTime, is24Hour: Boolean, ringLevel: Int = 0): DotMatrix {
        val hour = if (is24Hour) time.hour else (time.hour + 11) % 12 + 1
        return DotMatrix(size, size).apply {
            text(hour.toString().padStart(2, '0'), x = left, y = top, font = font)
            text(time.minute.toString().padStart(2, '0'), x = left, y = top + digitHeight + 1, font = font)
            if (ringLevel > 0) ring.forEach { (x, y) -> this[x, y] = ringLevel }
        }
    }
}
