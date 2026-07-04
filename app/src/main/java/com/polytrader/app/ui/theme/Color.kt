package com.polytrader.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * "Meridian" palette — dark-first, hand-tuned for financial data density.
 */

// Ink scale (dark surfaces)
val Ink950 = Color(0xFF070A12)
val Ink900 = Color(0xFF0B0F1A)
val Ink850 = Color(0xFF101726)
val Ink800 = Color(0xFF141C2E)
val Ink700 = Color(0xFF1B2540)
val Ink600 = Color(0xFF232D4A)
val InkBorder = Color(0xFF243050)

// Light surfaces
val Paper50 = Color(0xFFF7F8FC)
val Paper100 = Color(0xFFEEF1F8)
val Paper200 = Color(0xFFE1E6F2)
val PaperBorder = Color(0xFFD4DAEA)

// Brand
val Indigo300 = Color(0xFF9DB4FF)
val Indigo400 = Color(0xFF7F9CFF)
val Indigo500 = Color(0xFF6C8CFF)
val Indigo600 = Color(0xFF5470E6)
val Indigo700 = Color(0xFF3F55B8)

val Aqua300 = Color(0xFF7BEFD9)
val Aqua400 = Color(0xFF4FE7CB)
val Aqua500 = Color(0xFF38E1C6)
val Aqua600 = Color(0xFF1FB8A0)

// Semantic
val YesGreen = Color(0xFF2FD576)
val YesGreenDim = Color(0xFF1D7A48)
val NoRed = Color(0xFFFF5C7A)
val NoRedDim = Color(0xFF97364A)
val GoldAi = Color(0xFFF5B84C)
val GoldAiDim = Color(0xFF8F6A2A)

// Text
val TextBrightDark = Color(0xFFEAF0FB)
val TextBodyDark = Color(0xFFC6D0E2)
val TextMutedDark = Color(0xFF8B98B3)
val TextBrightLight = Color(0xFF141B2C)
val TextBodyLight = Color(0xFF3A4560)
val TextMutedLight = Color(0xFF6B7690)

// Chart series (categorical, colorblind-aware ordering)
val ChartSeries = listOf(
    Indigo500,
    Aqua500,
    GoldAi,
    Color(0xFFB78CFF), // violet
    Color(0xFFFF9E6C), // coral
    Color(0xFF6CCBFF), // sky
)
