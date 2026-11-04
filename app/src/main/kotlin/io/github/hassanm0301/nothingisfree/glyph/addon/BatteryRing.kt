package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.dotmatrix.Brightness
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyFace
import kotlin.math.PI
import kotlin.math.atan2

/** The battery ring to show: the phone's charge, 0..100, drawn with [settings]. */
data class RingGauge(val settings: BatteryRingSettings, val percent: Int)

/**
 * The battery ring addon: the outermost line of LEDs, all the way around the round matrix, shows
 * the phone's charge. Full, the whole ring is lit. As the charge drops it empties from the top,
 * the used-up part sweeping [RingDirection.CLOCKWISE] or anticlockwise until only the dot just
 * before the top is left. Each dot is an equal share of the charge and stays lit until its share
 * is used up, so the last one shows down to 1%.
 */
object BatteryRing {

    /**
     * The ring's dots on [face], in the order they light: the first holds the last of the charge,
     * the last (the top one) goes out first. The ring is the matrix edge, less what the face
     * occupies and [exclude] (the battery bar's dots, when that addon is on too).
     */
    fun cells(
        face: ToyFace,
        settings: BatteryRingSettings,
        exclude: Set<Pair<Int, Int>> = emptySet(),
    ): List<Pair<Int, Int>> {
        val c = (face.size - 1) / 2.0
        // The order the ring empties in: from the top, round the way it drains.
        val drainOrder = NotificationPulse.ring(face, exclude).sortedBy { (x, y) ->
            val clockwise = angle(x - c, y - c)
            if (settings.direction == RingDirection.CLOCKWISE) clockwise else (2 * PI - clockwise) % (2 * PI)
        }
        return drainOrder.reversed()
    }

    /**
     * Clockwise angle of ([dx], [dy]) from straight up, in 0 until 2π (y grows downwards). The top
     * dot is 0, so it goes out first whichever way the ring drains.
     */
    private fun angle(dx: Double, dy: Double): Double {
        val a = atan2(dx, -dy)
        return if (a < 0) a + 2 * PI else a
    }

    /** How many of [dots] are lit at [percent]: each holds an equal share, lit until it is used up. */
    fun lit(percent: Int, dots: Int): Int = BatteryBar.lit(percent, dots)

    /** Draw [gauge] on [frame] over [ring], the cells from [cells]: lit dots, then the used-up ones. */
    fun draw(frame: DotMatrix, ring: List<Pair<Int, Int>>, gauge: RingGauge) {
        val lit = lit(gauge.percent, ring.size)
        val on = Brightness.level(gauge.settings.brightness)
        val off = Brightness.level(gauge.settings.emptyBrightness)
        ring.forEachIndexed { i, (x, y) -> frame[x, y] = if (i < lit) on else off }
    }
}
