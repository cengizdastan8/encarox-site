package com.encarox.kesit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

@Composable
fun KesitTheme(content: @Composable () -> Unit) {
    // Video duzenlemede arayuz her zaman koyu: acik zemin goruntunun
    // algilanan parlakligini ve rengini bozar.
    MaterialTheme(colorScheme = Dark, content = content)
}
