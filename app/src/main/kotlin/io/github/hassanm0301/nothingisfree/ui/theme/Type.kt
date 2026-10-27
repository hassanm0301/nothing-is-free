package io.github.hassanm0301.nothingisfree.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.hassanm0301.nothingisfree.R

/** Ndot 57: the dot-matrix display face. Headlines, numerals, the clock. */
val Ndot = FontFamily(Font(R.font.ndot57, FontWeight.Normal))

/** Ndot 55: slightly looser dots. Use for very large sizes where 57 gets dense. */
val NdotLoose = FontFamily(Font(R.font.ndot55, FontWeight.Normal))

/** Ndot 55 Caps: all-caps variant with extra glyphs. */
val NdotCaps = FontFamily(Font(R.font.ndot55_caps, FontWeight.Normal))

/** NType 82: the body face. Regular for copy, Headline for titles. */
val NType = FontFamily(
    Font(R.font.ntype82_regular, FontWeight.Normal),
    Font(R.font.ntype82_headline, FontWeight.Medium),
    Font(R.font.ntype82_headline, FontWeight.SemiBold),
    Font(R.font.ntype82_headline, FontWeight.Bold),
)

/** NType 82 Mono: single-weight mono used by Nothing for small system readouts. */
val NTypeMono = FontFamily(Font(R.font.ntype82_mono, FontWeight.Normal))

/** Lettera Mono: labels, captions, metadata. Pair with uppercase + tracking. */
val Lettera = FontFamily(
    Font(R.font.lettera_mono_light, FontWeight.Light),
    Font(R.font.lettera_mono_regular, FontWeight.Normal),
    Font(R.font.lettera_mono_medium, FontWeight.Medium),
)

/** Tracking applied to uppercase mono labels. */
val LabelTracking = 0.08.em

val NothingTypography = Typography(
    // Display + headline: Ndot. Nothing never bolds the dot face, size does the work.
    displayLarge = TextStyle(fontFamily = Ndot, fontSize = 64.sp, lineHeight = 68.sp),
    displayMedium = TextStyle(fontFamily = Ndot, fontSize = 48.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = Ndot, fontSize = 36.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = Ndot, fontSize = 32.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Ndot, fontSize = 26.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = Ndot, fontSize = 22.sp, lineHeight = 26.sp),

    // Titles + body: NType 82.
    titleLarge = TextStyle(fontFamily = NType, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = NType, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = NType, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = NType, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = NType, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = NType, fontSize = 12.sp, lineHeight = 16.sp),

    // Labels: Lettera Mono, meant to be rendered uppercase with LabelTracking.
    labelLarge = TextStyle(fontFamily = Lettera, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = LabelTracking),
    labelMedium = TextStyle(fontFamily = Lettera, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = LabelTracking),
    labelSmall = TextStyle(fontFamily = Lettera, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = LabelTracking),
)
