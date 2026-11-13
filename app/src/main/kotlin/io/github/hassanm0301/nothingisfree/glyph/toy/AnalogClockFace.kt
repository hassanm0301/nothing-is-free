package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.Brightness
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The Analog clock toy's frame for a round [size]x[size] Glyph Matrix: a short hour hand and a
 * long minute hand from the centre dot, each one dot wide, at the [settings]' brightnesses.
 *
 * The minute hand reaches the line of dots just inside the edge ([minuteLength]: 5 on the 13x13
 * matrix, 11 on a 25x25), the hour hand about three fifths of that ([hourLength]: 3 and 7). The
 * hour hand moves with the minutes, as on a real clock. A dial has no 24-hour form, so the phone's
 * 12/24-hour setting changes nothing.
 *
 * A hand is the dots nearest its line (see [hand]), so the angle is only as fine as the dots
 * allow: on the 13x13 matrix 56 of the 60 minutes draw a minute hand of their own (7 and 8 past
 * share one, as do their mirror images). [occupies] is every dot a hand can reach, the same every
 * minute: all of the matrix but its edge, which is left free for addons.
 */
class AnalogClockFace(
    override val size: Int,
    val settings: AnalogClockSettings = AnalogClockSettings(),
) : ToyFace {
    private val centre = (size - 1) / 2

    /** How far the minute hand reaches from the centre dot, in dots along a row or column. */
    val minuteLength = size / 2 - 1

    /** How far the hour hand reaches: three fifths of [minuteLength], rounded. */
    val hourLength = (minuteLength * 3 + 2) / 5

    private val hourLevel: Int
    private val minuteLevel: Int

    init {
        val s = settings.normalized()
        hourLevel = Brightness.level(s.hourBrightness)
        minuteLevel = Brightness.level(s.minuteBrightness)
    }

    private val reach = BooleanArray(size * size).also { cells ->
        val hands = (0 until 60).map { minuteHand(it) } + (0 until HALF_DAY_MINUTES).map { hourHand(it / 60, it % 60) }
        hands.flatten().forEach { (x, y) -> cells[y * size + x] = true }
    }

    override fun occupies(x: Int, y: Int): Boolean = x in 0 until size && y in 0 until size && reach[y * size + x]

    /** The minute hand's dots at [minute] past, from the centre out. */
    fun minuteHand(minute: Int): List<Pair<Int, Int>> = hand(minute * DEGREES_PER_MINUTE, minuteLength)

    /** The hour hand's dots at [hour]:[minute], from the centre out; it creeps on with the minutes. */
    fun hourHand(hour: Int, minute: Int): List<Pair<Int, Int>> =
        hand((hour % 12 * 60 + minute) * DEGREES_PER_MINUTE / 12, hourLength)

    /**
     * The dots of a hand [length] long pointing [degrees] clockwise from 12 o'clock, from the
     * centre out. It takes one dot per step along the axis it leans towards (rows for a hand
     * nearer upright, columns for one nearer level), each the nearest to the true line, so
     * consecutive dots always touch. A sloping hand takes fewer steps, to stay about as long as an
     * upright one: at 45 degrees a hand of 5 is four diagonal dots. It stops short of the matrix
     * edge, which only matters on a 25x25, where rounding would put a few sloping minute hands on it.
     */
    private fun hand(degrees: Double, length: Int): List<Pair<Int, Int>> {
        val radians = Math.toRadians(degrees)
        val dx = sin(radians)
        val dy = -cos(radians)
        val major = max(abs(dx), abs(dy))
        return (0..(length * major).roundToInt())
            .map { step ->
                val along = step / major
                centre + nearest(dx * along) to centre + nearest(dy * along)
            }
            .takeWhile { (x, y) -> isInside(x, y) }
    }

    /** Whether ([x], [y]) is an LED inside the edge: one with an LED on all four sides. */
    private fun isInside(x: Int, y: Int): Boolean =
        isLed(x, y) && isLed(x - 1, y) && isLed(x + 1, y) && isLed(x, y - 1) && isLed(x, y + 1)

    /** [v] to the nearest whole dot, halves away from zero, so mirrored hands are mirror images. */
    private fun nearest(v: Double): Int = if (v < 0) -abs(v).roundToInt() else v.roundToInt()

    override fun render(time: LocalTime, is24Hour: Boolean): DotMatrix = DotMatrix(size, size).apply {
        hourHand(time.hour, time.minute).forEach { (x, y) -> this[x, y] = hourLevel }
        // Where the hands cross or lie over each other, the brighter one shows.
        minuteHand(time.minute).forEach { (x, y) -> this[x, y] = max(this[x, y], minuteLevel) }
    }

    private companion object {
        const val DEGREES_PER_MINUTE = 6.0
        const val HALF_DAY_MINUTES = 12 * 60
    }
}
