package io.github.hassanm0301.nothingisfree.ui.glyph

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrixCanvas
import io.github.hassanm0301.nothingisfree.glyph.toy.BinaryClockFace
import io.github.hassanm0301.nothingisfree.glyph.toy.BinaryClockSettings
import io.github.hassanm0301.nothingisfree.glyph.toy.BitStyle
import io.github.hassanm0301.nothingisfree.ui.components.DotDivider
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.components.NothingListRow
import io.github.hassanm0301.nothingisfree.ui.components.NothingSlider
import io.github.hassanm0301.nothingisfree.ui.components.NothingSwitch
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Settings and a live preview for the "Binary clock" Glyph Toy. The preview draws the exact frame
 * the toy sends to a 13x13 matrix, so style and brightness changes show here and on the phone at
 * the same time.
 */
@Composable
fun BinaryClockCard(
    settings: BinaryClockSettings,
    onChange: ((BinaryClockSettings) -> BinaryClockSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val face = remember { BinaryClockFace(13) }
    val is24Hour = DateFormat.is24HourFormat(context)
    val time by produceState(LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    val frame = remember(time.hour, time.minute, is24Hour, settings) { face.render(time, is24Hour, settings) }
    val label = time.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "hh:mm"))

    NothingCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoLabel("Binary clock · BCD", Modifier.weight(1f))
            MonoLabel(label, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(Spacing.lg))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DotMatrixCanvas(frame, Modifier.width(168.dp), isDot = face::inCircle)
        }
        Spacer(Modifier.height(Spacing.sm))
        MonoLabel(
            "Columns H H M M · rows 8 4 2 1",
            Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(Spacing.lg))

        MonoLabel("Bits")
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            NothingChip("Dot", settings.style == BitStyle.DOT, onClick = { onChange { it.copy(style = BitStyle.DOT) } })
            NothingChip("2×2 square", settings.style == BitStyle.SQUARE, onClick = { onChange { it.copy(style = BitStyle.SQUARE) } })
        }
        if (settings.style == BitStyle.DOT) {
            Spacer(Modifier.height(Spacing.md))
            MonoLabel("Spacing")
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                NothingChip("Gap", settings.dotGap, onClick = { onChange { it.copy(dotGap = true) } })
                NothingChip("Touching", !settings.dotGap, onClick = { onChange { it.copy(dotGap = false) } })
            }
        }
        Spacer(Modifier.height(Spacing.sm))

        NothingListRow(
            "Show unused bits",
            subtitle = "Bits no digit can reach",
            trailing = {
                NothingSwitch(settings.showUnusedBits, { on -> onChange { it.copy(showUnusedBits = on) } })
            },
        )
        DotDivider()
        Spacer(Modifier.height(Spacing.md))

        BrightnessControl("Active brightness", settings.activeBrightness) { v ->
            onChange { it.copy(activeBrightness = v) }
        }
        Spacer(Modifier.height(Spacing.md))
        BrightnessControl("Inactive brightness", settings.inactiveBrightness) { v ->
            onChange { it.copy(inactiveBrightness = v) }
        }
        Spacer(Modifier.height(Spacing.lg))

        Text(
            "Pick it in Settings › Glyph Interface › Flip to Glyph › Always-on Glyph Toy › Binary clock.",
            style = MaterialTheme.typography.bodyMedium,
            color = NothingTheme.colors.muted,
        )
    }
}

/** A 0..255 brightness: label and value, then a slider with ±1 steps for exact values. */
@Composable
private fun BrightnessControl(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MonoLabel(label, Modifier.weight(1f))
        MonoLabel("$value / 255", color = MaterialTheme.colorScheme.onBackground)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Decrease $label", enabled = value > 0) {
            onValueChange(value - 1)
        }
        NothingSlider(value, onValueChange, range = 0..255, modifier = Modifier.weight(1f))
        StepButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Increase $label", enabled = value < 255) {
            onValueChange(value + 1)
        }
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
