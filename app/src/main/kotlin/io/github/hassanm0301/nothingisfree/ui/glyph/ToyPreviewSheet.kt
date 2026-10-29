package io.github.hassanm0301.nothingisfree.ui.glyph

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonId
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyId
import io.github.hassanm0301.nothingisfree.ui.AppViewModel
import io.github.hassanm0301.nothingisfree.ui.components.ButtonVariant
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.components.NothingListRow
import io.github.hassanm0301.nothingisfree.ui.components.NothingSlider
import io.github.hassanm0301.nothingisfree.ui.components.NothingSwitch
import io.github.hassanm0301.nothingisfree.ui.components.StatusDot
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing
import java.time.LocalTime

private const val MINUTES_PER_DAY = 24 * 60

/**
 * Try a toy out: the matrix large, at any time of day picked on a slider, and a simulated
 * notification that runs the pulse here only, not on the phone. Uses the toy's real settings and
 * addons, drawn through the same `frame()` as the toy service.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToyPreviewSheet(toy: ToyId, viewModel: AppViewModel, onDismiss: () -> Unit) {
    val addons by viewModel.addons.collectAsStateWithLifecycle()
    val binaryClock by viewModel.binaryClock.collectAsStateWithLifecycle()
    val pulseWindowSeconds by viewModel.pulseWindowSeconds.collectAsStateWithLifecycle()
    val now by rememberNow()

    // null follows the clock; otherwise the minute of the day picked on the slider.
    var pickedMinute by rememberSaveable { mutableStateOf<Int?>(null) }
    var simulatedAt by rememberSaveable { mutableStateOf<Long?>(null) }

    val time = pickedMinute?.let { LocalTime.of(it / 60, it % 60) } ?: now
    val face = remember(toy, binaryClock) { toy.face(PREVIEW_SIZE, binaryClock) }
    val pulseOn = toy in addons[AddonId.NOTIFICATION_PULSE].orEmpty()
    val pulse by rememberPulse(simulatedAt, pulseWindowSeconds, pulseOn)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen)
                .navigationBarsPadding()
                .padding(bottom = Spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonoLabel("Preview · ${toy.title}", Modifier.weight(1f))
                if (pulse.pulsing) {
                    StatusDot()
                    Spacer(Modifier.width(Spacing.sm))
                    MonoLabel("Pulsing · ${(pulse.remainingMs + 999) / 1_000} s", color = NothingTheme.colors.accent)
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ToyMatrix(face, time, pulse.level, Modifier.width(240.dp))
            }
            Spacer(Modifier.height(Spacing.lg))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(time), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                NothingChip("Now", selected = pickedMinute == null, onClick = { pickedMinute = null })
            }
            NothingSlider(
                value = pickedMinute ?: (time.hour * 60 + time.minute),
                onValueChange = { pickedMinute = it },
                range = 0 until MINUTES_PER_DAY,
            )
            Row {
                MonoLabel(formatTime(LocalTime.MIDNIGHT), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                MonoLabel(formatTime(LocalTime.of(23, 59)), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(Spacing.lg))

            if (pulseOn) {
                NothingButton(
                    if (pulse.pulsing) "Stop" else "Simulate notification",
                    onClick = { simulatedAt = if (pulse.pulsing) null else System.currentTimeMillis() },
                    variant = if (pulse.pulsing) ButtonVariant.Secondary else ButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Runs the ${AddonId.NOTIFICATION_PULSE.title.lowercase()} here for its set window. " +
                        "Nothing is sent to the phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NothingTheme.colors.muted,
                )
            } else {
                NothingListRow(
                    AddonId.NOTIFICATION_PULSE.title,
                    subtitle = "Off for this toy",
                    trailing = {
                        NothingSwitch(false, { viewModel.setAddon(AddonId.NOTIFICATION_PULSE, toy, it) })
                    },
                )
                Text(
                    "Switch it on to simulate a notification.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NothingTheme.colors.muted,
                )
            }
        }
    }
}
