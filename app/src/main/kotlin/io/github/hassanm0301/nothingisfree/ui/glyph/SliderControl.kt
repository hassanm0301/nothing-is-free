package io.github.hassanm0301.nothingisfree.ui.glyph

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingSlider
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import java.util.Locale

/**
 * A whole number in [range]: label and [format]ted value, then a slider with ±1 steps for exact
 * values. [onStep] gets the step (+1 or -1) rather than a new value, so it applies to the latest
 * stored value.
 */
@Composable
internal fun SliderControl(
    label: String,
    value: Int,
    range: IntRange,
    format: (Int) -> String,
    onValueChange: (Int) -> Unit,
    onStep: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MonoLabel(label, Modifier.weight(1f))
        MonoLabel(format(value), color = MaterialTheme.colorScheme.onBackground)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Decrease $label", enabled = value > range.first) {
            onStep(-1)
        }
        NothingSlider(value, onValueChange, range = range, modifier = Modifier.weight(1f))
        StepButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Increase $label", enabled = value < range.last) {
            onStep(1)
        }
    }
}

/** A brightness, 0..100 %, in steps of 1 %. */
@Composable
internal fun PercentControl(label: String, percent: Int, onValueChange: (Int) -> Unit, onStep: (Int) -> Unit) =
    SliderControl(label, percent, 0..100, { "$it%" }, onValueChange, onStep)

/** A time in [rangeMs], in steps of 0.1 s; [onStep] gets ±100 ms. */
@Composable
internal fun TimeControl(label: String, ms: Long, rangeMs: LongRange, onValueChange: (Long) -> Unit, onStep: (Long) -> Unit) =
    SliderControl(
        label,
        (ms / 100).toInt(),
        (rangeMs.first / 100).toInt()..(rangeMs.last / 100).toInt(),
        { formatTenths(it.toLong()) },
        onValueChange = { onValueChange(it * 100L) },
        onStep = { onStep(it * 100L) },
    )

/** A duration for the UI, to the tenth of a second: "0.5 s", "12.3 s", "1 min 30.0 s". */
internal fun formatMs(ms: Long): String = formatTenths((ms + 50) / 100)

private fun formatTenths(tenths: Long): String {
    val seconds = tenths / 10.0
    return if (tenths >= 600) {
        String.format(Locale.ROOT, "%d min %.1f s", tenths / 600, (tenths % 600) / 10.0)
    } else {
        String.format(Locale.ROOT, "%.1f s", seconds)
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) MaterialTheme.colorScheme.onBackground else NothingTheme.colors.hairline,
        )
    }
}
