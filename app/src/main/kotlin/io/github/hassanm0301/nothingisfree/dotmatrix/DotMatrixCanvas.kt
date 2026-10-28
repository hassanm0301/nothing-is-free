package io.github.hassanm0301.nothingisfree.dotmatrix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme

/**
 * Renders a [DotMatrix] as round dots. Lit cells use [onColor] scaled by brightness,
 * unlit cells draw [offColor] so the grid itself reads as hardware. Cells where [isDot] is false
 * are left empty, e.g. the corners of a round Glyph Matrix.
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
                value > 0 -> drawCircle(onColor.copy(alpha = value / 255f), radius, center)
                showOff -> drawCircle(offColor, radius, center)
            }
        }
    }
}
