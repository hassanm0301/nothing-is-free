package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import java.time.LocalTime

/**
 * What a Glyph Toy draws on its own, before any addon: the frame for a time, and the area it
 * keeps for itself. Addons (e.g. the notification pulse) draw only outside [occupies].
 */
interface ToyFace {
    val size: Int

    fun render(time: LocalTime, is24Hour: Boolean): DotMatrix

    /** Whether ([x], [y]) is in the face's drawing area, lit or not. */
    fun occupies(x: Int, y: Int): Boolean

    /** Whether ([x], [y]) is a physical LED of the round matrix. */
    fun isLed(x: Int, y: Int): Boolean = RoundMatrix.isLed(size, x, y)
}
