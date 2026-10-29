package io.github.hassanm0301.nothingisfree.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.PillShape
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Monochrome switch: on = ink track with paper thumb, off = hairline track with muted thumb. */
@Composable
fun NothingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val nothing = NothingTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = scheme.background,
            checkedTrackColor = scheme.onBackground,
            checkedBorderColor = scheme.onBackground,
            uncheckedThumbColor = nothing.muted,
            uncheckedTrackColor = nothing.card,
            uncheckedBorderColor = nothing.hairline,
        ),
    )
}

/** Monochrome slider over whole numbers: ink thumb and track, hairline for the rest. */
@Composable
fun NothingSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val nothing = NothingTheme.colors
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt().coerceIn(range)) },
        modifier = modifier,
        enabled = enabled,
        valueRange = range.first.toFloat()..range.last.toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = scheme.onBackground,
            activeTrackColor = scheme.onBackground,
            inactiveTrackColor = nothing.hairline,
            disabledThumbColor = nothing.muted,
            disabledActiveTrackColor = nothing.muted,
            disabledInactiveTrackColor = nothing.hairline,
        ),
    )
}

/** Pill chip with an uppercase mono label. */
@Composable
fun NothingChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val nothing = NothingTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = PillShape,
        label = { Text(text.uppercase(), style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = scheme.onBackground,
            selectedContainerColor = scheme.onBackground,
            selectedLabelColor = scheme.background,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = nothing.hairline,
            selectedBorderColor = scheme.onBackground,
        ),
    )
}

/** Settings-style row: title, optional subtitle, trailing slot. */
@Composable
fun NothingListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(Modifier.padding(end = Spacing.md)) { leading() }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                MonoLabel(subtitle, Modifier.padding(top = Spacing.xs), style = MaterialTheme.typography.labelSmall)
            }
        }
        if (trailing != null) {
            Box(Modifier.padding(start = Spacing.md)) { trailing() }
        }
    }
}

/**
 * Circular progress: a dotted track and a solid accent arc. The look of Nothing's battery
 * and activity widgets.
 */
@Composable
fun NothingProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    stroke: Dp = 6.dp,
    color: Color = NothingTheme.colors.accent,
    track: Color = NothingTheme.colors.dotOff,
    dots: Int = 36,
    content: @Composable BoxScopeAlias.() -> Unit = {},
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.toPx()
            val r = (this.size.minDimension - sw) / 2f
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            for (i in 0 until dots) {
                val a = Math.toRadians(360.0 * i / dots - 90.0)
                drawCircle(track, sw / 2f, Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat()))
            }
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
        content(this)
    }
}

typealias BoxScopeAlias = androidx.compose.foundation.layout.BoxScope

/** A horizontal row of dots lit up to [value] of [max]; the "steps" style bar. */
@Composable
fun DotBar(
    value: Int,
    max: Int,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    gap: Dp = 6.dp,
    on: Color = NothingTheme.colors.dotOn,
    off: Color = NothingTheme.colors.dotOff,
) {
    Canvas(modifier.fillMaxWidth().size(dotSize)) {
        val step = (dotSize + gap).toPx()
        val r = dotSize.toPx() / 2f
        val count = (size.width / step).toInt().coerceAtLeast(1)
        val lit = (count * value.toFloat() / max.coerceAtLeast(1)).toInt()
        for (i in 0 until count) {
            drawCircle(if (i < lit) on else off, r, Offset(r + i * step, size.height / 2f))
        }
    }
}

/** Outline-only rounded tile; useful for empty/placeholder slots such as a future widget. */
@Composable
fun PlaceholderTile(label: String, modifier: Modifier = Modifier) {
    val nothing = NothingTheme.colors
    androidx.compose.material3.OutlinedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, nothing.hairline),
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
    ) {
        Box(Modifier.fillMaxWidth().padding(Spacing.lg), contentAlignment = Alignment.Center) {
            MonoLabel(label)
        }
    }
}
