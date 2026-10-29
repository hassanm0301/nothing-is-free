package io.github.hassanm0301.nothingisfree.ui.glyph

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.hassanm0301.nothingisfree.core.notifications.GlyphNotificationListener
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrixCanvas
import io.github.hassanm0301.nothingisfree.glyph.addon.PulseWave
import io.github.hassanm0301.nothingisfree.glyph.addon.frame
import io.github.hassanm0301.nothingisfree.glyph.toy.ClockFace
import io.github.hassanm0301.nothingisfree.ui.components.ButtonVariant
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.components.StatusDot
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing
import kotlinx.coroutines.delay
import java.time.LocalTime

/**
 * Settings and a live preview for the "Pulse clock" Glyph Toy. The preview draws the exact frame
 * the toy sends to a 13x13 matrix, pulse included.
 */
@Composable
fun PulseClockCard(
    lastAlertAt: Long?,
    pulseWindowSeconds: Int,
    pulseWindowChoices: List<Int>,
    onPulseWindowChange: (Int) -> Unit,
    onTestPulse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val face = remember { ClockFace(13) }
    val is24Hour = DateFormat.is24HourFormat(context)
    val windowMs = pulseWindowSeconds * 1_000L

    val initial = Preview(face.render(LocalTime.now(), is24Hour), pulsing = false)
    val preview by produceState(initial, lastAlertAt, windowMs, is24Hour) {
        while (true) {
            val now = System.currentTimeMillis()
            val elapsed = PulseWave.elapsed(lastAlertAt, windowMs, now)
            val ring = elapsed?.let(PulseWave::level) ?: 0
            value = Preview(face.frame(LocalTime.now(), is24Hour, ring), pulsing = elapsed != null)
            if (elapsed != null) withFrameMillis { } else delay(1_000L - now % 1_000L)
        }
    }

    var hasAccess by remember { mutableStateOf(GlyphNotificationListener.hasAccess(context)) }
    LifecycleResumeEffect(Unit) {
        hasAccess = GlyphNotificationListener.hasAccess(context)
        onPauseOrDispose { }
    }

    NothingCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoLabel("Pulse clock · 13×13", Modifier.weight(1f))
            if (preview.pulsing) {
                StatusDot()
                Spacer(Modifier.width(Spacing.sm))
                MonoLabel("Pulsing", color = NothingTheme.colors.accent)
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DotMatrixCanvas(preview.frame, Modifier.width(168.dp), isDot = face::inCircle)
        }
        Spacer(Modifier.height(Spacing.lg))

        MonoLabel("Pulse after a notification for")
        Spacer(Modifier.height(Spacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            pulseWindowChoices.forEach { seconds ->
                NothingChip(
                    text = if (seconds < 60) "${seconds}s" else "${seconds / 60}m",
                    selected = seconds == pulseWindowSeconds,
                    onClick = { onPulseWindowChange(seconds) },
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        Text(
            if (hasAccess) {
                "Notification access is on. Pick the toy in Settings › Glyph Interface › " +
                    "Flip to Glyph › Always-on Glyph Toy."
            } else {
                "Needs notification access to know when something arrives."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = NothingTheme.colors.muted,
        )
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            NothingButton(
                if (hasAccess) "Manage access" else "Grant access",
                onClick = { context.startActivity(GlyphNotificationListener.accessSettingsIntent(context)) },
                variant = if (hasAccess) ButtonVariant.Ghost else ButtonVariant.Primary,
            )
            NothingButton("Test pulse", onClick = onTestPulse, variant = ButtonVariant.Secondary)
        }
    }
}

private class Preview(val frame: DotMatrix, val pulsing: Boolean)
