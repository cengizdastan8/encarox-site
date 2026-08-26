package com.encarox.kodo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.encarox.kodo.ui.theme.KodoIndigo
import com.encarox.kodo.ui.theme.KodoMint

/**
 * The Kodo mark: a rounded "tray/inbox" with a code dropping into it
 * (a downward chevron) — codes fall into one place. Drawn in code so it
 * scales crisply at any size and needs no raster assets.
 */
@Composable
fun KodoLogo(
    size: Dp = 96.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.11f
        val gradient = Brush.linearGradient(
            colors = listOf(KodoIndigo, KodoMint),
            start = Offset(0f, 0f),
            end = Offset(s, s)
        )

        // Rounded tray (open-topped container).
        val left = s * 0.20f
        val right = s * 0.80f
        val top = s * 0.46f
        val bottom = s * 0.80f
        val radius = s * 0.10f
        val tray = Path().apply {
            moveTo(left, top)
            lineTo(left, bottom - radius)
            quadraticBezierTo(left, bottom, left + radius, bottom)
            lineTo(right - radius, bottom)
            quadraticBezierTo(right, bottom, right, bottom - radius)
            lineTo(right, top)
        }
        drawPath(tray, brush = gradient, style = Stroke(width = stroke, cap = StrokeCap.Round))

        // Code dropping in: vertical stem + downward chevron.
        val cx = s * 0.5f
        drawLine(
            brush = gradient,
            start = Offset(cx, s * 0.16f),
            end = Offset(cx, s * 0.52f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        val chevron = Path().apply {
            moveTo(cx - s * 0.14f, s * 0.40f)
            lineTo(cx, s * 0.56f)
            lineTo(cx + s * 0.14f, s * 0.40f)
        }
        drawPath(chevron, brush = gradient, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

