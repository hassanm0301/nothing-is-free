package io.github.hassanm0301.nothingisfree.ui.gallery

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hassanm0301.nothingisfree.core.device.NothingDevice
import io.github.hassanm0301.nothingisfree.glyph.GlyphCapability
import io.github.hassanm0301.nothingisfree.glyph.toy.ClockToyPreferences
import io.github.hassanm0301.nothingisfree.ui.components.ButtonVariant
import io.github.hassanm0301.nothingisfree.ui.components.DotBar
import io.github.hassanm0301.nothingisfree.ui.components.DotDivider
import io.github.hassanm0301.nothingisfree.ui.components.DotMatrixClock
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.components.NothingIconButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingListRow
import io.github.hassanm0301.nothingisfree.ui.components.NothingProgressRing
import io.github.hassanm0301.nothingisfree.ui.components.NothingSwitch
import io.github.hassanm0301.nothingisfree.ui.components.PlaceholderTile
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.StatusDot
import io.github.hassanm0301.nothingisfree.ui.glyph.PulseClockCard
import io.github.hassanm0301.nothingisfree.ui.theme.Grey100
import io.github.hassanm0301.nothingisfree.ui.theme.Grey400
import io.github.hassanm0301.nothingisfree.ui.theme.Grey700
import io.github.hassanm0301.nothingisfree.ui.theme.Grey900
import io.github.hassanm0301.nothingisfree.ui.theme.NothingBlack
import io.github.hassanm0301.nothingisfree.ui.theme.NothingRed
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.NothingWhite
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing
import io.github.hassanm0301.nothingisfree.ui.theme.ThemeMode
import java.time.format.DateTimeFormatter

private val DateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE dd MMM")

@Composable
fun GalleryScreen(viewModel: GalleryViewModel = viewModel()) {
    val now by viewModel.now.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val lastAlertAt by viewModel.lastAlertAt.collectAsStateWithLifecycle()
    val pulseWindowSeconds by viewModel.pulseWindowSeconds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "dev"
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.screen,
                end = Spacing.screen,
                top = inner.calculateTopPadding() + Spacing.lg,
                bottom = inner.calculateBottomPadding() + Spacing.xxl,
            ),
        ) {
            header(versionName)
            clockCard(now)
            glyphToySection(lastAlertAt, pulseWindowSeconds, viewModel::setPulseWindowSeconds, viewModel::testPulse)
            themeSection(themeMode, viewModel::setThemeMode)
            typographySection()
            colourSection()
            buttonSection()
            controlSection()
            widgetCards()
            listSection()
            deviceSection()
            footer()
        }
    }
}

private fun LazyListScope.header(versionName: String) = item {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot()
        Spacer(Modifier.width(Spacing.sm))
        MonoLabel("Component gallery  ·  v$versionName")
    }
    Spacer(Modifier.height(Spacing.md))
    Text("Nothing", style = MaterialTheme.typography.displayLarge)
    Text("is free", style = MaterialTheme.typography.displayLarge, color = NothingTheme.colors.muted)
    Spacer(Modifier.height(Spacing.sm))
    Text(
        "A Nothing OS styled base. Widgets and Glyph come next.",
        style = MaterialTheme.typography.bodyMedium,
        color = NothingTheme.colors.muted,
    )
}

private fun LazyListScope.clockCard(now: java.time.LocalDateTime) = item {
    Spacer(Modifier.height(Spacing.lg))
    NothingCard(inverted = true) {
        MonoLabel(now.format(DateFmt), color = contentMuted())
        Spacer(Modifier.height(Spacing.md))
        DotMatrixClock(now.toLocalTime(), Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.weight(1f))
            MonoLabel("Dot matrix", color = contentMuted())
        }
    }
}

private fun LazyListScope.glyphToySection(
    lastAlertAt: Long?,
    pulseWindowSeconds: Int,
    onPulseWindowChange: (Int) -> Unit,
    onTestPulse: () -> Unit,
) = item {
    SectionHeader("Glyph toy")
    PulseClockCard(
        lastAlertAt = lastAlertAt,
        pulseWindowSeconds = pulseWindowSeconds,
        pulseWindowChoices = ClockToyPreferences.pulseWindowChoices,
        onPulseWindowChange = onPulseWindowChange,
        onTestPulse = onTestPulse,
    )
}

/** Muted tone for text inside an inverted card. */
@Composable
private fun contentMuted(): Color = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.55f)

