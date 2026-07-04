package com.polytrader.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.polytrader.app.R

/** Display serif for hero numbers & headlines. */
val SuezOne = FontFamily(
    Font(R.font.suez_one, FontWeight.Normal),
)

/** Workhorse UI sans. */
val PlexSans = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_bold, FontWeight.Bold),
)

/** Tabular numerals for prices, probabilities, and addresses. */
val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

val PolyTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SuezOne, fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = SuezOne, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.25).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = SuezOne, fontSize = 26.sp, lineHeight = 32.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = PlexSans, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = PlexSans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PlexSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = PlexSans, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PlexSans, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PlexSans, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp,
        letterSpacing = 0.4.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = PlexSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp,
        letterSpacing = 0.6.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = PlexSans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp,
        letterSpacing = 0.6.sp,
    ),
)
