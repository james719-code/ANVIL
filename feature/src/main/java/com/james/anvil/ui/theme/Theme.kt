package com.james.anvil.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccentLight,
    onPrimary = Color(0xFF3A1303),
    primaryContainer = Color(0xFF5A2410),
    onPrimaryContainer = Color(0xFFFFDBCB),
    secondary = Color(0xFFB4BCC6),
    onSecondary = Color(0xFF1E252C),
    secondaryContainer = SurfaceElevatedDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = Color(0xFF9CC3E6),
    onTertiary = Color(0xFF0E2A42),
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF6C7279),
    outlineVariant = BorderDark,
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceElevatedDark,
    surfaceContainerHighest = Color(0xFF2B3035),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1D3),
    onPrimaryContainer = Color(0xFF4A1504),
    secondary = Color(0xFF4F5A66),
    onSecondary = Color.White,
    secondaryContainer = SurfaceElevatedLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = Color(0xFF2F5F8A),
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceElevatedLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFF8A8E93),
    outlineVariant = BorderLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = BackgroundLight,
    surfaceContainer = SurfaceElevatedLight,
    surfaceContainerHigh = SurfaceElevatedLight,
    surfaceContainerHighest = Color(0xFFE2E0D9),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun ANVILTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
