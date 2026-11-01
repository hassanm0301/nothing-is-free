package io.github.hassanm0301.nothingisfree.ui.glyph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.hassanm0301.nothingisfree.glyph.toy.BinaryClockSettings
import io.github.hassanm0301.nothingisfree.glyph.toy.BitStyle
import io.github.hassanm0301.nothingisfree.ui.components.DotDivider
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.components.NothingListRow
import io.github.hassanm0301.nothingisfree.ui.components.NothingSwitch
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

        PercentControl(
            "Active brightness",
            settings.activeBrightness,
            onValueChange = { v -> onChange { it.copy(activeBrightness = v) } },
            onStep = { d -> onChange { it.copy(activeBrightness = (it.activeBrightness + d).coerceIn(0, 100)) } },
        )
        Spacer(Modifier.height(Spacing.md))
        PercentControl(
            "Inactive brightness",
            settings.inactiveBrightness,
            onValueChange = { v -> onChange { it.copy(inactiveBrightness = v) } },
            onStep = { d -> onChange { it.copy(inactiveBrightness = (it.inactiveBrightness + d).coerceIn(0, 100)) } },
        )
    }
}
