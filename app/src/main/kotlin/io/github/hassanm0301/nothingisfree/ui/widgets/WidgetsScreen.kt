package io.github.hassanm0301.nothingisfree.ui.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
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
 * Widgets tab: Precise volume, which opens its own page, and the surfaces home-screen widgets will
 * use. Those are previews only until the Glance widgets land (docs/WIDGETS.md); battery and steps
 * show sample values.
 */
@Composable
fun WidgetsScreen(viewModel: AppViewModel, insets: PaddingValues) {
    var volumeOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = volumeOpen) { volumeOpen = false }
    // Keeps the list's scroll position while the page is open.
    val pageState = rememberSaveableStateHolder()

    pageState.SaveableStateProvider(if (volumeOpen) "precise_volume" else "list") {
        if (volumeOpen) {
            PreciseVolumeScreen(viewModel, insets, onBack = { volumeOpen = false })
        } else {
            WidgetsList(viewModel, insets, onOpenPreciseVolume = { volumeOpen = true })
        }
    }
}

@Composable
private fun WidgetsList(viewModel: AppViewModel, insets: PaddingValues, onOpenPreciseVolume: () -> Unit) {
    val now by viewModel.now.collectAsStateWithLifecycle()

    NothingScreen(insets) {
        screenHeader(
            "Volume · home screen",
            "Widgets",
            "Precise volume works now. The home-screen widgets below are previews and can't be placed yet.",
        )
        item {
            SectionHeader("Volume")
            PreciseVolumeCard(viewModel, onClick = onOpenPreciseVolume)
        }
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

/** Opens Precise volume's page; says whether it is on and how it writes the level. */
@Composable
private fun PreciseVolumeCard(viewModel: AppViewModel, onClick: () -> Unit) {
    val format by viewModel.volumeFormat.collectAsStateWithLifecycle()
    val on by rememberPreciseVolumeOn()

    NothingCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Precise volume", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "The volume level as text, beside the volume bar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NothingTheme.colors.muted,
                )
                Spacer(Modifier.height(Spacing.xs))
                MonoLabel(
                    if (on) "On · ${format.title}" else "Off",
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingTheme.colors.accent,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = NothingTheme.colors.muted,
            )
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
