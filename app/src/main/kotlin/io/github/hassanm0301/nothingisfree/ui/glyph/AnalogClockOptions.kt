package io.github.hassanm0301.nothingisfree.ui.glyph

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.hassanm0301.nothingisfree.glyph.toy.AnalogClockSettings
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/** The Analog clock toy's options. Changes reach the phone's matrix straight away. */
@Composable
fun AnalogClockOptions(
    settings: AnalogClockSettings,
    onChange: ((AnalogClockSettings) -> AnalogClockSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    NothingCard(modifier) {
        SliderControl(
            "Hour hand",
            settings.hourBrightness,
            AnalogClockSettings.BRIGHTNESS_RANGE,
            { "$it%" },
            onValueChange = { v -> onChange { it.copy(hourBrightness = v) } },
            // Kept within range by AnalogClockPreferences.
            onStep = { d -> onChange { it.copy(hourBrightness = it.hourBrightness + d) } },
        )
        Spacer(Modifier.height(Spacing.md))
        SliderControl(
            "Minute hand",
            settings.minuteBrightness,
            AnalogClockSettings.BRIGHTNESS_RANGE,
            { "$it%" },
            onValueChange = { v -> onChange { it.copy(minuteBrightness = v) } },
            onStep = { d -> onChange { it.copy(minuteBrightness = it.minuteBrightness + d) } },
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "The brightness of each hand. Set them apart to tell the hands apart by more than their " +
                "length; where they overlap, the brighter one shows. Addons keep their own brightness.",
            style = MaterialTheme.typography.bodyMedium,
            color = NothingTheme.colors.muted,
        )
    }
}
