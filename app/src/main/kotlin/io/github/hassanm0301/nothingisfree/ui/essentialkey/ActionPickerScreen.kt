package io.github.hassanm0301.nothingisfree.ui.essentialkey

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.essentialkey.EssentialKeyPreferences
import io.github.hassanm0301.nothingisfree.essentialkey.KeyAction
import io.github.hassanm0301.nothingisfree.essentialkey.KeyAction.Kind
import io.github.hassanm0301.nothingisfree.essentialkey.KeyGesture
import io.github.hassanm0301.nothingisfree.essentialkey.title
import io.github.hassanm0301.nothingisfree.ui.components.ButtonVariant
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingListRow
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.detailHeader
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ICON_SIZE = 36.dp

/** An app a gesture can open. */
private class LaunchableApp(val packageName: String, val label: String, val icon: ImageBitmap?)

/** Which sub-list the picker shows: the actions, or the apps for "Open an app". */
private enum class PickerMode { ACTIONS, APPS }

/** Picks what [gesture] does: any action, an app, or a link the user types in. */
@Composable
fun ActionPickerScreen(gesture: KeyGesture, insets: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val actions by EssentialKeyPreferences.actions.collectAsStateWithLifecycle()
    val current = actions.actionFor(gesture)
    var mode by rememberSaveable { mutableStateOf(PickerMode.ACTIONS) }
    var link by rememberSaveable { mutableStateOf((current as? KeyAction.OpenLink)?.uri ?: "") }
    var apps by androidx.compose.runtime.remember { mutableStateOf<List<LaunchableApp>?>(null) }
    val iconPx = with(LocalDensity.current) { ICON_SIZE.roundToPx() }

    fun pick(action: KeyAction) {
        EssentialKeyPreferences.setAction(gesture, action)
        onBack()
    }

    if (mode == PickerMode.APPS) {
        LaunchedEffect(Unit) {
            if (apps == null) apps = withContext(Dispatchers.Default) { loadApps(context, iconPx) }
        }
    }

    NothingScreen(insets) {
        detailHeader(
            back = if (mode == PickerMode.APPS) "Actions" else "Essential key",
            onBack = { if (mode == PickerMode.APPS) mode = PickerMode.ACTIONS else onBack() },
            kicker = gesture.title,
            title = if (mode == PickerMode.APPS) "Open an app" else "Pick an action",
            body = actionSummary(context, current).let { "Now: $it." } +
                if (gesture == KeyGesture.SINGLE_PRESS) {
                    " The single press only reaches this app once Nothing's own key functions are off."
                } else {
                    ""
                },
        )

        if (mode == PickerMode.APPS) {
            val list = apps
            if (list == null) {
                item { Text("Loading apps…", color = NothingTheme.colors.muted) }
            } else {
                items(list, key = { it.packageName }) { app ->
                    NothingListRow(
                        app.label,
                        modifier = Modifier.clickable(role = Role.Button) { pick(KeyAction.LaunchApp(app.packageName)) },
                        subtitle = app.packageName,
                        leading = {
                            Box(Modifier.size(ICON_SIZE)) {
                                app.icon?.let { Image(it, contentDescription = null, Modifier.size(ICON_SIZE)) }
                            }
                        },
                        trailing = {
                            Selected(current == KeyAction.LaunchApp(app.packageName))
                        },
                    )
                }
            }
            return@NothingScreen
        }

        KeyAction.Group.entries.forEach { group ->
            item(key = group.name) {
                SectionHeader(group.title)
                Kind.entries.filter { it.group == group }.forEach { kind ->
                    NothingListRow(
                        kind.title,
                        modifier = Modifier.clickable(role = Role.Button) {
                            when (kind) {
                                Kind.LAUNCH_APP -> mode = PickerMode.APPS
                                Kind.OPEN_LINK -> Unit
                                else -> pick(KeyAction.of(kind))
                            }
                        },
                        subtitle = if (kind == Kind.LAUNCH_APP && current is KeyAction.LaunchApp) {
                            actionSummary(context, current)
                        } else {
                            kind.summary
                        },
                        trailing = { Selected(current.kind == kind) },
                    )
                    if (kind == Kind.OPEN_LINK) {
                        LinkEditor(link, onChange = { link = it }, onSave = { pick(KeyAction.OpenLink(link.trim())) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Selected(selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    RadioButton(
        selected = selected,
        onClick = null,
        colors = RadioButtonDefaults.colors(selectedColor = scheme.onBackground, unselectedColor = NothingTheme.colors.hairline),
    )
}

/** The link to open: typed in, and saved with the button. */
@Composable
private fun LinkEditor(value: String, onChange: (String) -> Unit, onSave: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val valid = value.isNotBlank() && value.trim().contains(':')
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("https://… · tel:… · intent:…") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = scheme.onBackground,
            unfocusedBorderColor = NothingTheme.colors.hairline,
            cursorColor = scheme.onBackground,
        ),
        isError = value.isNotBlank() && !valid,
    )
    Spacer(Modifier.height(Spacing.sm))
    Text(
        "Anything with a scheme: a web page, a phone number (tel:), a map (geo:), an app's deep link, " +
            "or an intent: URI for a specific screen of an app.",
        style = MaterialTheme.typography.bodySmall,
        color = NothingTheme.colors.muted,
    )
    Spacer(Modifier.height(Spacing.sm))
    NothingButton("Use this link", onClick = onSave, enabled = valid, variant = ButtonVariant.Ghost)
    Spacer(Modifier.height(Spacing.md))
}

/**
 * The launcher's apps, sorted by name, with icons drawn at the size the row shows them (never at a
 * drawable's own size, which a third-party app can make huge). One icon failing costs only its row.
 */
private fun loadApps(context: Context, iconPx: Int): List<LaunchableApp> {
    val pm = context.packageManager
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(launcher, PackageManager.ResolveInfoFlags.of(0))
        .distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != context.packageName }
        .map { info ->
            val icon = try {
                info.loadIcon(pm).toBitmap(iconPx, iconPx).asImageBitmap()
            } catch (e: RuntimeException) {
                Log.w("ActionPicker", "No icon for ${info.activityInfo.packageName}", e)
                null
            }
            LaunchableApp(info.activityInfo.packageName, info.loadLabel(pm).toString(), icon)
        }
        .sortedBy { it.label.lowercase() }
}
