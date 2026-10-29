package io.github.hassanm0301.nothingisfree.ui.settings

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.core.device.NothingDevice
import io.github.hassanm0301.nothingisfree.core.update.AppUpdater
import io.github.hassanm0301.nothingisfree.core.update.GitHubReleases
import io.github.hassanm0301.nothingisfree.glyph.GlyphCapability
import io.github.hassanm0301.nothingisfree.ui.AppViewModel
import io.github.hassanm0301.nothingisfree.ui.components.DotDivider
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.components.NothingListRow
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.screenHeader
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing
import io.github.hassanm0301.nothingisfree.ui.theme.ThemeMode
import io.github.hassanm0301.nothingisfree.ui.update.UpdateCard

/** Settings tab: updates, appearance, this phone, and about. */
@Composable
fun SettingsScreen(viewModel: AppViewModel, insets: PaddingValues) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val update by viewModel.update.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember { AppUpdater.currentVersion(context) }

    NothingScreen(insets) {
        screenHeader("Nothing is Free · v$versionName", "Settings", live = update is AppUpdater.State.Available)
        item {
            SectionHeader("Updates")
            UpdateCard(
                state = update,
                currentVersion = versionName,
                onCheck = viewModel::checkForUpdate,
                onInstall = viewModel::installUpdate,
            )
        }
        item {
            SectionHeader("Theme")
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ThemeMode.entries.forEach { m ->
                    NothingChip(text = m.name, selected = m == themeMode, onClick = { viewModel.setThemeMode(m) })
                }
            }
        }
        item {
            SectionHeader("Device")
            DeviceCard()
        }
        item {
            SectionHeader("About")
            NothingListRow(
                "Source",
                subtitle = "github.com/${GitHubReleases.REPO}",
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/${GitHubReleases.REPO}".toUri()))
                },
                trailing = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open on GitHub",
                        tint = NothingTheme.colors.muted,
                    )
                },
            )
            DotDivider()
            NothingListRow("Fonts", subtitle = "Ndot · NType 82 · Lettera Mono, via xeji01/nothingfont")
        }
    }
}

@Composable
private fun DeviceCard() {
    val model = NothingDevice.detectedModel
    val glyph = GlyphCapability.of(model)
    NothingCard(Modifier.fillMaxWidth()) {
        KeyValue("Manufacturer", NothingDevice.manufacturer)
        KeyValue("Model", NothingDevice.model)
        KeyValue("Device", NothingDevice.device)
        KeyValue("Android", "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}")
        KeyValue("Nothing", if (NothingDevice.isNothing) model.marketingName else "No")
        KeyValue(
            "Glyph",
            when (glyph) {
                GlyphCapability.None -> "None"
                is GlyphCapability.Strips -> "Strips · ${glyph.deviceId}"
                is GlyphCapability.Matrix -> "Matrix ${glyph.width}×${glyph.width} · ${glyph.deviceId}" +
                    (if (glyph.aodOnly) " · AOD only" else "")
            },
        )
    }
}

@Composable
private fun KeyValue(key: String, value: String) {
    Row(Modifier.padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        MonoLabel(key, Modifier.width(120.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
