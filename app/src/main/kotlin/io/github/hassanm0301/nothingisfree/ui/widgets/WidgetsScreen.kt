package io.github.hassanm0301.nothingisfree.ui.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.ui.AppViewModel
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.screenHeader
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/** The pages the Widgets tab opens from its list. */
private enum class WidgetPage { PRECISE_VOLUME, CLOCK }

/** Widgets tab: Precise volume and the home-screen clock, each opening its own page. */
@Composable
fun WidgetsScreen(viewModel: AppViewModel, insets: PaddingValues) {
    var page by rememberSaveable { mutableStateOf<WidgetPage?>(null) }
    BackHandler(enabled = page != null) { page = null }
    // Keeps the list's scroll position while a page is open.
    val pageState = rememberSaveableStateHolder()

    pageState.SaveableStateProvider(page?.name ?: "list") {
        when (page) {
            WidgetPage.PRECISE_VOLUME -> PreciseVolumeScreen(viewModel, insets, onBack = { page = null })
            WidgetPage.CLOCK -> ClockWidgetScreen(viewModel, insets, onBack = { page = null })
            null -> WidgetsList(viewModel, insets, onOpen = { page = it })
        }
    }
}

@Composable
private fun WidgetsList(viewModel: AppViewModel, insets: PaddingValues, onOpen: (WidgetPage) -> Unit) {
    val format by viewModel.volumeFormat.collectAsStateWithLifecycle()
    val font by viewModel.clockWidgetFont.collectAsStateWithLifecycle()
    val volumeOn by rememberPreciseVolumeOn()

    NothingScreen(insets) {
        screenHeader("Volume · home screen", "Widgets")
        item {
            SectionHeader("Volume")
            WidgetCard(
                title = "Precise volume",
                summary = "The volume level as text, beside the volume bar",
                status = if (volumeOn) "On · ${format.title}" else "Off",
                onClick = { onOpen(WidgetPage.PRECISE_VOLUME) },
            )
        }
        item {
            SectionHeader("Home screen")
            WidgetCard(
                title = "Clock",
                summary = "The time, the day and date, and your next alarm",
                status = font.title,
                onClick = { onOpen(WidgetPage.CLOCK) },
            )
        }
    }
}

/** A row of the list: opens a widget's page, with its state in the accent colour. */
@Composable
private fun WidgetCard(title: String, summary: String, status: String, onClick: () -> Unit) {
    NothingCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Spacing.xs))
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = NothingTheme.colors.muted)
                Spacer(Modifier.height(Spacing.xs))
                MonoLabel(status, style = MaterialTheme.typography.labelSmall, color = NothingTheme.colors.accent)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = NothingTheme.colors.muted,
            )
        }
    }
}
