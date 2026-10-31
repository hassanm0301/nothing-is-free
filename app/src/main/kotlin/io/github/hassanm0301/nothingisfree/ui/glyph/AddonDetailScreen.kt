package io.github.hassanm0301.nothingisfree.ui.glyph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.core.notifications.GlyphNotificationListener
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonId
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonPreferences
import io.github.hassanm0301.nothingisfree.ui.AppViewModel
import io.github.hassanm0301.nothingisfree.ui.components.ButtonVariant
import io.github.hassanm0301.nothingisfree.ui.components.DotDivider
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.components.NothingListRow
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.NothingSwitch
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.detailHeader
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/** One addon's settings. It is switched on per toy from each toy's page. */
@Composable
fun AddonDetailScreen(
    addon: AddonId,
    viewModel: AppViewModel,
    insets: PaddingValues,
    back: String,
    onBack: () -> Unit,
) {
    val addons by viewModel.addons.collectAsStateWithLifecycle()
    val on = addons[addon].orEmpty()

    NothingScreen(insets) {
        detailHeader(back, onBack, kicker = "Addon", title = addon.title, body = addon.summary)
        when (addon) {
            AddonId.NOTIFICATION_PULSE -> item { NotificationPulseSettings(viewModel, anyToy = on.isNotEmpty()) }
        }
    }
}

@Composable
private fun NotificationPulseSettings(viewModel: AppViewModel, anyToy: Boolean) {
    val context = LocalContext.current
    val pulse by viewModel.pulse.collectAsStateWithLifecycle()
    var hasAccess by remember { mutableStateOf(GlyphNotificationListener.hasAccess(context)) }
    LifecycleResumeEffect(Unit) {
        hasAccess = GlyphNotificationListener.hasAccess(context)
        onPauseOrDispose { }
    }

    SectionHeader("Pulse")
    NothingCard {
        MonoLabel("Time to max brightness")
        Spacer(Modifier.height(Spacing.sm))
        ChoiceChips(AddonPreferences.riseChoices, pulse.riseMs, ::formatMs) { ms ->
            viewModel.updatePulse { it.copy(riseMs = ms) }
        }
        Spacer(Modifier.height(Spacing.lg))

        BrightnessControl(
            "Max brightness",
            pulse.maxBrightness,
            onValueChange = { v -> viewModel.updatePulse { it.copy(maxBrightness = v) } },
            onStep = { d -> viewModel.updatePulse { it.copy(maxBrightness = (it.maxBrightness + d).coerceIn(1, 255)) } },
            range = 1..255,
        )
        Spacer(Modifier.height(Spacing.lg))

        MonoLabel("Number of pulses")
        Spacer(Modifier.height(Spacing.sm))
        ChoiceChips(AddonPreferences.countChoices, pulse.count, { "$it" }) { n ->
            viewModel.updatePulse { it.copy(count = n) }
        }
    }
    Spacer(Modifier.height(Spacing.sm))
    val total = formatMs(pulse.durationMs)
    Text(
        "Each pulse fades up to max brightness and back down in the same time. " +
            if (pulse.count == 1) "One pulse takes $total." else "${pulse.count} pulses take $total.",
        style = MaterialTheme.typography.bodyMedium,
        color = NothingTheme.colors.muted,
    )

    SectionHeader("Battery saver")
    NothingListRow(
        "Check once a minute",
        subtitle = "With the clock update",
        trailing = {
            NothingSwitch(pulse.batterySaver, { on -> viewModel.updatePulse { it.copy(batterySaver = on) } })
        },
    )
    DotDivider()
    NothingListRow(
        "Pulse for old ones too",
        subtitle = "Every minute while any are in the shade",
        trailing = {
            NothingSwitch(
                pulse.repeatWhileShowing,
                { on -> viewModel.updatePulse { it.copy(repeatWhileShowing = on) } },
                enabled = pulse.batterySaver,
            )
        },
    )
    Text(
        if (pulse.batterySaver) {
            "On: notifications are only looked at when the clock updates each minute, so the pulse " +
                "runs with that update instead of keeping the phone up on its own. It pulses " +
                if (pulse.repeatWhileShowing) {
                    "every minute while a notification is still in the shade."
                } else {
                    "if one arrived during the last minute."
                }
        } else {
            "Off: the ring pulses the moment a notification arrives."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = NothingTheme.colors.muted,
    )

    SectionHeader("Notification access")
    Text(
        if (hasAccess) {
            "On. Silent notifications, ongoing ones like media, and ones Do Not Disturb holds back don't pulse."
        } else {
            "Needs notification access to know when something arrives. On a sideloaded install, Android " +
                "may block it as a restricted setting: App info › ⋮ › Allow restricted settings, then try again."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = NothingTheme.colors.muted,
    )
    Spacer(Modifier.height(Spacing.md))
    NothingButton(
        if (hasAccess) "Manage access" else "Grant access",
        onClick = { context.startActivity(GlyphNotificationListener.accessSettingsIntent(context)) },
        variant = if (hasAccess) ButtonVariant.Ghost else ButtonVariant.Primary,
    )

    SectionHeader("Test on phone")
    Text(
        if (anyToy) {
            "Sends a pretend notification, so the ring pulses on the phone's matrix if a toy it is on " +
                "for is showing" + if (pulse.batterySaver) ", from the next minute." else "."
        } else {
            "Switch the addon on from a toy's page first."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = NothingTheme.colors.muted,
    )
    Spacer(Modifier.height(Spacing.md))
    NothingButton(
        "Test on phone",
        onClick = viewModel::testPulseOnPhone,
        variant = ButtonVariant.Secondary,
        enabled = anyToy,
    )
}

/** One chip per choice in a wrapping row, the [selected] one filled. */
@Composable
private fun <T> ChoiceChips(choices: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        choices.forEach { choice ->
            NothingChip(text = label(choice), selected = choice == selected, onClick = { onPick(choice) })
        }
    }
}

/** A duration for the UI: "1.5 s", "30 s", "1 min 30 s". */
private fun formatMs(ms: Long): String {
    val tenths = (ms + 50) / 100
    val seconds = tenths / 10
    return when {
        seconds >= 60 -> "${seconds / 60} min" + if (seconds % 60 != 0L) " ${seconds % 60} s" else ""
        tenths % 10 == 0L -> "$seconds s"
        else -> "$seconds.${tenths % 10} s"
    }
}
