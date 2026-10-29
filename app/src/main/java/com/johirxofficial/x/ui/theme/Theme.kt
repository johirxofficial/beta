package com.johirxofficial.x.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF22D3EE)
val SurfaceDark = Color(0xFF10161D)
val BackgroundDark = Color(0xFF0B0F14)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF001319),
    primaryContainer = Color(0xFF0E3A44),
    onPrimaryContainer = Color(0xFFA5F3FC),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF001319),
    tertiary = Color(0xFF7DD3FC),
    background = BackgroundDark,
    onBackground = Color(0xFFE6EDF3),
    surface = SurfaceDark,
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1B242E),
    onSurfaceVariant = Color(0xFF9FB0BF),
    surfaceContainer = Color(0xFF141C24),
    surfaceContainerHigh = Color(0xFF19222C),
    outline = Color(0xFF2C3A47),
    outlineVariant = Color(0xFF22303C),
    error = Color(0xFFF87171),
    onError = Color(0xFF2A0B0B),
)

@Composable
fun XTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content,
    )
}
