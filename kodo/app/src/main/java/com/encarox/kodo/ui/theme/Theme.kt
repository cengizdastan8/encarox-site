package com.encarox.kodo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = KodoIndigo,
    onPrimary = KodoOnDark,
    primaryContainer = KodoIndigoDark,
    onPrimaryContainer = KodoOnDark,
    secondary = KodoMint,
    onSecondary = KodoInk,
    tertiary = KodoCoral,
    background = KodoInk,
    onBackground = KodoOnDark,
    surface = KodoSurfaceDark,
    onSurface = KodoOnDark,
    surfaceVariant = KodoSurfaceVariantDark,
    outline = KodoOutlineDark
)

private val LightColors = lightColorScheme(
    primary = KodoIndigo,
    onPrimary = KodoSurfaceLight,
    primaryContainer = KodoSurfaceVariantLight,
    onPrimaryContainer = KodoIndigoDark,
    secondary = KodoMintDark,
    onSecondary = KodoSurfaceLight,
    tertiary = KodoCoral,
    background = KodoCloud,
    onBackground = KodoOnLight,
    surface = KodoSurfaceLight,
    onSurface = KodoOnLight,
    surfaceVariant = KodoSurfaceVariantLight,
    outline = KodoOutlineLight
)

@Composable
fun KodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = KodoTypography,
        content = content
    )
}
