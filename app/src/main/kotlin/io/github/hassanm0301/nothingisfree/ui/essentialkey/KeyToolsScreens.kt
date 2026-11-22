// Adapted from Essential Key Tools by KoukeNeko (MIT License, see docs/licenses/EssentialKeyTools.txt).
package io.github.hassanm0301.nothingisfree.ui.essentialkey

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.essentialkey.DEFAULT_ESSENTIAL_KEY_SCAN_CODE
import io.github.hassanm0301.nothingisfree.essentialkey.EssentialKeyPreferences
import io.github.hassanm0301.nothingisfree.essentialkey.EssentialKeyService
import io.github.hassanm0301.nothingisfree.essentialkey.KeyEventStream
import io.github.hassanm0301.nothingisfree.essentialkey.ObservedGesture
import io.github.hassanm0301.nothingisfree.essentialkey.ObservedKeyEvent
import io.github.hassanm0301.nothingisfree.essentialkey.title
import io.github.hassanm0301.nothingisfree.ui.components.ButtonVariant
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.detailHeader
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Shown where the page needs the service and it isn't on. */
private const val NEEDS_SERVICE = "Turn on Essential key access first (Essential key › Access): only " +
    "the accessibility service sees key presses."

/**
 * Learns which key is the Essential Key: while this page shows, the service reports every key
 * (bar volume) and runs nothing. Press the Essential Key, then save its scan code.
 */
@Composable
fun KeySetupScreen(insets: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val saved by EssentialKeyPreferences.scanCode.collectAsStateWithLifecycle()
    var captured by remember { mutableStateOf<ObservedKeyEvent?>(null) }
    val serviceOn = remember { EssentialKeyService.isOn(context) }

    DisposableEffect(Unit) {
        KeyEventStream.learning = true
        onDispose { KeyEventStream.learning = false }
    }
    LaunchedEffect(Unit) {
        KeyEventStream.keys.collect { if (it.down) captured = it }
    }

    NothingScreen(insets) {
        detailHeader(
            back = "Essential key",
            onBack = onBack,
            kicker = "Key setup",
            title = "Press the Essential Key",
            body = "Most Nothing phones report it as scan code $DEFAULT_ESSENTIAL_KEY_SCAN_CODE, which is " +
                "the default. If presses aren't seen on yours, press it now and save what shows.",
        )
        item {
            if (!serviceOn) {
                Text(NEEDS_SERVICE, style = MaterialTheme.typography.bodyMedium, color = NothingTheme.colors.accent)
                Spacer(Modifier.height(Spacing.md))
            }
            NothingCard {
                MonoLabel("Last key pressed")
                Spacer(Modifier.height(Spacing.sm))
                val event = captured
                if (event == null) {
                    Text("Waiting for a key…", color = NothingTheme.colors.muted)
                } else {
                    Value("Scan code", event.scanCode.toString())
                    Value("Key code", event.keyCode.toString())
                }
            }
            Spacer(Modifier.height(Spacing.md))
            NothingCard(outlined = true) {
                MonoLabel("Saved")
                Spacer(Modifier.height(Spacing.sm))
                Text("$saved", style = MaterialTheme.typography.displaySmall)
            }
            Spacer(Modifier.height(Spacing.md))
            val event = captured
            NothingButton(
                "Use scan code ${event?.scanCode ?: "…"}",
                enabled = event != null && event.scanCode != saved,
                onClick = { event?.let { EssentialKeyPreferences.setScanCode(it.scanCode) } },
            )
            if (saved != DEFAULT_ESSENTIAL_KEY_SCAN_CODE) {
                Spacer(Modifier.height(Spacing.sm))
                NothingButton(
                    "Back to $DEFAULT_ESSENTIAL_KEY_SCAN_CODE",
                    variant = ButtonVariant.Ghost,
                    onClick = { EssentialKeyPreferences.setScanCode(DEFAULT_ESSENTIAL_KEY_SCAN_CODE) },
                )
            }
        }
    }
}

/** Shows the Essential Key's events and the gesture they make, without running its action. */
@Composable
fun KeyTestScreen(insets: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val serviceOn = remember { EssentialKeyService.isOn(context) }
    var gesture by remember { mutableStateOf<ObservedGesture?>(null) }
    val log = remember { mutableStateListOf<ObservedKeyEvent>() }

    DisposableEffect(Unit) {
        KeyEventStream.testing = true
        onDispose { KeyEventStream.testing = false }
    }
    LaunchedEffect(Unit) {
        KeyEventStream.keys.collect {
            log.add(0, it)
            if (log.size > LOG_SIZE) log.removeAt(log.lastIndex)
        }
    }
    LaunchedEffect(Unit) {
        KeyEventStream.gestures.collect { gesture = it }
    }

    NothingScreen(insets) {
        detailHeader(
            back = "Essential key",
            onBack = onBack,
            kicker = "Key test",
            title = "Try a gesture",
            body = "Press the Essential Key once, twice, three times or hold it. Its action doesn't " +
                "run while this page shows.",
        )
        item {
            if (!serviceOn) {
                Text(NEEDS_SERVICE, style = MaterialTheme.typography.bodyMedium, color = NothingTheme.colors.accent)
                Spacer(Modifier.height(Spacing.md))
            }
            NothingCard(inverted = gesture != null) {
                MonoLabel(
                    gesture?.let { "Recognised · ${TIME.format(Instant.ofEpochMilli(it.atMs))}" } ?: "Gesture",
                    color = if (gesture != null) MaterialTheme.colorScheme.background else NothingTheme.colors.muted,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(gesture?.gesture?.title ?: "—", style = MaterialTheme.typography.displaySmall)
            }
            SectionHeader("Key events")
            if (log.isEmpty()) {
                Text("None yet.", color = NothingTheme.colors.muted)
            }
            log.forEach { e ->
                Value(if (e.down) "Down" else "Up", "scan ${e.scanCode} · key ${e.keyCode}")
            }
        }
    }
}

@Composable
private fun Value(label: String, value: String) {
    Row(Modifier, verticalAlignment = Alignment.CenterVertically) {
        MonoLabel(label, Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private const val LOG_SIZE = 12
private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
