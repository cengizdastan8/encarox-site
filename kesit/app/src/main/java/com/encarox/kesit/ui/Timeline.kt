package com.encarox.kesit.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.encarox.kesit.media.Filmstrip
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Suruklemenin neyi hareket ettirdigi. */
enum class DragTarget { START, END, SELECTION, SCRUB }

/** Secimin inebilecegi en kisa sure. */
const val MIN_SELECTION_MS = 200L

private val TRACK_HEIGHT = 78.dp
private val OVERVIEW_HEIGHT = 26.dp
private val HANDLE_WIDTH = 13.dp
private val TOUCH_SLOP = 30.dp

/**
 * Iki katli zaman cizgisi.
 *
 * Ust serit: videonun tamami. Su an hangi bolgeye yakinlastigini gosterir ve
 * surukleyerek o pencereyi kaydirmani saglar.
 *
 * Alt serit: yakinlastirilmis pencere; kare onizlemeleri, iki tutamac ve
 * oynatma cizgisi burada. Yakinlastikca bir piksel daha az milisaniye eder,
 * yani parmak ayni hassasiyetle calisirken kesme cozunurlugu artar.
 */
@Composable
fun TrimTimeline(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    playheadMs: Long,
    viewStartMs: Long,
    viewEndMs: Long,
    filmstrip: Filmstrip?,
    aspectRatio: Float,
    onSelectionChange: (Long, Long, DragTarget) -> Unit,
    onScrub: (Long) -> Unit,
    onWindowChange: (Long, Long) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OverviewBar(
            durationMs = durationMs,
            startMs = startMs,
            endMs = endMs,
            playheadMs = playheadMs,
            viewStartMs = viewStartMs,
            viewEndMs = viewEndMs,
            onWindowChange = onWindowChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(OVERVIEW_HEIGHT)
                .clip(RoundedCornerShape(6.dp))
                .background(Surface2),
        )

        FilmstripTrack(
            durationMs = durationMs,
            startMs = startMs,
            endMs = endMs,
            playheadMs = playheadMs,
            viewStartMs = viewStartMs,
            viewEndMs = viewEndMs,
            filmstrip = filmstrip,
            aspectRatio = aspectRatio,
            onSelectionChange = onSelectionChange,
            onScrub = onScrub,
            onWindowChange = onWindowChange,
            onDragStateChange = onDragStateChange,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(TRACK_HEIGHT)
                .clip(RoundedCornerShape(8.dp))
                .background(TrackBg),
        )
    }
}

@Composable
private fun OverviewBar(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    playheadMs: Long,
    viewStartMs: Long,
    viewEndMs: Long,
    onWindowChange: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = durationMs.coerceAtLeast(1L)
    val latest by rememberUpdatedState(longArrayOf(viewStartMs, viewEndMs, total))

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                fun pan(centerX: Float) {
                    val (vs, ve, dur) = Triple(latest[0], latest[1], latest[2])
                    val span = (ve - vs).coerceAtLeast(1L)
                    val center = (centerX / size.width.coerceAtLeast(1) * dur).toLong()
                    var newStart = center - span / 2
                    newStart = newStart.coerceIn(0L, (dur - span).coerceAtLeast(0L))
                    onWindowChange(newStart, newStart + span)
                }
                detectDragGestures(
                    onDragStart = { pan(it.x) },
                    onDrag = { change, _ -> change.consume(); pan(change.position.x) },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val (vs, ve, dur) = Triple(latest[0], latest[1], latest[2])
                    val span = (ve - vs).coerceAtLeast(1L)
                    val center = (offset.x / size.width.coerceAtLeast(1) * dur).toLong()
                    val newStart = (center - span / 2).coerceIn(0L, (dur - span).coerceAtLeast(0L))
                    onWindowChange(newStart, newStart + span)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        fun x(t: Long) = (t.toFloat() / total * w).coerceIn(0f, w)

        // Secilen bolge
        drawRect(
            color = Accent.copy(alpha = 0.45f),
            topLeft = Offset(x(startMs), 0f),
            size = Size((x(endMs) - x(startMs)).coerceAtLeast(2f), h),
        )
        // Oynatma konumu
        drawRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(x(playheadMs) - 1f, 0f),
            size = Size(2f, h),
        )
        // Yakinlastirma penceresi
        val vs = x(viewStartMs)
        val ve = x(viewEndMs)
        drawRect(
            color = Color.White.copy(alpha = 0.10f),
            topLeft = Offset(vs, 0f),
            size = Size((ve - vs).coerceAtLeast(3f), h),
        )
        val stroke = 1.5f
        drawRect(
            color = Color.White.copy(alpha = 0.7f),
            topLeft = Offset(vs, stroke / 2),
            size = Size((ve - vs).coerceAtLeast(3f), h - stroke),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
        )
    }
}

