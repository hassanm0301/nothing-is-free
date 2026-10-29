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
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyId
import io.github.hassanm0301.nothingisfree.ui.AppViewModel
import io.github.hassanm0301.nothingisfree.ui.components.ButtonVariant
import io.github.hassanm0301.nothingisfree.ui.components.DotDivider
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.components.NothingListRow
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.NothingSwitch
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.detailHeader
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/** One addon's settings: which toys it runs on, then its own options. */
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
        item {
            SectionHeader("Use on")
            ToyId.entries.forEachIndexed { i, toy ->
                if (i > 0) DotDivider()
                NothingListRow(
                    toy.title,
                    trailing = {
                        NothingSwitch(checked = toy in on, onCheckedChange = { viewModel.setAddon(addon, toy, it) })
                    },
                )
            }
        }
        when (addon) {
            AddonId.NOTIFICATION_PULSE -> item { NotificationPulseSettings(viewModel, anyToy = on.isNotEmpty()) }
        }
    }
}

@Composable
private fun NotificationPulseSettings(viewModel: AppViewModel, anyToy: Boolean) {
    val context = LocalContext.current
    val windowSeconds by viewModel.pulseWindowSeconds.collectAsStateWithLifecycle()
    var hasAccess by remember { mutableStateOf(GlyphNotificationListener.hasAccess(context)) }
    LifecycleResumeEffect(Unit) {
        hasAccess = GlyphNotificationListener.hasAccess(context)
        onPauseOrDispose { }
    }

    SectionHeader("Pulse for")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        AddonPreferences.pulseWindowChoices.forEach { seconds ->
            NothingChip(
                text = if (seconds < 60) "${seconds}s" else "${seconds / 60}m",
                selected = seconds == windowSeconds,
                onClick = { viewModel.setPulseWindowSeconds(seconds) },
            )
        }
    }
    Spacer(Modifier.height(Spacing.sm))
    Text(
        "How long the ring breathes after a notification, rounded up to whole breaths.",
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
            "Sends a pretend notification, so the ring pulses on the phone's matrix if one of these toys is showing."
        } else {
            "Switch the addon on for a toy first."
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
