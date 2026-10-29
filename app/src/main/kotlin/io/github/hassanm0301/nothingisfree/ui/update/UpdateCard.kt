package io.github.hassanm0301.nothingisfree.ui.update

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.hassanm0301.nothingisfree.core.update.AppUpdater
import io.github.hassanm0301.nothingisfree.core.update.AppUpdater.State
import io.github.hassanm0301.nothingisfree.core.update.Release
import io.github.hassanm0301.nothingisfree.ui.components.ButtonVariant
import io.github.hassanm0301.nothingisfree.ui.components.DotBar
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.StatusDot
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/** App updates from GitHub releases: status, release notes, and the download / install flow. */
@Composable
fun UpdateCard(
    state: State,
    currentVersion: String,
    onCheck: () -> Unit,
    onInstall: (Release) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var canInstall by remember { mutableStateOf(AppUpdater.canInstall(context)) }
    LifecycleResumeEffect(Unit) {
        canInstall = AppUpdater.canInstall(context)
        onPauseOrDispose { }
    }

    val release = when (state) {
        is State.Available -> state.release
        is State.Downloading -> state.release
        is State.Installing -> state.release
        is State.Failed -> state.release
        else -> null
    }

    NothingCard(modifier, inverted = state is State.Available) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoLabel("Installed · v$currentVersion", Modifier.weight(1f), color = muted(state))
            if (state is State.Available) {
                StatusDot()
                Spacer(Modifier.width(Spacing.sm))
                MonoLabel("New", color = NothingTheme.colors.accent)
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            when (state) {
                State.Idle, State.Checking -> "Checking for updates…"
                State.UpToDate -> "Up to date"
                is State.Available -> "${state.release.name} is out"
                is State.Downloading -> "Downloading ${state.release.tag}"
                is State.Installing -> "Installing ${state.release.tag}"
                is State.Failed -> "Update failed"
            },
            style = MaterialTheme.typography.titleLarge,
        )

        when (state) {
            is State.Available -> if (state.release.notes.isNotBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    state.release.notes.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted(state),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            is State.Downloading -> {
                Spacer(Modifier.height(Spacing.md))
                val pct = state.progress?.let { (it * 100).toInt() }
                DotBar(value = pct ?: 0, max = 100)
                Spacer(Modifier.height(Spacing.sm))
                MonoLabel(pct?.let { "$it%" } ?: "…", style = MaterialTheme.typography.labelSmall)
            }
            is State.Failed -> {
                Spacer(Modifier.height(Spacing.sm))
                Text(state.message, style = MaterialTheme.typography.bodyMedium, color = NothingTheme.colors.muted)
            }
            else -> Unit
        }

        if (release != null && !canInstall && state !is State.Downloading && state !is State.Installing) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "Allow this app to install unknown apps to update in place.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted(state),
            )
        }

        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            when {
                release?.apk != null && (state is State.Available || state is State.Failed) -> {
                    if (canInstall) {
                        NothingButton("Update", onClick = { onInstall(release) }, variant = ButtonVariant.Accent)
                    } else {
                        NothingButton(
                            "Allow installs",
                            onClick = { context.startActivity(AppUpdater.installPermissionIntent(context)) },
                        )
                    }
                    NothingButton(
                        "Release",
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, release.pageUrl.toUri())) },
                        variant = ButtonVariant.Ghost,
                    )
                }
                state is State.UpToDate || state is State.Failed ->
                    NothingButton("Check again", onClick = onCheck, variant = ButtonVariant.Secondary)
                else -> NothingButton("Working…", onClick = {}, variant = ButtonVariant.Secondary, enabled = false)
            }
        }
    }
}

/** Muted tone, respecting the inverted card used when an update is waiting. */
@Composable
private fun muted(state: State) =
    if (state is State.Available) {
        androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.55f)
    } else {
        NothingTheme.colors.muted
    }
