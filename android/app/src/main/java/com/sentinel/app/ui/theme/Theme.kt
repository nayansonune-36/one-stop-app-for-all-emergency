package com.sentinel.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = Color.White,
    primaryContainer = RedSoft,
    onPrimaryContainer = RedPrimary,
    secondary = LightNavy,
    onSecondary = Color.White,
    secondaryContainer = LightSoft,
    onSecondaryContainer = LightText,
    tertiary = LightGreen,
    onTertiary = Color.White,
    background = LightPaper,
    onBackground = LightText,
    surface = LightCard,
    onSurface = LightText,
    surfaceVariant = LightSoft,
    onSurfaceVariant = LightMuted,
    outline = LightLine,
    outlineVariant = LightLine,
    error = RedPrimary,
    onError = Color.White
)

val DarkColorScheme = darkColorScheme(
    primary = RedPrimary,
    onPrimary = Color.White,
    primaryContainer = RedPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = DarkText,
    onSecondary = DarkNavy,
    secondaryContainer = DarkSoft,
    onSecondaryContainer = DarkText,
    tertiary = DarkGreen,
    onTertiary = DarkPaper,
    background = DarkPaper,
    onBackground = DarkText,
    surface = DarkCard,
    onSurface = DarkText,
    surfaceVariant = DarkSoft,
    onSurfaceVariant = DarkMuted,
    outline = DarkLine,
    outlineVariant = DarkLine,
    error = RedPrimary,
    onError = Color.White
)

@Composable
fun SentinelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = SentinelShapes,
            content = content
        )
    }
}
