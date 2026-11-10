package io.github.hassanm0301.nothingisfree.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.ui.AppViewModel
import io.github.hassanm0301.nothingisfree.ui.components.NothingButton
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.detailHeader
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing
import io.github.hassanm0301.nothingisfree.widget.ClockWidget
import io.github.hassanm0301.nothingisfree.widget.ClockWidgetFont
import io.github.hassanm0301.nothingisfree.widget.ClockWidgetLines
import io.github.hassanm0301.nothingisfree.widget.ClockWidgetRenderer

/** The size the preview is drawn for, about a 4x2 widget. */
private val PreviewWidth: Dp = 280.dp
private val PreviewHeight: Dp = 132.dp

/** Stands in for a wallpaper: the widget's text is white whatever the app's theme. */
private val PreviewBackdrop = Color(0xFF1A1A1A)

/** The clock widget's page: what it looks like now, its font, and putting it on the home screen. */
@Composable
fun ClockWidgetScreen(viewModel: AppViewModel, insets: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val font by viewModel.clockWidgetFont.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()

    // The next alarm can only have changed while the app was away; read it again on each return.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    val minute = now.withSecond(0).withNano(0)
    val lines = remember(minute, resumes) { ClockWidget.lines(context, minute) }

    NothingScreen(insets) {
        detailHeader(
            back = "Widgets",
            onBack = onBack,
            kicker = "Home screen",
            title = "Clock",
            body = "The time, the day and date, and your next alarm, for the home screen.",
        )
        item {
            SectionHeader("Preview")
            WidgetPreview(lines, font)

            SectionHeader("Font")
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ClockWidgetFont.entries.forEach { f ->
                    NothingChip(f.title, selected = f == font, onClick = { viewModel.setClockWidgetFont(f) })
                }
            }

            SectionHeader("Home screen")
            if (ClockWidget.canPin(context)) {
                Text(
                    "Add it from here, or from the home screen's own widget list under Nothing is Free. " +
                        "Resize it there: the text grows and shrinks with it. A tap opens your alarms.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NothingTheme.colors.muted,
                )
                Spacer(Modifier.height(Spacing.md))
                NothingButton("Add to home screen", onClick = { ClockWidget.requestPin(context) })
            } else {
                Text(
                    "Add it from the home screen's widget list, under Nothing is Free. Resize it there: " +
                        "the text grows and shrinks with it. A tap opens your alarms.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NothingTheme.colors.muted,
                )
            }

            SectionHeader("Good to know")
            Text(
                "Android won't lend a widget an app's own fonts, so the app draws this one as a picture " +
                    "and redraws it each minute. That never wakes the phone: asleep, nothing runs, and the " +
                    "clock catches up as the phone wakes. The next alarm costs nothing: Android says when " +
                    "it changes. It is the next one any app has set as an alarm clock, usually the clock app's.",
                style = MaterialTheme.typography.bodyMedium,
                color = NothingTheme.colors.muted,
            )
        }
    }
}

/** The widget as the home screen draws it, from the same renderer, on a dark backdrop. */
@Composable
private fun WidgetPreview(lines: ClockWidgetLines, font: ClockWidgetFont) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val picture = remember(lines, font, density) {
        with(density) {
            ClockWidgetRenderer.render(context, lines, font, PreviewWidth.roundToPx(), PreviewHeight.roundToPx())
        }.asImageBitmap()
    }
    Box(
        Modifier
            .fillMaxWidth()
            .background(PreviewBackdrop, MaterialTheme.shapes.large)
            .padding(Spacing.cardPadding),
        contentAlignment = Alignment.Center,
    ) {
        Image(picture, contentDescription = lines.spoken, Modifier.height(PreviewHeight))
    }
}
