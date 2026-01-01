package com.james.anvil.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
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

private val DarkColorScheme = darkColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    primaryContainer = DeepBlue,
    onPrimaryContainer = PaleBlue,
    secondary = MutedTeal,
    onSecondary = Color.White,
    secondaryContainer = DeepTeal,
    onSecondaryContainer = PaleTeal,
    tertiary = LightTeal,
    onTertiary = Charcoal,
    background = Charcoal,
    onBackground = TextWhite,
    surface = SurfaceDark,
    onSurface = TextWhite,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateGray,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DeepBlue,
    onPrimary = Color.White,
    primaryContainer = PaleBlue,
    onPrimaryContainer = DeepBlue,
    secondary = DeepTeal,
    onSecondary = Color.White,
    secondaryContainer = PaleTeal,
    onSecondaryContainer = DeepTeal,
    tertiary = MutedTeal,
    onTertiary = Color.White,
    background = PaperWhite,
    onBackground = TextBlack,
    surface = SurfaceLight,
    onSurface = TextBlack,
    surfaceVariant = PaperWhite,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun ANVILTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color by default for consistent branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Update status bar color
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
        content = content
    )
}