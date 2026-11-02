package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.core.battery.BatteryState
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyFace

/** A battery addon to draw: the one with settings [look], showing [state]. */
data class BatteryGauge(val look: BatteryLook, val state: BatteryState) {
    constructor(look: BatteryLook, percent: Int) : this(look, BatteryState(percent))

    val percent: Int get() = state.percent

    /** Draw on [frame] over [cells], the dots an [AddonLayout] for [look] gives. */
    fun draw(frame: DotMatrix, cells: List<Pair<Int, Int>>) = when (look) {
        is BatterySettings -> BatteryBar.draw(frame, cells, look, state.percent)
        is BinaryBatterySettings -> BinaryBattery.draw(frame, cells, look, state)
    }
}

/**
 * Where the addons draw on [face]: the battery addon whose settings are [look] (none if null),
 * and the notification pulse ring on the rest of the edge, so the pulse never covers the reading.
 * Depends only on the face and the battery addon's settings; toys work it out once per face rather
 * than per frame.
 */
class AddonLayout(face: ToyFace, look: BatteryLook?) {
    /** The battery addon's dots, in the order it draws them; empty with none. */
    val battery: List<Pair<Int, Int>> = when (look) {
        is BatterySettings -> BatteryBar.cells(face, look)
        is BinaryBatterySettings -> BinaryBattery.cells(face, look)
        null -> emptyList()
    }
    val ring: List<Pair<Int, Int>> = NotificationPulse.ring(face, exclude = battery.toSet())
}

/** Blocks of LEDs along one edge of the round matrix, where the battery addons go. */
internal object EdgeBlock {
    /**
     * A block [length] dots long and [depth] lines deep at [edge] of [face], as its lines from the
     * edge inwards, each running left to right (top and bottom) or top to bottom (sides). It sits
     * on the outermost line with [length] LEDs, centred there, and goes [depth] lines in. Null if
     * no line is long enough or a cell of the block is not an LED. The face is not consulted.
     */
    fun lines(face: ToyFace, edge: BatteryEdge, length: Int, depth: Int): List<List<Pair<Int, Int>>>? {
        val n = face.size
        val fromNear = edge == BatteryEdge.TOP || edge == BatteryEdge.LEFT
        fun line(i: Int) = if (fromNear) i else n - 1 - i
        fun cell(line: Int, along: Int) = if (edge.horizontal) along to line else line to along
        val first = (0 until n).firstOrNull { i -> (0 until n).count { j -> cell(line(i), j).isLed(face) } >= length }
            ?: return null
        val leds = (0 until n).filter { j -> cell(line(first), j).isLed(face) }
        val start = leds[(leds.size - length) / 2]
        val block = (0 until depth).map { d -> (start until start + length).map { j -> cell(line(first + d), j) } }
        return block.takeIf { lines -> lines.all { cells -> cells.all { it.isLed(face) } } }
    }

    private fun Pair<Int, Int>.isLed(face: ToyFace) = face.isLed(first, second)
}
