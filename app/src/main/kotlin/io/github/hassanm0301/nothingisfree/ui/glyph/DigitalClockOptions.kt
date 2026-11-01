package io.github.hassanm0301.nothingisfree.ui.glyph

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.hassanm0301.nothingisfree.glyph.toy.DigitalClockSettings
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/** The Digital clock toy's options. Changes reach the phone's matrix straight away. */
@Composable
fun DigitalClockOptions(
    settings: DigitalClockSettings,
    onChange: ((DigitalClockSettings) -> DigitalClockSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    NothingCard(modifier) {
        SliderControl(
            "Brightness",
            settings.brightness,
            DigitalClockSettings.BRIGHTNESS_RANGE,
            { "$it%" },
            onValueChange = { v -> onChange { it.copy(brightness = v) } },
            // Kept within range by DigitalClockPreferences.
            onStep = { d -> onChange { it.copy(brightness = it.brightness + d) } },
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "The digits only. Addons keep their own brightness.",
            style = MaterialTheme.typography.bodyMedium,
            color = NothingTheme.colors.muted,
        )
    }
}
