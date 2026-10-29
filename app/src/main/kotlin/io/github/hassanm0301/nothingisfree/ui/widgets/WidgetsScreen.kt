package io.github.hassanm0301.nothingisfree.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.ui.AppViewModel
import io.github.hassanm0301.nothingisfree.ui.components.DotBar
import io.github.hassanm0301.nothingisfree.ui.components.DotMatrixClock
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingProgressRing
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.PlaceholderTile
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.screenHeader
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE dd MMM")

/**
 * Widgets tab: the surfaces home-screen widgets will use. Previews only until the Glance widgets
 * land (docs/WIDGETS.md); battery and steps show sample values.
 */
@Composable
fun WidgetsScreen(viewModel: AppViewModel, insets: PaddingValues) {
    val now by viewModel.now.collectAsStateWithLifecycle()

    NothingScreen(insets) {
        screenHeader(
            "Home screen · coming soon",
            "Widgets",
            "Previews of the widgets on the way. They can't be placed on the home screen yet.",
        )
        item {
            SectionHeader("Clock")
            ClockWidget(now)
        }
        item {
            SectionHeader("Status")
            StatusWidgets()
            Spacer(Modifier.height(Spacing.sm))
            PlaceholderTile("Widget slot · coming soon", Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ClockWidget(now: LocalDateTime) {
    NothingCard(inverted = true) {
        // Muted tone for text inside the inverted card.
        val muted = LocalContentColor.current.copy(alpha = 0.55f)
        MonoLabel(now.format(DateFmt), color = muted)
        Spacer(Modifier.height(Spacing.md))
        // The theme's dot colours are for the page background; follow the inverted card instead.
        DotMatrixClock(
            now.toLocalTime(),
            Modifier.fillMaxWidth(),
            onColor = LocalContentColor.current,
            offColor = LocalContentColor.current.copy(alpha = 0.12f),
        )
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.weight(1f))
            MonoLabel("Dot matrix", color = muted)
        }
    }
}

@Composable
private fun StatusWidgets() {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        NothingCard(Modifier.weight(1f)) {
            MonoLabel("Battery")
            Spacer(Modifier.height(Spacing.md))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                NothingProgressRing(progress = 0.84f, size = 104.dp) {
                    Text("84", style = MaterialTheme.typography.headlineLarge)
                }
            }
            Spacer(Modifier.height(Spacing.md))
            MonoLabel("Charging", color = NothingTheme.colors.accent)
        }
        NothingCard(Modifier.weight(1f), outlined = true) {
            MonoLabel("Steps")
            Spacer(Modifier.height(Spacing.md))
            Text("6 412", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(Spacing.md))
            DotBar(value = 6412, max = 10_000)
            Spacer(Modifier.height(Spacing.sm))
            MonoLabel("of 10 000", style = MaterialTheme.typography.labelSmall)
        }
    }
}
