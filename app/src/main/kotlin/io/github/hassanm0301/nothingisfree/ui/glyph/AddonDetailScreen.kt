package io.github.hassanm0301.nothingisfree.ui.glyph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.core.notifications.GlyphNotificationListener
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonId
import io.github.hassanm0301.nothingisfree.glyph.addon.BatteryBar
import io.github.hassanm0301.nothingisfree.glyph.addon.BatteryDrain
import io.github.hassanm0301.nothingisfree.glyph.addon.BatteryEdge
import io.github.hassanm0301.nothingisfree.glyph.addon.BatteryGauge
import io.github.hassanm0301.nothingisfree.glyph.addon.BatteryRing
import io.github.hassanm0301.nothingisfree.glyph.addon.RingDirection
import io.github.hassanm0301.nothingisfree.glyph.addon.RingGauge
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyFace
import io.github.hassanm0301.nothingisfree.glyph.addon.PulseSettings
import io.github.hassanm0301.nothingisfree.glyph.addon.PulseStep
import io.github.hassanm0301.nothingisfree.glyph.addon.PulseWave
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
            AddonId.BATTERY_RING -> item { BatteryRingPage(viewModel, anyToy = on.isNotEmpty()) }
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

    val update = viewModel::updatePulse
    SectionHeader("Steps")
    NothingCard {
        PulseShape(pulse.steps, Modifier.fillMaxWidth().height(56.dp))
        pulse.steps.forEachIndexed { i, step ->
            val last = i == pulse.steps.lastIndex
            Spacer(Modifier.height(Spacing.sm))
            DotDivider()
            Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                MonoLabel(if (last) "Step ${i + 1} · back to 0%" else "Step ${i + 1}", Modifier.weight(1f))
                if (!last && pulse.steps.size > 2) {
                    IconButton(onClick = { update { it.removeStep(i) } }) {
                        Icon(Icons.Filled.Close, "Remove step ${i + 1}", tint = NothingTheme.colors.muted)
                    }
                }
            }
            if (!last) {
                PercentControl(
                    "Brightness",
                    step.brightness,
                    onValueChange = { v -> update { it.withStep(i) { s -> s.copy(brightness = v) } } },
                    onStep = { d -> update { it.withStep(i) { s -> s.copy(brightness = s.brightness + d) } } },
                )
            }
            TimeControl(
                if (last) "Time to fade out" else "Time to reach it",
                step.durationMs,
                PulseSettings.MIN_STEP_MS..PulseSettings.MAX_STEP_MS,
                onValueChange = { ms -> update { it.withStep(i) { s -> s.copy(durationMs = ms) } } },
                onStep = { d -> update { it.withStep(i) { s -> s.copy(durationMs = s.durationMs + d) } } },
            )
        }
        Spacer(Modifier.height(Spacing.md))
        NothingButton(
            "Add step",
            onClick = { update { it.addStep() } },
            variant = ButtonVariant.Secondary,
            enabled = pulse.steps.size < PulseSettings.MAX_STEPS,
        )
    }
    Spacer(Modifier.height(Spacing.sm))
    Text(
        "Each step eases from the brightness before it to its own over its time; the first starts " +
            "dark and the last always goes back to 0%. Up to ${PulseSettings.MAX_STEPS} steps. " +
            "One run takes ${formatMs(pulse.cycleMs)}.",
        style = MaterialTheme.typography.bodyMedium,
        color = NothingTheme.colors.muted,
    )

    SectionHeader("Repeat")
    NothingCard {
        SliderControl(
            "Times",
            pulse.repeat,
            1..PulseSettings.MAX_REPEAT,
            { "×$it" },
            onValueChange = { n -> update { it.copy(repeat = n) } },
            onStep = { d -> update { it.copy(repeat = it.repeat + d) } },
        )
    }
    Spacer(Modifier.height(Spacing.sm))
    Text(
        "After a notification the steps play ${pulse.repeat} " + (if (pulse.repeat == 1) "time" else "times") +
            ", ${formatMs(pulse.durationMs)} in all, ending dark each time.",
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
        SliderControl(
            "Charge",
            battery.brightness,
            1..100,
            { "$it%" },
            onValueChange = { v -> viewModel.updateBattery { it.copy(brightness = v) } },
            onStep = { d -> viewModel.updateBattery { it.copy(brightness = (it.brightness + d).coerceIn(1, 100)) } },
        )
        Spacer(Modifier.height(Spacing.lg))
        PercentControl(
            "Used up",
            battery.emptyBrightness,
            onValueChange = { v -> viewModel.updateBattery { it.copy(emptyBrightness = v) } },
            onStep = { d ->
                viewModel.updateBattery { it.copy(emptyBrightness = (it.emptyBrightness + d).coerceIn(0, 100)) }
            },
        )
    }
    Spacer(Modifier.height(Spacing.sm))
    Text(
        "Used-up dots at 0% stay dark, so only the charge left shows.",
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

@Composable
private fun BatteryRingPage(viewModel: AppViewModel, anyToy: Boolean) {
    val ring by viewModel.ring.collectAsStateWithLifecycle()
    val phonePercent by viewModel.batteryPercent.collectAsStateWithLifecycle()
    // null follows the phone; otherwise the level picked on the slider.
    var pickedPercent by rememberSaveable { mutableStateOf<Int?>(null) }
    val percent = pickedPercent ?: phonePercent ?: 100
    val blank = remember { BlankFace(PREVIEW_SIZE) }
    val dots = remember(blank, ring.direction) { BatteryRing.cells(blank, ring).size }

    SectionHeader("Preview")
    NothingCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoLabel("On the matrix · $PREVIEW_SIZE×$PREVIEW_SIZE", Modifier.weight(1f))
            MonoLabel(
                "${BatteryRing.lit(percent, dots)} of $dots dots",
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ToyMatrix(blank, LocalTime.MIDNIGHT, 0, Modifier.width(168.dp), ring = RingGauge(ring, percent))
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
        "The whole edge is lit when full. Each of its $dots dots is a share of the charge and stays " +
            "lit until it is used up, so the last one shows down to 1%. Slide to try other levels; " +
            "only this preview changes.",
        style = MaterialTheme.typography.bodyMedium,
        color = NothingTheme.colors.muted,
    )

    SectionHeader("Drains")
    NothingCard {
        ChoiceChips(RingDirection.entries, ring.direction, { it.label }) { direction ->
            viewModel.updateRing { it.copy(direction = direction) }
        }
    }
    Spacer(Modifier.height(Spacing.sm))
    Text(
        "The top dot goes out first, then the ring empties " +
            (if (ring.direction == RingDirection.CLOCKWISE) "clockwise" else "anticlockwise") +
            " from there, so the dot just " +
            (if (ring.direction == RingDirection.CLOCKWISE) "left" else "right") +
            " of the top is the last to go.",
        style = MaterialTheme.typography.bodyMedium,
        color = NothingTheme.colors.muted,
    )

    SectionHeader("Brightness")
    NothingCard {
        SliderControl(
            "Charge",
            ring.brightness,
            1..100,
            { "$it%" },
            onValueChange = { v -> viewModel.updateRing { it.copy(brightness = v) } },
            onStep = { d -> viewModel.updateRing { it.copy(brightness = (it.brightness + d).coerceIn(1, 100)) } },
        )
        Spacer(Modifier.height(Spacing.lg))
        PercentControl(
            "Used up",
            ring.emptyBrightness,
            onValueChange = { v -> viewModel.updateRing { it.copy(emptyBrightness = v) } },
            onStep = { d ->
                viewModel.updateRing { it.copy(emptyBrightness = (it.emptyBrightness + d).coerceIn(0, 100)) }
            },
        )
    }
    Spacer(Modifier.height(Spacing.sm))
    Text(
        "Used-up dots at 0% stay dark, so only the charge left shows.",
        style = MaterialTheme.typography.bodyMedium,
        color = NothingTheme.colors.muted,
    )

    SectionHeader("On the phone")
    Text(
        if (anyToy) {
            "The toy reads the level each time its clock updates, once a minute, so it never wakes " +
                "the phone for it. The notification pulse uses the same edge, so while a pulse plays " +
                "it covers the ring; the level shows again when it ends. With the battery level bar " +
                "on too, the ring goes around the bar."
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

/** One run of [steps] as a line: time across, brightness up. */
@Composable
private fun PulseShape(steps: List<PulseStep>, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onBackground
    val hairline = NothingTheme.colors.hairline
    Canvas(modifier) {
        val cycle = steps.sumOf { it.durationMs }.coerceAtLeast(1)
        val inset = 2.dp.toPx()
        val h = size.height - 2 * inset
        drawLine(hairline, Offset(0f, size.height - inset), Offset(size.width, size.height - inset), 1.dp.toPx())
        val samples = (size.width / 2).toInt().coerceAtLeast(2)
        val path = Path()
        for (i in 0..samples) {
            val x = size.width * i / samples
            val level = PulseWave.level(steps, (cycle * i / samples).coerceAtMost(cycle - 1))
            val y = inset + h * (1 - level / 255f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, ink, style = Stroke(width = 2.dp.toPx()))
    }
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
