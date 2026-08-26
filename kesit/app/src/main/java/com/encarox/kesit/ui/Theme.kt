package com.encarox.kesit.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFFFF4D6D)
val AccentSoft = Color(0xFFFF8FA3)
val TrackBg = Color(0xFF15181D)
val Surface1 = Color(0xFF13161B)
val Surface2 = Color(0xFF1C2027)
val Ink = Color(0xFFF2F4F7)
val InkDim = Color(0xFF9AA4B2)

private val Dark = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = AccentSoft,
    background = Color(0xFF0E1013),
    onBackground = Ink,
    surface = Surface1,
    onSurface = Ink,
    surfaceVariant = Surface2,
    onSurfaceVariant = InkDim,
    outline = Color(0xFF2A2F38),
)

private val Light = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = AccentSoft,
)

@Composable
fun KesitTheme(content: @Composable () -> Unit) {
    // Video duzenlemede koyu arayuz standart: goruntunun rengini bozmaz.
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Dark, content = content)
}
