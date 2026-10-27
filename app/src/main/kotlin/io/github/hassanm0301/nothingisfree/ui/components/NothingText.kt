package io.github.hassanm0301.nothingisfree.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/** Uppercase mono label: the Nothing caption. */
@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NothingTheme.colors.muted,
    style: TextStyle = MaterialTheme.typography.labelMedium,
) {
    Text(text = text.uppercase(), modifier = modifier, color = color, style = style, maxLines = 1)
}

/** Section header: mono label on the left, optional trailing content, hairline below. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.lg, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoLabel(title, Modifier.weight(1f))
        trailing()
    }
    DotDivider(Modifier.padding(bottom = Spacing.sm))
}

/** Dotted hairline; a row of 2dp dots, the recurring Nothing separator. */
@Composable
fun DotDivider(modifier: Modifier = Modifier, color: Color = NothingTheme.colors.hairline) {
    Canvas(modifier.fillMaxWidth().padding(vertical = 2.dp).height(2.dp)) {
        val step = 6.dp.toPx()
        var x = 1.dp.toPx()
        while (x < size.width) {
            drawCircle(color, radius = 1.dp.toPx(), center = Offset(x, size.height / 2))
            x += step
        }
    }
}
