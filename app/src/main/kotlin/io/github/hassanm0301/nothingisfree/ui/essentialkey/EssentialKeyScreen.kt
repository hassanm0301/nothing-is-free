package io.github.hassanm0301.nothingisfree.ui.essentialkey

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.essentialkey.EssentialKeyPreferences
import io.github.hassanm0301.nothingisfree.essentialkey.EssentialKeyService
import io.github.hassanm0301.nothingisfree.essentialkey.HapticPattern
import io.github.hassanm0301.nothingisfree.essentialkey.HapticStrength
import io.github.hassanm0301.nothingisfree.essentialkey.KeyAction
import io.github.hassanm0301.nothingisfree.essentialkey.KeyGesture
import io.github.hassanm0301.nothingisfree.essentialkey.KeyHaptics
import io.github.hassanm0301.nothingisfree.essentialkey.title
import io.github.hassanm0301.nothingisfree.essentialkey.unlock.DefaultFunctions
import io.github.hassanm0301.nothingisfree.essentialkey.unlock.UnlockStatus
import io.github.hassanm0301.nothingisfree.ui.components.ButtonVariant
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.components.NothingListRow
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.NothingSwitch
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.screenHeader
import io.github.hassanm0301.nothingisfree.ui.glyph.SliderControl
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/** The pages the Essential key tab opens from its list. */
private enum class KeyPage { ACTION, DEFAULTS, SETUP, TEST }

/**
 * Essential key tab: what each press of the Essential Key does, Nothing's own key functions
 * (which can be turned off to free the single press), feedback, and the key's setup.
 */
@Composable
fun EssentialKeyScreen(insets: PaddingValues) {
    var page by rememberSaveable { mutableStateOf<KeyPage?>(null) }
    var gesture by rememberSaveable { mutableStateOf(KeyGesture.SINGLE_PRESS) }
    BackHandler(enabled = page != null) { page = null }
    val pageState = rememberSaveableStateHolder()
    val back = { page = null }

    pageState.SaveableStateProvider(page?.name ?: "list") {
        when (page) {
            KeyPage.ACTION -> ActionPickerScreen(gesture, insets, onBack = back)
            KeyPage.DEFAULTS -> DefaultFunctionsScreen(insets, onBack = back)
            KeyPage.SETUP -> KeySetupScreen(insets, onBack = back)
            KeyPage.TEST -> KeyTestScreen(insets, onBack = back)
            null -> KeyList(
                insets,
                onPick = {
                    gesture = it
                    page = KeyPage.ACTION
                },
                onOpen = { page = it },
            )
        }
    }
}

/** Something that can change outside the app, read again each time the app comes back. */
@Composable
private fun <T> rememberOnResume(read: (Context) -> T): T {
    val context = LocalContext.current
    var value by remember { mutableStateOf(read(context)) }
    LifecycleResumeEffect(Unit) {
        value = read(context)
        onPauseOrDispose { }
    }
    return value
}

