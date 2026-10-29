package io.github.hassanm0301.nothingisfree.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.hassanm0301.nothingisfree.dotmatrix.DotGlyph
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/** One destination in a [NothingTabBar]: a mono label over a 5x5 dot icon. */
class NothingTab(val label: String, val icon: DotGlyph)

/**
 * Bottom tab bar in the Nothing style: no container or pill, just dot icons and mono labels.
 * The selected tab is drawn in ink with a red dot under it; [badged] tabs get a red dot on the
 * icon, e.g. an update waiting.
 */
@Composable
fun NothingTabBar(
    tabs: List<NothingTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    badged: (Int) -> Boolean = { false },
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        DotDivider()
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            tabs.forEachIndexed { i, tab ->
                val isSelected = i == selected
                val tint = if (isSelected) MaterialTheme.colorScheme.onBackground else NothingTheme.colors.muted
                Column(
                    Modifier
                        .weight(1f)
                        .selectable(selected = isSelected, onClick = { onSelect(i) }, role = Role.Tab)
                        .padding(top = Spacing.md, bottom = Spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        DotIcon(tab.icon, tint)
                        if (badged(i)) StatusDot(Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp))
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    MonoLabel(tab.label, color = tint, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(Spacing.xs))
                    StatusDot(Modifier.size(4.dp), color = if (isSelected) NothingTheme.colors.accent else Color.Transparent)
                }
            }
        }
    }
}

@Composable
private fun DotIcon(glyph: DotGlyph, color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val cell = size.width / glyph.width
        for (y in 0 until glyph.height) for (x in 0 until glyph.width) {
            if (glyph[x, y]) drawCircle(color, cell * 0.36f, Offset((x + 0.5f) * cell, (y + 0.5f) * cell))
        }
    }
}

/** Dot icons for the app's tabs. */
object TabIcons {
    /** A round matrix with a lit centre. */
    val Glyph = DotGlyph(listOf(".###.", "#...#", "#.#.#", "#...#", ".###."))

    /** Four tiles. */
    val Widgets = DotGlyph(listOf("##.##", "##.##", ".....", "##.##", "##.##"))

    /** Two sliders. */
    val Settings = DotGlyph(listOf(".#...", "#####", ".....", "...#.", "#####"))
}