@Composable
private fun FilmstripTrack(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    playheadMs: Long,
    viewStartMs: Long,
    viewEndMs: Long,
    filmstrip: Filmstrip?,
    aspectRatio: Float,
    onSelectionChange: (Long, Long, DragTarget) -> Unit,
    onScrub: (Long) -> Unit,
    onWindowChange: (Long, Long) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    var widthPx by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableFloatStateOf(0f) }

    val span = (viewEndMs - viewStartMs).coerceAtLeast(1L)
    // Sadece surukleme sirasinda degisen degerleri okuyabilmek icin: pointerInput
    // anahtarina koyarsak jest ortasinda dedektor yeniden kurulur ve surukleme kopar.
    val live by rememberUpdatedState(
        TrackSnapshot(durationMs, startMs, endMs, viewStartMs, viewEndMs)
    )

    val thumbHeightPx = heightPx.roundToInt().coerceAtLeast(1)
    val thumbWidthPx = (thumbHeightPx * aspectRatio).roundToInt().coerceIn(8, 512)
    val slotCount = if (widthPx > 0 && thumbWidthPx > 0) {
        ceil(widthPx / thumbWidthPx).toInt().coerceIn(1, 64)
    } else 0

    val thumbs = rememberThumbnails(
        filmstrip = filmstrip,
        viewStartMs = viewStartMs,
        viewEndMs = viewEndMs,
        count = slotCount,
        widthPx = thumbWidthPx,
        heightPx = thumbHeightPx,
    )

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    widthPx = it.width.toFloat()
                    heightPx = it.height.toFloat()
                }
                .pointerInput(Unit) {
                    var target = DragTarget.SCRUB
                    var pointerX = 0f
                    val slopPx = with(density) { TOUCH_SLOP.toPx() }

                    fun timeAt(x: Float): Long {
                        val s = live
                        val sp = (s.viewEnd - s.viewStart).coerceAtLeast(1L)
                        val ratio = (x / size.width.coerceAtLeast(1)).toDouble()
                        return (s.viewStart + (ratio * sp)).toLong()
                    }

                    fun xOf(t: Long): Float {
                        val s = live
                        val sp = (s.viewEnd - s.viewStart).coerceAtLeast(1L)
                        return ((t - s.viewStart).toFloat() / sp) * size.width
                    }

                    /** Tutamac kenara dayandiginda pencereyi kaydirir. */
                    fun autoPan(x: Float) {
                        val s = live
                        val sp = (s.viewEnd - s.viewStart).coerceAtLeast(1L)
                        if (sp >= s.duration) return
                        val edge = size.width * 0.10f
                        val step = (sp * 0.04).toLong().coerceAtLeast(1L)
                        val shift = when {
                            x < edge && s.viewStart > 0 -> -step
                            x > size.width - edge && s.viewEnd < s.duration -> step
                            else -> 0L
                        }
                        if (shift == 0L) return
                        val newStart = (s.viewStart + shift).coerceIn(0L, (s.duration - sp).coerceAtLeast(0L))
                        onWindowChange(newStart, newStart + sp)
                    }

                    detectDragGestures(
                        onDragStart = { offset ->
                            val s = live
                            pointerX = offset.x
                            val sx = xOf(s.start)
                            val ex = xOf(s.end)
                            val dStart = abs(offset.x - sx)
                            val dEnd = abs(offset.x - ex)
                            target = when {
                                dStart <= slopPx && dStart <= dEnd -> DragTarget.START
                                dEnd <= slopPx -> DragTarget.END
                                offset.x > sx && offset.x < ex -> DragTarget.SELECTION
                                else -> DragTarget.SCRUB
                            }
                            if (target == DragTarget.START || target == DragTarget.END) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onDragStateChange(true)
                            if (target == DragTarget.SCRUB) onScrub(timeAt(offset.x).coerceIn(0L, s.duration))
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            pointerX = (pointerX + amount.x)
                            val s = live
                            val t = timeAt(pointerX)
                            when (target) {
                                DragTarget.START -> {
                                    val limit = (s.end - MIN_SELECTION_MS).coerceAtLeast(0L)
                                    val ns = t.coerceIn(0L, limit)
                                    onSelectionChange(ns, s.end, DragTarget.START)
                                    autoPan(pointerX)
                                }
                                DragTarget.END -> {
                                    val floor = (s.start + MIN_SELECTION_MS).coerceAtMost(s.duration)
                                    val ne = t.coerceIn(floor, s.duration)
                                    onSelectionChange(s.start, ne, DragTarget.END)
                                    autoPan(pointerX)
                                }
                                DragTarget.SELECTION -> {
                                    val sp = (live.viewEnd - live.viewStart).coerceAtLeast(1L)
                                    val deltaMs = (amount.x / size.width.coerceAtLeast(1) * sp).toLong()
                                    val length = s.end - s.start
                                    val room = (s.duration - length).coerceAtLeast(0L)
                                    val ns = (s.start + deltaMs).coerceIn(0L, room)
                                    onSelectionChange(ns, ns + length, DragTarget.SELECTION)
                                    autoPan(pointerX)
                                }
                                DragTarget.SCRUB -> onScrub(t.coerceIn(0L, s.duration))
                            }
                        },
                        onDragEnd = { onDragStateChange(false) },
                        onDragCancel = { onDragStateChange(false) },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val s = live
                        val sp = (s.viewEnd - s.viewStart).coerceAtLeast(1L)
                        val t = s.viewStart + (offset.x / size.width.coerceAtLeast(1) * sp).toLong()
                        onScrub(t.coerceIn(0L, s.duration))
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            fun x(t: Long) = (((t - viewStartMs).toFloat() / span) * w)

            // Kare onizlemeleri
            if (slotCount > 0) {
                val slotW = w / slotCount
                thumbs.forEachIndexed { index, image ->
                    if (image == null) return@forEachIndexed
                    drawImage(
                        image = image,
                        dstOffset = IntOffset((index * slotW).roundToInt(), 0),
                        dstSize = IntSize(ceil(slotW).toInt(), h.roundToInt()),
                    )
                }
            }

            val sx = x(startMs).coerceIn(0f, w)
            val ex = x(endMs).coerceIn(0f, w)

            // Secim disinda kalan kisimlari karart
            if (sx > 0f) drawRect(Color.Black.copy(alpha = 0.62f), Offset.Zero, Size(sx, h))
            if (ex < w) drawRect(Color.Black.copy(alpha = 0.62f), Offset(ex, 0f), Size(w - ex, h))

            // Secim cercevesi
            val border = 3f
            drawRect(Accent, Offset(sx, 0f), Size((ex - sx).coerceAtLeast(1f), border))
            drawRect(Accent, Offset(sx, h - border), Size((ex - sx).coerceAtLeast(1f), border))

            // Tutamaclar
            val handleW = with(density) { HANDLE_WIDTH.toPx() }
            drawHandle(sx - handleW, handleW, h, atStart = true)
            drawHandle(ex, handleW, h, atStart = false)

            // Oynatma cizgisi
            val px = x(playheadMs)
            if (px >= -2f && px <= w + 2f) {
                drawRect(Color.White, Offset(px - 1.5f, 0f), Size(3f, h))
                drawCircle(Color.White, radius = 5f, center = Offset(px, 5f))
            }
        }
    }
}

