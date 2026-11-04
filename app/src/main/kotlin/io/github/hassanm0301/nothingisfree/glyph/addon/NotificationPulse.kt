package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyFace
import java.time.LocalTime

/**
 * The notification pulse addon: the outer ring of the round matrix breathes (see [PulseWave]) for
 * a while after a notification. Works on any [ToyFace]: the ring is the matrix edge minus
 * whatever the face occupies, and minus the battery addon's dots when one is on too (bar the
 * battery ring, which shares the whole edge: the pulse covers it while it plays).
 */
object NotificationPulse {

    /** Edge LEDs of the round matrix that [face] leaves free, less [exclude] (other addons' dots). */
    fun ring(face: ToyFace, exclude: Set<Pair<Int, Int>> = emptySet()): List<Pair<Int, Int>> = (0 until face.size)
        .flatMap { y -> (0 until face.size).map { x -> x to y } }
        .filter { (x, y) ->
            val edge = face.isLed(x, y) &&
                listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1).any { (nx, ny) -> !face.isLed(nx, ny) }
            edge && !face.occupies(x, y) && (x to y) !in exclude
        }

    /** Light [ring] at [level] (0..255) on [frame]; 0 leaves it untouched. */
    fun draw(frame: DotMatrix, ring: List<Pair<Int, Int>>, level: Int) {
        if (level > 0) ring.forEach { (x, y) -> frame[x, y] = level }
    }
}

/**
 * The full frame a toy shows: [face] at [time], plus the addons: the notification pulse at
 * [pulseLevel] (0..255, 0 = none) and a [battery] addon (null = none). [layout] is where they
 * go; pass one worked out ahead to save doing it per frame. The toy services and the in-app
 * previews both draw through this.
 */
fun ToyFace.frame(
    time: LocalTime,
    is24Hour: Boolean,
    pulseLevel: Int = 0,
    battery: BatteryGauge? = null,
    layout: AddonLayout = AddonLayout(this, battery?.look),
): DotMatrix = render(time, is24Hour).also { frame ->
    battery?.draw(frame, layout.battery)
    NotificationPulse.draw(frame, layout.ring, pulseLevel)
}
