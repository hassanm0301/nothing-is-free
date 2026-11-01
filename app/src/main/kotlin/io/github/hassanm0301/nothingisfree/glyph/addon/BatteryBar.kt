package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.dotmatrix.Brightness
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyFace

/** The battery level to show: the phone's charge, 0..100, drawn with [settings]. */
data class BatteryGauge(val settings: BatterySettings, val percent: Int)

/**
 * The battery level addon: a line of [DOTS] dots on the outermost row or column of the round
 * matrix, on the edge picked in [BatterySettings]. Each dot is an equal share of the charge (20%
 * for five), and stays lit until its share is used up, so a single dot shows down to 1%.
 */
object BatteryBar {
    const val DOTS = 5

    /**
     * The bar's dots on [face], in the order they light: the first holds the last of the charge,
     * the last goes out first. On a 13-wide matrix they are the whole outermost line; on a wider
     * one, its middle [DOTS]. Dots the face occupies are left out.
     */
    fun cells(face: ToyFace, settings: BatterySettings): List<Pair<Int, Int>> {
        val n = face.size
        val horizontal = settings.edge.horizontal
        val fromNear = settings.edge == BatteryEdge.TOP || settings.edge == BatteryEdge.LEFT
        // Lines from the chosen edge inwards; the bar takes the first with room for all its dots.
        val leds = (0 until n).asSequence()
            .map { i -> if (fromNear) i else n - 1 - i }
            .map { line ->
                (0 until n).map { j -> if (horizontal) j to line else line to j }.filter { (x, y) -> face.isLed(x, y) }
            }
            .firstOrNull { it.size >= DOTS } ?: return emptyList()
        // Left to right, or top to bottom.
        val bar = leds.drop((leds.size - DOTS) / 2).take(DOTS)
        // Emptying right to left, the left-hand dot lights first. Otherwise the far end does: the
        // right-hand dot, or on the sides the bottom one, like a battery standing upright.
        val fillOrder = if (horizontal && settings.drain == BatteryDrain.RIGHT_TO_LEFT) bar else bar.reversed()
        return fillOrder.filterNot { (x, y) -> face.occupies(x, y) }
    }

    /** How many of [dots] are lit at [percent]: each holds an equal share, lit until it is used up. */
    fun lit(percent: Int, dots: Int = DOTS): Int = (percent.coerceIn(0, 100) * dots + 99) / 100

    /**
     * How much of the highest lit dot's share is left at [percent], 0..1: 1 when it is full (80%
     * on five dots), 0.5 when half used (70%), and 0 with nothing lit.
     */
    fun highestDotCharge(percent: Int, dots: Int = DOTS): Double {
        val p = percent.coerceIn(0, 100)
        val lit = lit(p, dots)
        if (lit == 0) return 0.0
        // In hundredths of one dot's share.
        return (p * dots - (lit - 1) * 100) / 100.0
    }

    /**
     * Draw [gauge] on [frame] over [bar], the cells from [cells]: lit dots, then the used-up ones.
     * With [BatterySettings.fadeHighestDot], the highest lit dot sits between the two brightnesses
     * by how much of its share is left.
     */
    fun draw(frame: DotMatrix, bar: List<Pair<Int, Int>>, gauge: BatteryGauge) {
        val s = gauge.settings
        val lit = lit(gauge.percent, bar.size)
        val on = Brightness.level(s.brightness)
        val off = Brightness.level(s.emptyBrightness)
        val highest = if (s.fadeHighestDot) {
            val left = highestDotCharge(gauge.percent, bar.size)
            Brightness.level(s.emptyBrightness + (s.brightness - s.emptyBrightness) * left)
        } else {
            on
        }
        bar.forEachIndexed { i, (x, y) ->
            frame[x, y] = when {
                i >= lit -> off
                i == lit - 1 -> highest
                else -> on
            }
        }
    }
}

/**
 * Where the addons draw on [face]: the battery bar (with [battery] settings, or none when the
 * addon is off), and the notification pulse ring on the rest of the edge, so the pulse never
 * covers the reading. Depends only on the face and the bar's edge; toys work it out once per face
 * rather than per frame.
 */
class AddonLayout(face: ToyFace, battery: BatterySettings?) {
    val bar: List<Pair<Int, Int>> = battery?.let { BatteryBar.cells(face, it) }.orEmpty()
    val ring: List<Pair<Int, Int>> = NotificationPulse.ring(face, exclude = bar.toSet())
}