private data class TrackSnapshot(
    val duration: Long,
    val start: Long,
    val end: Long,
    val viewStart: Long,
    val viewEnd: Long,
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandle(
    left: Float,
    width: Float,
    height: Float,
    atStart: Boolean,
) {
    drawRoundRect(
        color = Accent,
        topLeft = Offset(left, 0f),
        size = Size(width, height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            if (atStart) width / 2 else 2f,
            if (atStart) width / 2 else 2f,
        ),
    )
    val gripW = 2f
    val gripH = height * 0.34f
    val top = (height - gripH) / 2
    drawRect(Color.White.copy(alpha = 0.9f), Offset(left + width / 2 - 3.5f, top), Size(gripW, gripH))
    drawRect(Color.White.copy(alpha = 0.9f), Offset(left + width / 2 + 1.5f, top), Size(gripW, gripH))
}

/**
 * Gorunen pencere icin kare onizlemelerini yukler. Once onbellek aninda
 * basilir, sonra kisa bir gecikmeyle eksikler tamamlanir; boylece hizli
 * yakinlastirmada bosuna kare cozulmez.
 */
@Composable
private fun rememberThumbnails(
    filmstrip: Filmstrip?,
    viewStartMs: Long,
    viewEndMs: Long,
    count: Int,
    widthPx: Int,
    heightPx: Int,
): List<ImageBitmap?> {
    val slots = remember { mutableStateListOf<ImageBitmap?>() }

    LaunchedEffect(filmstrip, viewStartMs, viewEndMs, count, widthPx, heightPx) {
        if (filmstrip == null || count <= 0 || widthPx <= 0 || heightPx <= 0) return@LaunchedEffect
        val span = (viewEndMs - viewStartMs).coerceAtLeast(1L)
        val times = List(count) { i -> viewStartMs + ((i + 0.5) / count * span).toLong() }

        val cached: List<Bitmap?> = times.map { filmstrip.cached(it) }
        slots.clear()
        cached.forEach { slots.add(it?.asImageBitmap()) }

        delay(90)

        times.forEachIndexed { index, time ->
            if (slots.getOrNull(index) != null) return@forEachIndexed
            val bitmap = filmstrip.frame(time, widthPx, heightPx) ?: return@forEachIndexed
            if (index < slots.size) slots[index] = bitmap.asImageBitmap()
        }
    }

    return slots
}
