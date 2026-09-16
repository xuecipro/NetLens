package com.opensource.netlens.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val BrandBlue = Color(0xFF1B6EF3)
val BrandBlueDark = Color(0xFF4DA3FF)
val Navy = Color(0xFF0B1F3A)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = Color(0xFF002B5C),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF0F766E),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF5B21B6),
    background = Color(0xFFF6F8FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004A8A),
    onPrimaryContainer = Color(0xFFD6E7FF),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2A),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE6EDF7),
    surface = Color(0xFF121A2A),
    onSurface = Color(0xFFE6EDF7),
    surfaceVariant = Color(0xFF1C2536),
    onSurfaceVariant = Color(0xFFB0BEC9),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** True when this device can sample wallpaper / Monet colors. */
fun monetSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Material You / Monet — same approach as Google's Compose samples:
 * dynamicLight/DarkColorScheme(activity), then apply to window bars.
 */
@Composable
fun NetLensTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    // Must be Activity context for wallpaper colors (MIUI/OEM safe)
    val context = LocalContext.current
    val activity = context.findActivity()
    val colorScheme = resolveColorScheme(context, activity, dark, dynamicColor)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = activity?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}

fun resolveColorScheme(
    context: Context,
    activity: Activity?,
    dark: Boolean,
    dynamicColor: Boolean
): ColorScheme {
    if (dynamicColor && monetSupported()) {
        val ctx = activity ?: context
        return runCatching {
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }.getOrNull() ?: if (dark) DarkColors else LightColors
    }
    return if (dark) DarkColors else LightColors
}

/** Preview swatches so users can see whether Monet is active. */
fun previewSwatches(scheme: ColorScheme): List<Pair<String, Color>> = listOf(
    "primary" to scheme.primary,
    "secondary" to scheme.secondary,
    "tertiary" to scheme.tertiary,
    "surface" to scheme.surface,
    "background" to scheme.background
)
