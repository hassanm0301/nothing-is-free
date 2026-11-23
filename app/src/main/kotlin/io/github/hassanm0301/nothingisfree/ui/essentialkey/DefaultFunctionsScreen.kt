// Adapted from Essential Key Tools by KoukeNeko (MIT License, see docs/licenses/EssentialKeyTools.txt).
package io.github.hassanm0301.nothingisfree.ui.essentialkey

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.hassanm0301.nothingisfree.essentialkey.unlock.DefaultFunctions
import io.github.hassanm0301.nothingisfree.essentialkey.unlock.PackageState
import io.github.hassanm0301.nothingisfree.essentialkey.unlock.ShizukuPackageToggler
import io.github.hassanm0301.nothingisfree.essentialkey.unlock.ShizukuStatus
import io.github.hassanm0301.nothingisfree.ui.components.ButtonVariant
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.detailHeader
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Nothing's own Essential Key functions, which take the single press before any app sees it.
 * Each can be turned off or back on through Shizuku, or by hand from its App info page. Everything
 * is read again when the app comes back, so a change made in Settings (or by an OS update) shows.
 */
@Composable
fun DefaultFunctionsScreen(insets: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val shizuku = remember { ShizukuPackageToggler(context) }
    val scope = rememberCoroutineScope()
    var states by remember { mutableStateOf(DefaultFunctions.states(context)) }
    var shizukuStatus by remember { mutableStateOf(shizuku.status()) }
    var busy by remember { mutableStateOf(false) }
    // What went wrong last time Shizuku was asked, kept on the page (a toast is gone too soon to read).
    var failure by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        states = DefaultFunctions.states(context)
        shizukuStatus = shizuku.status()
    }

    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose { }
    }
    DisposableEffect(shizuku) {
        val stop = shizuku.observeStatus { shizukuStatus = shizuku.status() }
        onDispose(stop)
    }

    val installed = states.filter { it.installed }

    NothingScreen(insets) {
        detailHeader(
            back = "Essential key",
            onBack = onBack,
            kicker = "Default functions",
            title = "Nothing's key functions",
            body = "Essential Space (and on some phones Recorder and Essential intelligence) answers " +
                "the Essential Key before any app can. Double, triple and long press work here " +
                "without changing anything; turn these off only if you want the single press too.",
        )

        item {
            NothingCard(inverted = true) {
                MonoLabel("Before you turn them off", color = MaterialTheme.colorScheme.background)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Turning them off disables those apps entirely: Essential Space, its captures and " +
                        "Recorder stop working until they are turned back on. They stay off if this app " +
                        "is uninstalled, and an OS update may switch them back on. Turn them on again " +
                        "here or from their App info pages.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            SectionHeader("On this phone")
            if (installed.isEmpty()) {
                Text(
                    "None of Nothing's key functions are installed, so nothing takes the key first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NothingTheme.colors.muted,
                )
            }
            states.forEach { state -> StateRow(state, DefaultFunctions.label(context, state.packageName)) }
        }

        if (installed.isNotEmpty()) {
            item {
                SectionHeader("With Shizuku")
                when (shizukuStatus) {
                    ShizukuStatus.NOT_RUNNING -> Note(
                        "Shizuku isn't running. With Shizuku (shizuku.rikka.app) started, the functions can " +
                            "be turned off and on from here, even where App info greys out Disable.",
                    )
                    ShizukuStatus.PERMISSION_DENIED -> Note(
                        "Shizuku permission was denied. Allow Nothing is Free in the Shizuku app, then come back.",
                    )
                    ShizukuStatus.PERMISSION_REQUIRED -> NothingButton(
                        "Allow Shizuku",
                        onClick = shizuku::requestPermission,
                    )
                    ShizukuStatus.READY -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        installed.forEach { state ->
                            val label = DefaultFunctions.label(context, state.packageName)
                            NothingButton(
                                if (state.isFreed) "Turn $label on" else "Turn $label off",
                                enabled = !busy,
                                variant = if (state.isFreed) ButtonVariant.Ghost else ButtonVariant.Primary,
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        val error = shizuku.setPackageEnabled(state.packageName, enabled = state.isFreed)
                                        busy = false
                                        refresh()
                                        val after = states.firstOrNull { it.packageName == state.packageName }
                                        failure = when {
                                            error != null -> "Couldn't change $label: $error"
                                            after != null && after.isFreed == state.isFreed ->
                                                "Android accepted the change, but $label is still " +
                                                    if (state.isFreed) "off." else "on."
                                            else -> null
                                        }
                                    }
                                },
                            )
                        }
                        failure?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = NothingTheme.colors.accent) }
                    }
                }
            }

            item {
                SectionHeader("By hand")
                Note(
                    "Open each one's App info and press Disable (or Enable to undo). Nothing OS doesn't " +
                        "offer Disable for some of them; use Shizuku or ADB there.",
                )
                Spacer(Modifier.height(Spacing.sm))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    installed.forEach { state ->
                        NothingButton(
                            "App info · ${DefaultFunctions.label(context, state.packageName)}",
                            variant = ButtonVariant.Ghost,
                            onClick = {
                                try {
                                    context.startActivity(DefaultFunctions.appInfoIntent(state.packageName))
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, "Couldn't open App info", Toast.LENGTH_LONG).show()
                                }
                            },
                        )
                    }
                }
            }

            item { AdbCommands(installed) }
        }
    }
}

/**
 * The same change from a computer, for when neither App info nor Shizuku will do it. These are
 * the commands App info's Disable and Enable run, so either can be undone the same way.
 */
@Composable
private fun AdbCommands(installed: List<PackageState>) {
    val clipboard = LocalClipboardManager.current
    val off = installed.joinToString("\n") { "adb shell pm disable-user --user 0 ${it.packageName}" }
    val on = installed.joinToString("\n") { "adb shell pm enable --user 0 ${it.packageName}" }
    SectionHeader("With ADB")
    Note("From a computer with USB debugging on (or wireless debugging), run:")
    Spacer(Modifier.height(Spacing.sm))
    NothingCard(outlined = true) {
        MonoLabel("Turn off")
        Spacer(Modifier.height(Spacing.xs))
        Text(off, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(Spacing.md))
        MonoLabel("Turn back on")
        Spacer(Modifier.height(Spacing.xs))
        Text(on, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
    Spacer(Modifier.height(Spacing.sm))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        NothingButton("Copy off", variant = ButtonVariant.Ghost, onClick = { clipboard.setText(AnnotatedString(off)) })
        NothingButton("Copy on", variant = ButtonVariant.Ghost, onClick = { clipboard.setText(AnnotatedString(on)) })
    }
}

@Composable
private fun StateRow(state: PackageState, label: String) {
    Row(Modifier.padding(vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            MonoLabel(state.packageName, style = MaterialTheme.typography.labelSmall)
        }
        MonoLabel(
            when {
                !state.installed -> "Not installed"
                state.isFreed -> "Off"
                else -> "On"
            },
            color = if (state.isConsuming) NothingTheme.colors.accent else NothingTheme.colors.muted,
        )
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = NothingTheme.colors.muted)
}
