package io.github.hassanm0301.nothingisfree.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.ui.AppViewModel
import io.github.hassanm0301.nothingisfree.ui.components.ButtonVariant
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.detailHeader
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.PillShape
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing
import io.github.hassanm0301.nothingisfree.volume.PreciseVolumeService
import io.github.hassanm0301.nothingisfree.volume.VolumeFormat

/** The step out of 15 that the previews show. */
private const val SAMPLE_STEP = 7
private const val SAMPLE_MAX = 15

/** Whether Precise volume is switched on in Accessibility settings, checked again on each resume. */
@Composable
fun rememberPreciseVolumeOn(): State<Boolean> {
    val context = LocalContext.current
    val on = remember { mutableStateOf(PreciseVolumeService.isOn(context)) }
    LifecycleResumeEffect(Unit) {
        on.value = PreciseVolumeService.isOn(context)
        onPauseOrDispose { }
    }
    return on
}

/** Precise volume's page: how the level is written, and the access it needs. */
@Composable
fun PreciseVolumeScreen(viewModel: AppViewModel, insets: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val format by viewModel.volumeFormat.collectAsStateWithLifecycle()
    val on by rememberPreciseVolumeOn()

    NothingScreen(insets) {
        detailHeader(
            back = "Widgets",
            onBack = onBack,
            kicker = "Volume",
            title = "Precise volume",
            body = "The volume level as text, beside the volume bar while you change the volume. " +
                "The bar itself stays as it is.",
        )
        item {
            SectionHeader("Show as")
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                VolumeFormat.entries.forEach { f ->
                    NothingChip(f.title, selected = f == format, onClick = { viewModel.setVolumeFormat(f) })
                }
            }
            Spacer(Modifier.height(Spacing.md))
            LabelPreview(format)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                when (format) {
                    VolumeFormat.PERCENT -> "How far the volume is towards full, rounded to the nearest percent."
                    VolumeFormat.STEPS -> "The volume step out of how many the phone has for that sound."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = NothingTheme.colors.muted,
            )

            SectionHeader("Accessibility access")
            Text(
                if (on) {
                    "On. Change the volume to see the level beside the bar."
                } else {
                    "Off. Precise volume runs as an accessibility service, because only those can see " +
                        "where the volume bar is and draw beside it. Turn on opens Accessibility " +
                        "settings: pick Precise volume there and switch it on. On a sideloaded install, " +
                        "Android may block it as a restricted setting: App info › ⋮ › Allow restricted " +
                        "settings, then try again."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = NothingTheme.colors.muted,
            )
            Spacer(Modifier.height(Spacing.md))
            NothingButton(
                if (on) "Manage access" else "Turn on",
                onClick = { context.startActivity(PreciseVolumeService.settingsIntent()) },
                variant = if (on) ButtonVariant.Ghost else ButtonVariant.Primary,
            )

            SectionHeader("Good to know")
            Text(
                "To find the volume bar it looks at system windows only, and only while the volume is " +
                    "changing. It doesn't look at other apps, and it stores and sends nothing. Nothing " +
                    "shows while the screen is off. If the bar can't be found, the level shows near the " +
                    "right edge of the screen instead.",
                style = MaterialTheme.typography.bodyMedium,
                color = NothingTheme.colors.muted,
            )
        }
    }
}

/** The label as it sits beside a volume bar, in the app's theme (the real one follows the system's). */
@Composable
private fun LabelPreview(format: VolumeFormat) {
    val nothing = NothingTheme.colors
    NothingCard(outlined = true) {
        MonoLabel("Preview")
        Spacer(Modifier.height(Spacing.md))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                format.text(SAMPLE_STEP, SAMPLE_MAX),
                Modifier
                    .background(nothing.card, PillShape)
                    .border(1.dp, nothing.hairline, PillShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.width(8.dp))
            // A stand-in for the system's volume bar, filled to the sample step.
            Box(
                Modifier
                    .size(width = 44.dp, height = 160.dp)
                    .clip(PillShape)
                    .background(nothing.hairline),
            ) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(SAMPLE_STEP.toFloat() / SAMPLE_MAX)
                        .background(MaterialTheme.colorScheme.onBackground),
                )
            }
        }
    }
}
