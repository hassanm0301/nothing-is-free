package io.github.hassanm0301.nothingisfree.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.PillShape

enum class ButtonVariant { Primary, Secondary, Accent, Ghost }

/** Pill button with an uppercase mono label. */
@Composable
fun NothingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val nothing = NothingTheme.colors
    val (container, content) = when (variant) {
        ButtonVariant.Primary -> scheme.primary to scheme.onPrimary
        ButtonVariant.Secondary -> nothing.card to scheme.onBackground
        ButtonVariant.Accent -> nothing.accent to Color.White
        ButtonVariant.Ghost -> Color.Transparent to scheme.onBackground
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.35f),
            disabledContentColor = content.copy(alpha = 0.5f),
        ),
        border = if (variant == ButtonVariant.Ghost) BorderStroke(1.dp, nothing.hairline) else null,
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
        elevation = null,
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

/** Circular icon button on a card surface. */
@Composable
fun NothingIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val nothing = NothingTheme.colors
    val scheme = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (accent) nothing.accent else nothing.card,
            contentColor = if (accent) Color.White else scheme.onBackground,
        ),
    ) {
        Icon(icon, contentDescription, Modifier.size(22.dp))
    }
}

/** A small filled dot, used as a live/status indicator. */
@Composable
fun StatusDot(modifier: Modifier = Modifier, color: Color = NothingTheme.colors.accent) {
    Box(modifier.size(8.dp).background(color, PillShape))
}
