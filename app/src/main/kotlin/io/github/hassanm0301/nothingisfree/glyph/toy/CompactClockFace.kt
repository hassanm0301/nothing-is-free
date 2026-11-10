package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.dotmatrix.DotTypeface
import java.time.LocalTime

/**
 * The Compact digital clock toy's frame for a round [size]x[size] Glyph Matrix: just two digits
 * side by side, the second digit of the hour (units) and the first digit of the minute (tens), in
 * the [settings]' font (see [CompactClockFont]). 12:46 shows `24`, 09:41 shows `94`.
 *
 * The hour follows the phone's 12/24-hour setting. The pair is centred, so a font with narrow
 * digits (the minimal `1`) stays centred. [occupies] is the box the widest two digits need, the
 * same every minute; the rest is left free for addons.
 */
class CompactClockFace(
    override val size: Int,
    val settings: CompactClockSettings = CompactClockSettings(),
) : ToyFace {
    private val font: DotTypeface = settings.font.typeface(size)
    private val scale = settings.font.scale(size)
    private val blockWidth = 2 * ('0'..'9').maxOf { font[it].width } * scale + GAP
    private val blockHeight = font['0'].height * scale
    private val left = (size - blockWidth) / 2
    private val top = (size - blockHeight) / 2

    override fun occupies(x: Int, y: Int): Boolean =
        x in left until left + blockWidth && y in top until top + blockHeight

    override fun render(time: LocalTime, is24Hour: Boolean): DotMatrix {
        val (first, second) = digits(time, is24Hour)
        val firstWidth = font[first].width * scale
        val width = firstWidth + GAP + font[second].width * scale
        // Rounds down, as the block itself does, so full-width pairs land exactly on `left`.
        val x = left + (blockWidth - width) / 2
        return DotMatrix(size, size).apply {
            digit(first, x)
            digit(second, x + firstWidth + GAP)
        }
    }

    private fun DotMatrix.digit(c: Char, x: Int) {
        val glyph = font[c]
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

        /** The two digits shown at [time]: the hour's units digit, then the minute's tens digit. */
        fun digits(time: LocalTime, is24Hour: Boolean): Pair<Char, Char> {
            val hour = if (is24Hour) time.hour else (time.hour + 11) % 12 + 1
            return '0' + hour % 10 to '0' + time.minute / 10
        }
    }
}
