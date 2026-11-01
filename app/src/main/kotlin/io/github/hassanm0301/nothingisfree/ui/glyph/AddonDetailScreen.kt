package io.github.hassanm0301.nothingisfree.ui.glyph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.core.notifications.GlyphNotificationListener
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonId
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonPreferences
import io.github.hassanm0301.nothingisfree.glyph.addon.BatteryBar
import io.github.hassanm0301.nothingisfree.glyph.addon.BatteryDrain
import io.github.hassanm0301.nothingisfree.glyph.addon.BatteryEdge
import io.github.hassanm0301.nothingisfree.glyph.addon.BatteryGauge
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyFace
import io.github.hassanm0301.nothingisfree.ui.AppViewModel
import io.github.hassanm0301.nothingisfree.ui.components.ButtonVariant
import io.github.hassanm0301.nothingisfree.ui.components.DotDivider
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.components.NothingListRow
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.NothingSlider
import io.github.hassanm0301.nothingisfree.ui.components.NothingSwitch
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.detailHeader
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing
import java.time.LocalTime

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
            AddonId.BATTERY_LEVEL -> item { BatteryLevelSettings(viewModel, anyToy = on.isNotEmpty()) }
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

@Composable
private fun BatteryLevelSettings(viewModel: AppViewModel, anyToy: Boolean) {
    val battery by viewModel.battery.collectAsStateWithLifecycle()
    val phonePercent by viewModel.batteryPercent.collectAsStateWithLifecycle()
    // null follows the phone; otherwise the level picked on the slider.
    var pickedPercent by rememberSaveable { mutableStateOf<Int?>(null) }
    val percent = pickedPercent ?: phonePercent ?: 100
    val blank = remember { BlankFace(PREVIEW_SIZE) }

    SectionHeader("Preview")
    NothingCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoLabel("On the matrix · $PREVIEW_SIZE×$PREVIEW_SIZE", Modifier.weight(1f))
            MonoLabel(
                "${BatteryBar.lit(percent)} of ${BatteryBar.DOTS} dots",
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ToyMatrix(blank, LocalTime.MIDNIGHT, 0, Modifier.width(168.dp), BatteryGauge(battery, percent))
        }
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$percent%", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            NothingChip("Now", selected = pickedPercent == null, onClick = { pickedPercent = null })
        }
        NothingSlider(value = percent, onValueChange = { pickedPercent = it }, range = 0..100)
        Row {
            MonoLabel("0%", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
            MonoLabel("100%", style = MaterialTheme.typography.labelSmall)
        }
    }
    Spacer(Modifier.height(Spacing.sm))
    Text(
        "Each dot is ${100 / BatteryBar.DOTS}% of the charge and stays lit until it is used up, so the " +
            "last dot shows down to 1%. Slide to try other levels; only this preview changes.",
        style = MaterialTheme.typography.bodyMedium,
        color = NothingTheme.colors.muted,
    )

    SectionHeader("Position")
    NothingCard {
        MonoLabel("Edge")
        Spacer(Modifier.height(Spacing.sm))
        ChoiceChips(BatteryEdge.entries, battery.edge, { it.label }) { edge ->
            viewModel.updateBattery { it.copy(edge = edge) }
        }
        Spacer(Modifier.height(Spacing.lg))
        MonoLabel("Drains")
        Spacer(Modifier.height(Spacing.sm))
        if (battery.edge.horizontal) {
            ChoiceChips(BatteryDrain.entries, battery.drain, { it.label }) { drain ->
                viewModel.updateBattery { it.copy(drain = drain) }
            }
        } else {
            Text(
                "Top to bottom, like a battery standing upright.",
                style = MaterialTheme.typography.bodyMedium,
                color = NothingTheme.colors.muted,
            )
        }
    }
    Spacer(Modifier.height(Spacing.sm))
    Text(
        when {
            !battery.edge.horizontal -> "The top dot goes out first and the bottom one last."
            battery.drain == BatteryDrain.RIGHT_TO_LEFT -> "The right-hand dot goes out first and the left-hand one last."
            else -> "The left-hand dot goes out first and the right-hand one last."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = NothingTheme.colors.muted,
    )

    SectionHeader("Brightness")
    NothingCard {
        BrightnessControl(
            "Charge",
            battery.brightness,
            onValueChange = { v -> viewModel.updateBattery { it.copy(brightness = v) } },
            onStep = { d -> viewModel.updateBattery { it.copy(brightness = (it.brightness + d).coerceIn(1, 255)) } },
            range = 1..255,
        )
        Spacer(Modifier.height(Spacing.lg))
        BrightnessControl(
            "Used up",
            battery.emptyBrightness,
            onValueChange = { v -> viewModel.updateBattery { it.copy(emptyBrightness = v) } },
            onStep = { d ->
                viewModel.updateBattery { it.copy(emptyBrightness = (it.emptyBrightness + d).coerceIn(0, 255)) }
            },
        )
    }
    Spacer(Modifier.height(Spacing.sm))
    Text(
        "Used-up dots at 0 stay dark, so only the charge left shows.",
        style = MaterialTheme.typography.bodyMedium,
        color = NothingTheme.colors.muted,
    )

    SectionHeader("On the phone")
    Text(
        if (anyToy) {
            "The toy reads the level each time its clock updates, once a minute, so it never wakes " +
                "the phone for it. With the notification pulse on too, the ring pulses around the " +
                "bar and leaves the level showing."
        } else {
            "Switch the addon on from a toy's page first."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = NothingTheme.colors.muted,
    )
}

/** A face that draws nothing, to show an addon on its own. */
private class BlankFace(override val size: Int) : ToyFace {
    override fun render(time: LocalTime, is24Hour: Boolean) = DotMatrix(size, size)
    override fun occupies(x: Int, y: Int) = false
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
