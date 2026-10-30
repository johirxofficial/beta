package com.johirxofficial.x.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Accent = Color(0xFF22D3EE)
val AccentSoft = Color(0xFF67E8F9)
val BackgroundDark = Color(0xFF070B10)
val SurfaceDark = Color(0xFF0F1620)
val SurfaceElevated = Color(0xFF16202C)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF001319),
    primaryContainer = Color(0xFF0B3C47),
    onPrimaryContainer = Color(0xFFA5F3FC),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF03142B),
    tertiary = Color(0xFFF472B6),
    background = BackgroundDark,
    onBackground = Color(0xFFE8EEF5),
    surface = SurfaceDark,
    onSurface = Color(0xFFE8EEF5),
    surfaceVariant = Color(0xFF1A2532),
    onSurfaceVariant = Color(0xFF97A8B8),
    surfaceContainer = Color(0xFF111A24),
    surfaceContainerHigh = Color(0xFF16202C),
    surfaceContainerHighest = Color(0xFF1B2733),
    outline = Color(0xFF2A3A49),
    outlineVariant = Color(0xFF1E2B38),
    error = Color(0xFFF87171),
    onError = Color(0xFF2A0B0B),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E7490),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF083344),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    tertiary = Color(0xFFDB2777),
    background = Color(0xFFF5F8FC),
    onBackground = Color(0xFF0B1620),
    surface = Color.White,
    onSurface = Color(0xFF0B1620),
    surfaceVariant = Color(0xFFE3E9F1),
    onSurfaceVariant = Color(0xFF52616F),
    surfaceContainer = Color(0xFFEDF2F8),
    surfaceContainerHigh = Color(0xFFE7EDF5),
    surfaceContainerHighest = Color(0xFFDDE5EE),
    outline = Color(0xFFB7C4D2),
    outlineVariant = Color(0xFFD6DFE9),
    error = Color(0xFFB42318),
    onError = Color.White,
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.3.sp,
    ),
)

@Composable
fun BetaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
