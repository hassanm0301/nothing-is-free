package io.github.hassanm0301.nothingisfree.dotmatrix

import kotlin.math.roundToInt

/**
 * Brightness as the user sets it, 0..100 %, against the 0..255 levels a [DotMatrix] holds. Every
 * brightness setting in the app is a percentage; frames convert with [level].
 */
object Brightness {
    const val MAX_PERCENT = 100

    /** The [DotMatrix] level for [percent]. */
    fun level(percent: Int): Int = level(percent.toDouble())

    fun level(percent: Double): Int = (percent.coerceIn(0.0, 100.0) * 255 / 100).roundToInt()

    /** The nearest percentage to a 0..255 [level], for settings saved on the old scale. */
    fun percent(level: Int): Int = (level.coerceIn(0, 255) * 100.0 / 255).roundToInt()
}
