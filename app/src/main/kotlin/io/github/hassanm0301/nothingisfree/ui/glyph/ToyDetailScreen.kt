package io.github.hassanm0301.nothingisfree.ui.glyph

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.core.device.NothingDevice
import io.github.hassanm0301.nothingisfree.glyph.GlyphCapability
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonCategory
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonId
import io.github.hassanm0301.nothingisfree.glyph.addon.BinaryBattery
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyId
import io.github.hassanm0301.nothingisfree.ui.AppViewModel
import io.github.hassanm0301.nothingisfree.ui.components.DotDivider
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingListRow
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.StatusDot
import io.github.hassanm0301.nothingisfree.ui.components.detailHeader
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/**
 * One toy's settings: what it shows right now, a Preview button, its own options, and the addons
 * it shows with them. Addons are switched for every toy at once, on their own pages, not here.
 */
@Composable
fun ToyDetailScreen(
    toy: ToyId,
    viewModel: AppViewModel,
    insets: PaddingValues,
    onBack: () -> Unit,
    onOpenAddon: (AddonId) -> Unit,
) {
    val addons by viewModel.addons.collectAsStateWithLifecycle()
    val digitalClock by viewModel.digitalClock.collectAsStateWithLifecycle()
    val binaryClock by viewModel.binaryClock.collectAsStateWithLifecycle()
    val lastAlertAt by viewModel.lastAlertAt.collectAsStateWithLifecycle()
    val pulseSettings by viewModel.pulse.collectAsStateWithLifecycle()
    val notificationsShowing by viewModel.notificationsShowing.collectAsStateWithLifecycle()
    val binaryBattery by viewModel.binaryBattery.collectAsStateWithLifecycle()
    val now by rememberNow()
    var previewing by rememberSaveable { mutableStateOf(false) }

    val face = remember(toy, digitalClock, binaryClock) { toy.face(PREVIEW_SIZE, digitalClock, binaryClock) }
    val pulseOn = AddonId.NOTIFICATION_PULSE in addons
    val pulse by rememberPulse(lastAlertAt, pulseSettings, pulseOn, notificationsShowing)
    val battery = batteryGauge(viewModel)

    NothingScreen(insets) {
        detailHeader("Glyph", onBack, kicker = "Glyph toy", title = toy.title, body = toy.summary)
        item {
            SectionHeader("Now")
            NothingCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonoLabel("On the matrix · $PREVIEW_SIZE×$PREVIEW_SIZE", Modifier.weight(1f))
                    if (pulse.pulsing) {
                        StatusDot()
                        Spacer(Modifier.width(Spacing.sm))
                        MonoLabel("Pulsing", color = NothingTheme.colors.accent)
                    } else {
                        MonoLabel(formatTime(now), color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ToyMatrix(face, now, pulse.level, Modifier.width(168.dp), battery)
                }
                Spacer(Modifier.height(Spacing.lg))
                NothingButton("Preview", onClick = { previewing = true }, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            SectionHeader("Options")
            when (toy) {
                ToyId.DIGITAL_CLOCK -> DigitalClockOptions(digitalClock, onChange = viewModel::updateDigitalClock)
                ToyId.BINARY_CLOCK -> BinaryClockOptions(binaryClock, onChange = viewModel::updateBinaryClock)
                ToyId.COMPACT_CLOCK -> Text(
                    "No options: it always shows the hour's first digit and the minute's second.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NothingTheme.colors.muted,
                )
            }
        }
        item {
            SectionHeader("Addons")
            Text(
                "Addons are switched on or off for every toy at once, one per category: in Glyph › " +
                    "Addons, or on an addon's own page. Tap one to open it.",
                style = MaterialTheme.typography.bodyMedium,
                color = NothingTheme.colors.muted,
            )
            // Where the binary battery really goes on this toy, if its edge has no room here.
            val binaryEdge = BinaryBattery.placement(face, binaryBattery)
            AddonCategory.entries.forEach { category ->
                Spacer(Modifier.height(Spacing.lg))
                MonoLabel(category.title)
                AddonId.entries.filter { it.category == category }.forEachIndexed { i, addon ->
                    if (i > 0) DotDivider()
                    val moved = addon == AddonId.BINARY_BATTERY && binaryEdge != null && binaryEdge != binaryBattery.edge
                    NothingListRow(
                        addon.title,
                        subtitle = if (moved) {
                            "Settings › · ${binaryEdge?.label} here, no room at the ${binaryBattery.edge.label.lowercase()}"
                        } else {
                            "Settings ›"
                        },
                        modifier = Modifier.clickable { onOpenAddon(addon) },
                        trailing = {
                            val on = addon in addons
                            MonoLabel(
                                if (on) "On" else "Off",
                                color = if (on) NothingTheme.colors.accent else NothingTheme.colors.muted,
                            )
                        },
                    )
                }
            }
        }
        item {
            SectionHeader("On the phone")
            Text(
                if (GlyphCapability.of(NothingDevice.detectedModel) is GlyphCapability.Matrix) {
                    "Settings › Glyph Interface › Flip to Glyph › Always-on Glyph Toy › ${toy.title}. " +
                        "Changes here show on the matrix straight away."
                } else {
                    "This phone has no Glyph Matrix. On a Phone (4a) Pro, pick it in Settings › Glyph " +
                        "Interface › Flip to Glyph › Always-on Glyph Toy › ${toy.title}."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = NothingTheme.colors.muted,
            )
        }
    }

    if (previewing) {
        ToyPreviewSheet(toy, viewModel, onDismiss = { previewing = false })
    }
}
