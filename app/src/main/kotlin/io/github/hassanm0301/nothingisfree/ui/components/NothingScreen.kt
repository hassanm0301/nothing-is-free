package io.github.hassanm0301.nothingisfree.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/** A tab's scrolling body: screen gutters, plus [insets] from the scaffold (status and tab bar). */
@Composable
fun NothingScreen(
    insets: PaddingValues,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    val direction = LocalLayoutDirection.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.screen + insets.calculateStartPadding(direction),
            end = Spacing.screen + insets.calculateEndPadding(direction),
            top = insets.calculateTopPadding() + Spacing.lg,
            bottom = insets.calculateBottomPadding() + Spacing.xl,
        ),
        content = content,
    )
}

/** Big Ndot title for a tab, with a mono kicker above and optional secondary copy below. */
fun LazyListScope.screenHeader(kicker: String, title: String, body: String? = null, live: Boolean = false) = item {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (live) {
            StatusDot()
            Spacer(Modifier.width(Spacing.sm))
        }
        MonoLabel(kicker)
    }
    Spacer(Modifier.height(Spacing.md))
    Text(title, style = MaterialTheme.typography.displayLarge)
    if (body != null) {
        Spacer(Modifier.height(Spacing.sm))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = NothingTheme.colors.muted)
    }
}
