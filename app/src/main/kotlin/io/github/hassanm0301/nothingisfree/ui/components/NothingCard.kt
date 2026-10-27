package io.github.hassanm0301.nothingisfree.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/**
 * The Nothing widget surface: flat, large radius, no elevation.
 * [inverted] flips to the opposite of the background (white card on black, black on white).
 */
@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    inverted: Boolean = false,
    outlined: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val nothing = NothingTheme.colors
    val container = when {
        outlined -> Color.Transparent
        inverted -> scheme.onBackground
        else -> nothing.card
    }
    val onContainer = if (inverted) scheme.background else scheme.onBackground
    val colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer)
    val border = if (outlined) BorderStroke(1.dp, nothing.hairline) else null
    val shape = MaterialTheme.shapes.large

    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(Spacing.cardPadding), content = content)
        }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(Spacing.cardPadding), content = content)
        }
    }
}
