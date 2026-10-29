package io.github.hassanm0301.nothingisfree.dotmatrix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import kotlin.math.pow

/**
 * Renders a [DotMatrix] as round dots. Lit cells blend from [offColor] to [onColor] by perceived
 * brightness, unlit cells draw [offColor] so the grid itself reads as hardware. Cells where [isDot]
 * is false are left empty, e.g. the corners of a round Glyph Matrix.
 */
@Composable
fun DotMatrixCanvas(
    matrix: DotMatrix,
    modifier: Modifier = Modifier,
    onColor: Color = NothingTheme.colors.dotOn,
    offColor: Color = NothingTheme.colors.dotOff,
    dotFill: Float = 0.68f,
    showOff: Boolean = true,
    isDot: (x: Int, y: Int) -> Boolean = { _, _ -> true },
) {
    Canvas(modifier.aspectRatio(matrix.width.toFloat() / matrix.height)) {
        val cell = size.width / matrix.width
        val radius = cell * dotFill / 2f
        matrix.forEach { x, y, value ->
            if (!isDot(x, y)) return@forEach
            val center = Offset((x + 0.5f) * cell, (y + 0.5f) * cell)
            when {
                value > 0 -> drawCircle(lerp(offColor, onColor, perceived(value)), radius, center)
                showOff -> drawCircle(offColor, radius, center)
            }
        }
    }
}

/**
 * How bright an LED driven at [value] (0..255) looks, 0..1. The eye is far more sensitive at the
 * low end, so a dim 24 on the matrix is clearly visible; a linear preview would hide it.
 */
private fun perceived(value: Int): Float = (value / 255f).pow(1 / 2.2f)