@Composable
private fun KeyList(insets: PaddingValues, onPick: (KeyGesture) -> Unit, onOpen: (KeyPage) -> Unit) {
    val context = LocalContext.current
    val actions by EssentialKeyPreferences.actions.collectAsStateWithLifecycle()
    val haptics by EssentialKeyPreferences.haptics.collectAsStateWithLifecycle()
    val onlyUnlocked by EssentialKeyPreferences.onlyWhenUnlocked.collectAsStateWithLifecycle()
    val serviceOn = rememberOnResume(EssentialKeyService::isOn)
    val defaults = rememberOnResume(DefaultFunctions::status)
    val dndAccess = rememberOnResume { c ->
        c.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
    }
    val keyHaptics = remember { KeyHaptics(context) }

    NothingScreen(insets) {
        screenHeader(
            kicker = if (serviceOn) "Essential Key · on" else "Essential Key · off",
            title = "Essential key",
            body = "Give each press of the Essential Key its own action, and turn Nothing's own " +
                "key functions off if you want the single press back.",
            live = serviceOn,
        )

        item {
            SectionHeader("Access")
            Text(
                if (serviceOn) {
                    "On. Presses of the Essential Key run the actions below."
                } else {
                    "Off. The key is read by an accessibility service, the only way an app can see " +
                        "hardware keys. Turn on opens Accessibility settings: pick Essential key there " +
                        "and switch it on. On a sideloaded install Android may block it as a " +
                        "restricted setting: App info › ⋮ › Allow restricted settings, then try again."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = NothingTheme.colors.muted,
            )
            Spacer(Modifier.height(Spacing.md))
            NothingButton(
                if (serviceOn) "Manage access" else "Turn on",
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                variant = if (serviceOn) ButtonVariant.Ghost else ButtonVariant.Primary,
            )
        }

        item {
            SectionHeader("Gestures")
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                KeyGesture.entries.forEach { g ->
                    val action = actions.actionFor(g)
                    val blocked = g == KeyGesture.SINGLE_PRESS && action != KeyAction.None &&
                        defaults != UnlockStatus.FREED && defaults != UnlockStatus.NO_CONSUMERS
                    LinkCard(
                        title = g.title,
                        summary = actionSummary(context, action),
                        status = when {
                            action == KeyAction.None -> "Nothing"
                            blocked -> "Essential Space still takes it"
                            else -> action.kind.group.title
                        },
                        statusColor = if (blocked) NothingTheme.colors.accent else NothingTheme.colors.muted,
                        onClick = { onPick(g) },
                    )
                }
            }
        }

        item {
            SectionHeader("Default functions")
            LinkCard(
                title = "Nothing's key functions",
                summary = "Essential Space and Recorder take the single press. Turn them off to use it here.",
                status = when (defaults) {
                    UnlockStatus.NO_CONSUMERS -> "None on this phone"
                    UnlockStatus.LOCKED -> "On"
                    UnlockStatus.PARTIALLY_FREED -> "Partly off"
                    UnlockStatus.FREED -> "Off · single press is free"
                },
                statusColor = NothingTheme.colors.accent,
                onClick = { onOpen(KeyPage.DEFAULTS) },
            )
        }

        item {
            SectionHeader("Vibration")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                HapticStrength.entries.forEach { s ->
                    NothingChip(s.title, selected = s == haptics.strength, onClick = {
                        EssentialKeyPreferences.updateHaptics { it.copy(strength = s) }
                        keyHaptics.perform(s, haptics.pattern)
                    })
                }
            }
            if (haptics.strength == HapticStrength.CUSTOM) {
                Spacer(Modifier.height(Spacing.md))
                CustomPattern(haptics.pattern, keyHaptics)
            }
            if (haptics.strength != HapticStrength.OFF) {
                NothingListRow(
                    "Only when an action runs",
                    subtitle = if (haptics.onActionOnly) "Once the gesture is known" else "On every press",
                    trailing = {
                        NothingSwitch(haptics.onActionOnly, { on ->
                            EssentialKeyPreferences.updateHaptics { it.copy(onActionOnly = on) }
                        })
                    },
                )
            }
        }

        item {
            SectionHeader("Screen lock")
            NothingListRow(
                "Only when unlocked",
                subtitle = if (onlyUnlocked) "The key does nothing on the lock screen" else "Works on the lock screen too",
                trailing = { NothingSwitch(onlyUnlocked, EssentialKeyPreferences::setOnlyWhenUnlocked) },
            )
            if (actions.uses(KeyAction.Kind.DO_NOT_DISTURB) || actions.uses(KeyAction.Kind.RINGER_CYCLE)) {
                SectionHeader("Permissions")
                NothingListRow(
                    "Do Not Disturb access",
                    subtitle = if (dndAccess) "Granted" else "Needed for Do Not Disturb and silent mode",
                    trailing = {
                        NothingButton(
                            if (dndAccess) "Manage" else "Allow",
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                            },
                            variant = if (dndAccess) ButtonVariant.Ghost else ButtonVariant.Primary,
                        )
                    },
                )
            }
        }

        item {
            SectionHeader("Key")
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                LinkCard(
                    title = "Key setup",
                    summary = "Teach the app which key is the Essential Key, if presses aren't seen",
                    status = "Scan code ${EssentialKeyPreferences.scanCode.collectAsStateWithLifecycle().value}",
                    onClick = { onOpen(KeyPage.SETUP) },
                )
                LinkCard(
                    title = "Key test",
                    summary = "Press the key and see the gesture, without running its action",
                    status = if (serviceOn) "Ready" else "Needs access",
                    onClick = { onOpen(KeyPage.TEST) },
                )
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(
                "Based on Essential Key Tools by KoukeNeko (MIT License).",
                style = MaterialTheme.typography.bodySmall,
                color = NothingTheme.colors.muted,
            )
        }
    }
}

