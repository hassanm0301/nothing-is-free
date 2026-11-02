package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.dotmatrix.Brightness
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyFace

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
        // Left to right, or top to bottom.
        val bar = EdgeBlock.lines(face, settings.edge, DOTS, depth = 1)?.single() ?: return emptyList()
        // Emptying right to left, the left-hand dot lights first. Otherwise the far end does: the
        // right-hand dot, or on the sides the bottom one, like a battery standing upright.
        val fillOrder = if (settings.edge.horizontal && settings.drain == BatteryDrain.RIGHT_TO_LEFT) bar else bar.reversed()
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
     * Draw [percent] with [s] on [frame] over [bar], the cells from [cells]: lit dots, then the
     * used-up ones. With [BatterySettings.fadeHighestDot], the highest lit dot sits between the two
     * brightnesses by how much of its share is left.
     */
    fun draw(frame: DotMatrix, bar: List<Pair<Int, Int>>, s: BatterySettings, percent: Int) {
        val lit = lit(percent, bar.size)
        val on = Brightness.level(s.brightness)
        val off = Brightness.level(s.emptyBrightness)
        val highest = if (s.fadeHighestDot) {
            val left = highestDotCharge(percent, bar.size)
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
