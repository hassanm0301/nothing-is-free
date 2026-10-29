package io.github.hassanm0301.nothingisfree.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hassanm0301.nothingisfree.core.update.AppUpdater
import io.github.hassanm0301.nothingisfree.ui.components.NothingTab
import io.github.hassanm0301.nothingisfree.ui.components.NothingTabBar
import io.github.hassanm0301.nothingisfree.ui.components.TabIcons
import io.github.hassanm0301.nothingisfree.ui.glyph.GlyphScreen
import io.github.hassanm0301.nothingisfree.ui.settings.SettingsScreen
import io.github.hassanm0301.nothingisfree.ui.widgets.WidgetsScreen

private enum class AppTab(val tab: NothingTab) {
    GLYPH(NothingTab("Glyph", TabIcons.Glyph)),
    WIDGETS(NothingTab("Widgets", TabIcons.Widgets)),
    SETTINGS(NothingTab("Settings", TabIcons.Settings)),
}

/** An update is waiting, downloading, installing, or failed part-way: worth a dot on Settings. */
private val AppUpdater.State.needsAttention: Boolean
    get() = this is AppUpdater.State.Available || this is AppUpdater.State.Downloading ||
        this is AppUpdater.State.Installing || (this is AppUpdater.State.Failed && release != null)

/** The app: three tabs over a Nothing tab bar. Back from another tab returns to Glyph. */
@Composable
fun AppScreen(viewModel: AppViewModel = viewModel()) {
    var current by rememberSaveable { mutableStateOf(AppTab.GLYPH) }
    BackHandler(enabled = current != AppTab.GLYPH) { current = AppTab.GLYPH }
    // Keeps each tab's scroll position while another one is showing.
    val tabState = rememberSaveableStateHolder()
    val update by viewModel.update.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NothingTabBar(
                tabs = AppTab.entries.map { it.tab },
                selected = current.ordinal,
                onSelect = { current = AppTab.entries[it] },
                badged = { it == AppTab.SETTINGS.ordinal && update.needsAttention },
            )
        },
    ) { insets ->
        tabState.SaveableStateProvider(current.name) {
            when (current) {
                AppTab.GLYPH -> GlyphScreen(viewModel, insets)
                AppTab.WIDGETS -> WidgetsScreen(viewModel, insets)
                AppTab.SETTINGS -> SettingsScreen(viewModel, insets)
            }
        }
        // Content scrolls edge to edge; keep the status bar on a solid background.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.background),
        )
    }
}