private fun LazyListScope.themeSection(mode: ThemeMode, onSelect: (ThemeMode) -> Unit) = item {
    SectionHeader("Theme")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ThemeMode.entries.forEach { m ->
            NothingChip(text = m.name, selected = m == mode, onClick = { onSelect(m) })
        }
    }
}

private fun LazyListScope.typographySection() = item {
    SectionHeader("Typography")
    Text("Ndot 57", style = MaterialTheme.typography.displayMedium)
    Text("Headline 32", style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(Spacing.sm))
    Text("NType 82 Headline, title large", style = MaterialTheme.typography.titleLarge)
    Text("NType 82 Regular, body large. The quick brown fox jumps over the lazy dog.", style = MaterialTheme.typography.bodyLarge)
    Text("Body medium, secondary copy.", style = MaterialTheme.typography.bodyMedium, color = NothingTheme.colors.muted)
    Spacer(Modifier.height(Spacing.sm))
    MonoLabel("Lettera Mono · label large", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
    MonoLabel("Lettera Mono · label medium")
    MonoLabel("Lettera Mono · label small", style = MaterialTheme.typography.labelSmall)
}

private fun LazyListScope.colourSection() = item {
    SectionHeader("Colour")
    val swatches = listOf(
        "Black" to NothingBlack, "White" to NothingWhite, "Red" to NothingRed,
        "900" to Grey900, "700" to Grey700, "400" to Grey400, "100" to Grey100,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        swatches.forEach { (name, color) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier
                        .size(36.dp)
                        .background(color, CircleShape)
                        .background(Color.Transparent),
                )
                Spacer(Modifier.height(Spacing.xs))
                MonoLabel(name, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun LazyListScope.buttonSection() = item {
    SectionHeader("Buttons")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        NothingButton("Primary", onClick = {})
        NothingButton("Secondary", onClick = {}, variant = ButtonVariant.Secondary)
    }
    Spacer(Modifier.height(Spacing.sm))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        NothingButton("Accent", onClick = {}, variant = ButtonVariant.Accent)
        NothingButton("Ghost", onClick = {}, variant = ButtonVariant.Ghost)
        NothingButton("Off", onClick = {}, enabled = false)
    }
    Spacer(Modifier.height(Spacing.md))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        NothingIconButton(Icons.Filled.Add, "Add", onClick = {})
        NothingIconButton(Icons.Filled.Settings, "Settings", onClick = {})
        NothingIconButton(Icons.Filled.Favorite, "Favourite", onClick = {}, accent = true)
    }
}

private fun LazyListScope.controlSection() = item {
    SectionHeader("Controls")
    var on by remember { mutableStateOf(true) }
    var off by remember { mutableStateOf(false) }
    var chip by remember { mutableIntStateOf(0) }
    NothingListRow("Glyph notifications", subtitle = "Lights when a message arrives", trailing = {
        NothingSwitch(on, { on = it })
    })
    DotDivider()
    NothingListRow("Always-on matrix", subtitle = "Phone (4a) Pro only", trailing = {
        NothingSwitch(off, { off = it })
    })
    Spacer(Modifier.height(Spacing.md))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf("Day", "Week", "Month").forEachIndexed { i, label ->
            NothingChip(label, selected = chip == i, onClick = { chip = i })
        }
    }
}

private fun LazyListScope.widgetCards() = item {
    SectionHeader("Widget surfaces")
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
    Spacer(Modifier.height(Spacing.sm))
    PlaceholderTile("Widget slot · coming soon", Modifier.fillMaxWidth())
}

private fun LazyListScope.listSection() = item {
    SectionHeader("List")
}

private val listEntries = listOf(
    "Appearance" to "Theme, fonts, density",
    "Widgets" to "Not yet available",
    "Glyph" to "Not yet available",
    "About" to "Version, licences",
)

private fun LazyListScope.deviceSection() {
    items(listEntries) { (title, subtitle) ->
        NothingListRow(title, subtitle = subtitle, trailing = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = NothingTheme.colors.muted)
        })
        DotDivider()
    }
    item {
        SectionHeader("Device")
        val model = NothingDevice.detectedModel
        val glyph = GlyphCapability.of(model)
        NothingCard {
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
}

@Composable
private fun KeyValue(key: String, value: String) {
    Row(Modifier.padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        MonoLabel(key, Modifier.width(120.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun LazyListScope.footer() = item {
    Spacer(Modifier.height(Spacing.xl))
    DotDivider()
    Spacer(Modifier.height(Spacing.md))
    MonoLabel("Widgets · Glyph · Soon", style = MaterialTheme.typography.labelSmall)
}
