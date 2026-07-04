package com.polytrader.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Indigo500,
    onPrimary = Ink950,
    primaryContainer = Ink700,
    onPrimaryContainer = Indigo300,
    secondary = Aqua500,
    onSecondary = Ink950,
    secondaryContainer = Ink700,
    onSecondaryContainer = Aqua300,
    tertiary = GoldAi,
    onTertiary = Ink950,
    tertiaryContainer = Ink700,
    onTertiaryContainer = GoldAi,
    background = Ink900,
    onBackground = TextBrightDark,
    surface = Ink900,
    onSurface = TextBrightDark,
    surfaceVariant = Ink850,
    onSurfaceVariant = TextBodyDark,
    surfaceContainer = Ink850,
    surfaceContainerHigh = Ink800,
    surfaceContainerHighest = Ink700,
    surfaceContainerLow = Ink900,
    surfaceContainerLowest = Ink950,
    outline = InkBorder,
    outlineVariant = Ink700,
    error = NoRed,
    onError = Ink950,
)

private val LightColors = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Paper200,
    onPrimaryContainer = Indigo700,
    secondary = Aqua600,
    onSecondary = Color.White,
    secondaryContainer = Paper200,
    onSecondaryContainer = Aqua600,
    tertiary = GoldAiDim,
    onTertiary = Color.White,
    background = Paper50,
    onBackground = TextBrightLight,
    surface = Paper50,
    onSurface = TextBrightLight,
    surfaceVariant = Paper100,
    onSurfaceVariant = TextBodyLight,
    surfaceContainer = Paper100,
    surfaceContainerHigh = Paper200,
    surfaceContainerHighest = Paper200,
    surfaceContainerLow = Paper50,
    surfaceContainerLowest = Color.White,
    outline = PaperBorder,
    outlineVariant = Paper200,
    error = NoRedDim,
    onError = Color.White,
)

/** Colors beyond the Material scheme: market semantics & chart hues. */
@Immutable
data class PolyExtraColors(
    val yes: Color,
    val yesContainer: Color,
    val no: Color,
    val noContainer: Color,
    val aiAccent: Color,
    val aiAccentDim: Color,
    val chartLine: Color,
    val chartVolume: Color,
    val chartGrid: Color,
    val textMuted: Color,
    val series: List<Color>,
)

private val DarkExtras = PolyExtraColors(
    yes = YesGreen,
    yesContainer = YesGreenDim.copy(alpha = 0.24f),
    no = NoRed,
    noContainer = NoRedDim.copy(alpha = 0.24f),
    aiAccent = GoldAi,
    aiAccentDim = GoldAiDim,
    chartLine = Indigo400,
    chartVolume = Indigo700.copy(alpha = 0.55f),
    chartGrid = Ink700,
    textMuted = TextMutedDark,
    series = ChartSeries,
)

private val LightExtras = PolyExtraColors(
    yes = Color(0xFF178A4C),
    yesContainer = Color(0xFF178A4C).copy(alpha = 0.12f),
    no = Color(0xFFC93A57),
    noContainer = Color(0xFFC93A57).copy(alpha = 0.12f),
    aiAccent = Color(0xFFA5761E),
    aiAccentDim = GoldAiDim,
    chartLine = Indigo600,
    chartVolume = Indigo600.copy(alpha = 0.30f),
    chartGrid = Paper200,
    textMuted = TextMutedLight,
    series = ChartSeries,
)

val LocalPolyExtras = staticCompositionLocalOf { DarkExtras }

/** Convenience accessor: `PolyTheme.extras.yes` etc. */
object PolyTheme {
    val extras: PolyExtraColors
        @Composable @ReadOnlyComposable get() = LocalPolyExtras.current
}

val PolyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PolyTraderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extras = if (darkTheme) DarkExtras else LightExtras
    CompositionLocalProvider(LocalPolyExtras provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PolyTypography,
            shapes = PolyShapes,
            content = content,
        )
    }
}
