package io.github.hassanm0301.nothingisfree.ui.glyph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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

/** The Binary clock toy's options. Changes reach the phone's matrix straight away. */
@Composable
fun BinaryClockOptions(
    settings: BinaryClockSettings,
    onChange: ((BinaryClockSettings) -> BinaryClockSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    NothingCard(modifier) {
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

        BrightnessControl(
            "Active brightness",
            settings.activeBrightness,
            onValueChange = { v -> onChange { it.copy(activeBrightness = v) } },
            onStep = { d -> onChange { it.copy(activeBrightness = (it.activeBrightness + d).coerceIn(0, 255)) } },
        )
        Spacer(Modifier.height(Spacing.md))
        BrightnessControl(
            "Inactive brightness",
            settings.inactiveBrightness,
            onValueChange = { v -> onChange { it.copy(inactiveBrightness = v) } },
            onStep = { d -> onChange { it.copy(inactiveBrightness = (it.inactiveBrightness + d).coerceIn(0, 255)) } },
        )
    }
}

/**
 * A 0..255 brightness: label and value, then a slider with ±1 steps for exact values. [onStep]
 * gets the step (+1 or -1) rather than a new value, so it applies to the latest stored value.
 */
@Composable
private fun BrightnessControl(label: String, value: Int, onValueChange: (Int) -> Unit, onStep: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MonoLabel(label, Modifier.weight(1f))
        MonoLabel("$value / 255", color = MaterialTheme.colorScheme.onBackground)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Decrease $label", enabled = value > 0) {
            onStep(-1)
        }
        NothingSlider(value, onValueChange, range = 0..255, modifier = Modifier.weight(1f))
        StepButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Increase $label", enabled = value < 255) {
            onStep(1)
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
