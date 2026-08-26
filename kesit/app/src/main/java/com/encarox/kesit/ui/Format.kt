package com.encarox.kesit.ui

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** 01:23.456 / 1:01:23.456 bicimi. Milimetrik kesmede milisaniye sart. */
fun timecode(ms: Long, millis: Boolean = true): String {
    val safe = ms.coerceAtLeast(0L)
    val hours = safe / 3_600_000
    val minutes = (safe % 3_600_000) / 60_000
    val seconds = (safe % 60_000) / 1000
    val rest = safe % 1000
    val head = if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
    return if (millis) String.format(Locale.US, "%s.%03d", head, rest) else head
}

/** Sure etiketi: kisa surelerde saniye ondaligiyla, uzunlarda dakikayla. */
fun durationLabel(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    return when {
        safe < 60_000 -> String.format(Locale.US, "%.2f sn", safe / 1000.0)
        else -> timecode(safe, millis = false)
    }
}

fun sizeLabel(bytes: Long): String {
    if (bytes <= 0) return "-"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return String.format(Locale.US, if (value >= 100) "%.0f %s" else "%.1f %s", value, units[index])
}

fun elapsedLabel(ms: Long): String =
    if (ms < 1000) "$ms ms" else String.format(Locale.US, "%.1f sn", ms / 1000.0)

fun signedMs(ms: Long): String = (if (ms >= 0) "+" else "-") + "${abs(ms)} ms"

fun percent(fraction: Float): Int = (fraction.coerceIn(0f, 1f) * 100).roundToInt()
