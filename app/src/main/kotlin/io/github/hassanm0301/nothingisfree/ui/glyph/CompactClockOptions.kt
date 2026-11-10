package io.github.hassanm0301.nothingisfree.ui.glyph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.hassanm0301.nothingisfree.glyph.toy.CompactClockFont
import io.github.hassanm0301.nothingisfree.glyph.toy.CompactClockSettings
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/** The Compact digital clock toy's options. Changes reach the phone's matrix straight away. */
@Composable
fun CompactClockOptions(
    settings: CompactClockSettings,
    onChange: ((CompactClockSettings) -> CompactClockSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    NothingCard(modifier) {
        MonoLabel("Font")
        Spacer(Modifier.height(Spacing.sm))
        // Wraps, so more fonts can be added to CompactClockFont without changing this screen.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            CompactClockFont.entries.forEach { font ->
                NothingChip(font.label, settings.font == font, onClick = { onChange { it.copy(font = font) } })
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            settings.font.description,
            style = MaterialTheme.typography.bodyMedium,
            color = NothingTheme.colors.muted,
        )
    }
}
