package io.github.hassanm0301.nothingisfree.ui.glyph

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
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonId
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyId
import io.github.hassanm0301.nothingisfree.ui.AppViewModel
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.StatusDot
import io.github.hassanm0301.nothingisfree.ui.components.detailHeader
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/**
 * One toy's settings: what it shows right now (with the addons that are on), a Preview button,
 * and its own options. Addons are switched and set up under Glyph › Addons, not here.
 */
@Composable
fun ToyDetailScreen(
    toy: ToyId,
    viewModel: AppViewModel,
    insets: PaddingValues,
    onBack: () -> Unit,
) {
    val addons by viewModel.addons.collectAsStateWithLifecycle()
    val digitalClock by viewModel.digitalClock.collectAsStateWithLifecycle()
    val binaryClock by viewModel.binaryClock.collectAsStateWithLifecycle()
    val compactClock by viewModel.compactClock.collectAsStateWithLifecycle()
    val lastAlertAt by viewModel.lastAlertAt.collectAsStateWithLifecycle()
    val pulseSettings by viewModel.pulse.collectAsStateWithLifecycle()
    val notificationsShowing by viewModel.notificationsShowing.collectAsStateWithLifecycle()
    val now by rememberNow()
    var previewing by rememberSaveable { mutableStateOf(false) }

    val face = remember(toy, digitalClock, binaryClock, compactClock) {
        toy.face(PREVIEW_SIZE, digitalClock, binaryClock, compactClock)
    }
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
                ToyId.COMPACT_CLOCK -> CompactClockOptions(compactClock, onChange = viewModel::updateCompactClock)
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
