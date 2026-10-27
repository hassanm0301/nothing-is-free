package io.github.hassanm0301.nothingisfree.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Nothing OS palette: pure black, pure white, one red, and a short grey ramp.
// Keep this file the single source of truth; res/values/colors.xml mirrors the core five.
val NothingBlack = Color(0xFF000000)
val NothingWhite = Color(0xFFFFFFFF)
val NothingRed = Color(0xFFD71921)

val Grey900 = Color(0xFF1A1A1A)
val Grey800 = Color(0xFF2A2A2A)
val Grey700 = Color(0xFF3D3D3D)
val Grey500 = Color(0xFF7A7A7A)
val Grey400 = Color(0xFF9E9E9E)
val Grey300 = Color(0xFFBDBDBD)
val Grey200 = Color(0xFFDADADA)
val Grey100 = Color(0xFFF2F2F2)

/**
 * Tokens that Material 3's [androidx.compose.material3.ColorScheme] has no slot for.
 * Read them through [NothingTheme.colors].
 */
@Immutable
data class NothingColors(
    /** The single accent. Used sparingly: live indicators, progress, destructive emphasis. */
    val accent: Color,
    /** Dot-matrix "lit" pixel. */
    val dotOn: Color,
    /** Dot-matrix "unlit" pixel, visible but quiet. */
    val dotOff: Color,
    /** Card / widget surface. */
    val card: Color,
    /** Secondary text, captions, mono labels. */
    val muted: Color,
    /** Hairline strokes and outlined controls. */
    val hairline: Color,
)

val DarkNothingColors = NothingColors(
    accent = NothingRed,
    dotOn = NothingWhite,
    dotOff = Grey800,
    card = Grey900,
    muted = Grey400,
    hairline = Grey700,
)

val LightNothingColors = NothingColors(
    accent = NothingRed,
    dotOn = NothingBlack,
    dotOff = Grey200,
    card = Grey100,
    muted = Grey500,
    hairline = Grey200,
)

val LocalNothingColors = staticCompositionLocalOf { DarkNothingColors }
