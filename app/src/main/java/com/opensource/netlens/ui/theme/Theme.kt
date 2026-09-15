package com.opensource.netlens.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandBlue = Color(0xFF1B6EF3)
val BrandBlueDark = Color(0xFF4DA3FF)
val Navy = Color(0xFF0B1F3A)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = Color(0xFF002B5C),
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF101828),
    surface = Color.White,
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = Color(0xFF475467),
    error = Color(0xFFD92D20)
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004A8A),
    onPrimaryContainer = Color(0xFFD6E7FF),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF003731),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE6EDF7),
    surface = Color(0xFF121A2A),
    onSurface = Color(0xFFE6EDF7),
    surfaceVariant = Color(0xFF1C2536),
    onSurfaceVariant = Color(0xFFB0BEC9),
    error = Color(0xFFF97066)
)

@Composable
fun NetLensTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
