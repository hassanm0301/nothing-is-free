package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.core.battery.BatteryState
import io.github.hassanm0301.nothingisfree.dotmatrix.Brightness
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyFace

/**
 * The binary battery addon: the exact percentage in binary-coded decimal on a 5x2 block at one
 * edge, read like the Binary clock. One line holds the tens (0..10), the other the units (0..9),
 * each as bits worth 8, 4, 2 and 1, then a status dot: charging on the tens line, battery saver on
 * the units line.
 *
 * At the top or bottom the lines are rows, tens above units, bits left to right with the status
 * dot on the right. On a side they are columns, tens left of units, bits top to bottom with the
 * status dot at the bottom.
 */
object BinaryBattery {
    /** Bits per digit: 8, 4, 2, 1. */
    const val BITS = 4

    /** Dots per line: the bits and a status dot. */
    const val LENGTH = BITS + 1

    /** Where the block goes when the chosen edge has no room beside the face, in this order. */
    private val fallback = listOf(BatteryEdge.LEFT, BatteryEdge.RIGHT, BatteryEdge.TOP, BatteryEdge.BOTTOM)

    /**
     * The edge the block sits on with [face]: the one in [settings] if it fits there, otherwise
     * the first of left, right, top and bottom that does. Null if it fits nowhere. On the 13x13
     * matrix the Digital clock's digits leave no room for a second row at the top or bottom, so
     * there it moves to a side.
     */
    fun placement(face: ToyFace, settings: BinaryBatterySettings): BatteryEdge? =
        (listOf(settings.edge) + fallback).firstOrNull { block(face, it) != null }

    /**
     * The block's dots on [face], in drawing order: the tens line's bits 8, 4, 2, 1 and its status
     * dot (charging), then the units line's (battery saver). Empty if it fits nowhere.
     */
    fun cells(face: ToyFace, settings: BinaryBatterySettings): List<Pair<Int, Int>> =
        placement(face, settings)?.let { block(face, it) }.orEmpty()

    /** The block at [edge], tens line first; null if a dot is off the matrix or inside the face. */
    private fun block(face: ToyFace, edge: BatteryEdge): List<Pair<Int, Int>>? {
        val lines = EdgeBlock.lines(face, edge, LENGTH, depth = 2) ?: return null
        // Outer line first; tens go on the upper row or the left column.
        val tensFirst = if (edge == BatteryEdge.BOTTOM || edge == BatteryEdge.RIGHT) lines.reversed() else lines
        return tensFirst.flatten().takeIf { cells -> cells.none { (x, y) -> face.occupies(x, y) } }
    }

    /** Which of the block's dots are on for [state], in the order of [cells]. */
    fun dots(state: BatteryState): List<Boolean> {
        val percent = state.percent.coerceIn(0, 100)
        fun line(digit: Int, status: Boolean) = (BITS - 1 downTo 0).map { bit -> (digit shr bit) and 1 == 1 } + status
        return line(percent / 10, state.charging) + line(percent % 10, state.powerSave)
    }

    /** Draw [state] with [s] on [frame] over [cells], the dots from [cells]. */
    fun draw(frame: DotMatrix, cells: List<Pair<Int, Int>>, s: BinaryBatterySettings, state: BatteryState) {
        val on = Brightness.level(s.activeBrightness)
        val off = Brightness.level(s.inactiveBrightness)
        cells.zip(dots(state)).forEach { (cell, lit) -> frame[cell.first, cell.second] = if (lit) on else off }
    }
}
