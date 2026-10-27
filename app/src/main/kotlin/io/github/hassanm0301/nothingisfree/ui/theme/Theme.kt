package io.github.hassanm0301.nothingisfree.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = NothingWhite,
    onPrimary = NothingBlack,
    primaryContainer = Grey900,
    onPrimaryContainer = NothingWhite,
    secondary = NothingRed,
    onSecondary = NothingWhite,
    secondaryContainer = NothingRed,
    onSecondaryContainer = NothingWhite,
    tertiary = Grey400,
    onTertiary = NothingBlack,
    background = NothingBlack,
    onBackground = NothingWhite,
    surface = NothingBlack,
    onSurface = NothingWhite,
    surfaceVariant = Grey900,
    onSurfaceVariant = Grey400,
    surfaceContainer = Grey900,
    surfaceContainerHigh = Grey800,
    surfaceContainerHighest = Grey700,
    surfaceContainerLow = Grey900,
    surfaceContainerLowest = NothingBlack,
    outline = Grey700,
    outlineVariant = Grey800,
    error = NothingRed,
    onError = NothingWhite,
    inverseSurface = NothingWhite,
    inverseOnSurface = NothingBlack,
    inversePrimary = NothingBlack,
    scrim = NothingBlack,
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = NothingBlack,
    onPrimary = NothingWhite,
    primaryContainer = Grey100,
    onPrimaryContainer = NothingBlack,
    secondary = NothingRed,
    onSecondary = NothingWhite,
    secondaryContainer = NothingRed,
    onSecondaryContainer = NothingWhite,
    tertiary = Grey500,
    onTertiary = NothingWhite,
    background = NothingWhite,
    onBackground = NothingBlack,
    surface = NothingWhite,
    onSurface = NothingBlack,
    surfaceVariant = Grey100,
    onSurfaceVariant = Grey500,
    surfaceContainer = Grey100,
    surfaceContainerHigh = Grey200,
    surfaceContainerHighest = Grey300,
    surfaceContainerLow = Grey100,
    surfaceContainerLowest = NothingWhite,
    outline = Grey200,
    outlineVariant = Grey100,
    error = NothingRed,
    onError = NothingWhite,
    inverseSurface = NothingBlack,
    inverseOnSurface = NothingWhite,
    inversePrimary = NothingWhite,
    scrim = NothingBlack,
)

/** Accessors for the Nothing-specific tokens; use alongside [MaterialTheme]. */
object NothingTheme {
    val colors: NothingColors
        @Composable @ReadOnlyComposable get() = LocalNothingColors.current
}

@Composable
fun NothingTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) DarkScheme else LightScheme
    val nothingColors = if (dark) DarkNothingColors else LightNothingColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalNothingColors provides nothingColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = NothingTypography,
            shapes = NothingShapes,
            content = content,
        )
    }
}