/** The custom vibration: each slider saves and plays as it is let go. */
@Composable
private fun CustomPattern(pattern: HapticPattern, haptics: KeyHaptics) {
    fun set(next: HapticPattern) = EssentialKeyPreferences.updateHaptics { it.copy(pattern = next) }
    fun step(transform: (HapticPattern) -> HapticPattern) {
        val next = transform(EssentialKeyPreferences.haptics.value.pattern).coerced()
        set(next)
        haptics.play(next)
    }
    NothingCard(outlined = true) {
        SliderControl("Pulses", pattern.pulseCount, HapticPattern.PULSE_COUNT_RANGE, { "$it" },
            { set(pattern.copy(pulseCount = it)) }, { d -> step { it.copy(pulseCount = it.pulseCount + d) } })
        SliderControl("Pulse length", pattern.pulseMillis, HapticPattern.PULSE_MILLIS_RANGE, { "$it ms" },
            { set(pattern.copy(pulseMillis = it)) }, { d -> step { it.copy(pulseMillis = it.pulseMillis + d) } })
        if (pattern.pulseCount > 1) {
            SliderControl(
                "Pulses a second",
                pattern.frequencyHz,
                HapticPattern.FREQUENCY_HZ_RANGE.first..HapticPattern.maxFrequencyHz(pattern.pulseMillis),
                { "$it Hz" },
                { set(pattern.copy(frequencyHz = it)) },
                { d -> step { it.copy(frequencyHz = it.frequencyHz + d) } },
            )
        }
        SliderControl("Strength", pattern.amplitudePercent, HapticPattern.AMPLITUDE_PERCENT_RANGE, { "$it%" },
            { set(pattern.copy(amplitudePercent = it)) }, { d -> step { it.copy(amplitudePercent = it.amplitudePercent + d) } })
        if (!haptics.hasAmplitudeControl) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "This phone's motor is either on or off, so a lower strength is made by pulsing it quickly.",
                style = MaterialTheme.typography.bodySmall,
                color = NothingTheme.colors.muted,
            )
        }
        Spacer(Modifier.height(Spacing.md))
        NothingButton("Try it", onClick = { haptics.play(pattern) }, variant = ButtonVariant.Ghost)
    }
}

/** What [action] does, in a line: the app's or link's name where it has one. */
internal fun actionSummary(context: Context, action: KeyAction): String = when (action) {
    KeyAction.None -> "Does nothing"
    is KeyAction.LaunchApp -> "Opens ${DefaultFunctions.label(context, action.packageName)}"
    is KeyAction.OpenLink -> "Opens ${action.uri}"
    is KeyAction.Simple -> action.kind.title
}

/** A row of a list that opens a page, with its state underneath. */
@Composable
internal fun LinkCard(
    title: String,
    summary: String,
    status: String,
    onClick: () -> Unit,
    statusColor: androidx.compose.ui.graphics.Color = NothingTheme.colors.accent,
) {
    NothingCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Spacing.xs))
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = NothingTheme.colors.muted, maxLines = 2)
                Spacer(Modifier.height(Spacing.xs))
                MonoLabel(status, style = MaterialTheme.typography.labelSmall, color = statusColor)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = NothingTheme.colors.muted)
        }
    }
}
