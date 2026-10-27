package io.github.hassanm0301.nothingisfree.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrixCanvas
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * A clock rendered on the shared [DotMatrix] model, so the exact same frame could be pushed to a
 * Glyph Matrix. Width is derived from the text; height is the 5x7 glyph plus a 1-dot margin.
 */
@Composable
fun DotMatrixClock(time: LocalTime, modifier: Modifier = Modifier, showSeconds: Boolean = false) {
    val text = if (showSeconds) time.format(DateTimeFormatter.ofPattern("HH:mm:ss")) else time.format(HHMM)
    val matrix = remember(text) {
        DotMatrix(width = DotMatrix.measure(text) + 2, height = 9).apply { text(text, x = 1, y = 1) }
    }
    DotMatrixCanvas(matrix, modifier)
}
