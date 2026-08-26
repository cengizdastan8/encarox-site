package com.encarox.kesit.ui

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import com.encarox.kesit.media.Filmstrip
import com.encarox.kesit.media.TrimEngine
import com.encarox.kesit.media.TrimMode
import com.encarox.kesit.media.TrimResult
import com.encarox.kesit.media.VideoInfo
import kotlinx.coroutines.CancellationException
import kotlin.math.roundToLong

sealed interface ExportState {
    data object Idle : ExportState
    data class Working(val progress: Float) : ExportState
    data class Done(val result: TrimResult) : ExportState
    data class Failed(val message: String) : ExportState
}

/**
 * Editorun butun durumu ve oynatici burada. Amac: her etkilesimin aninda
 * gorunmesi. Onizleme icin dosya uretmiyoruz, oynaticiya arali kirpma
 * veriyoruz; bu yuzden "izle" tusu beklemeden calisiyor.
 */
@OptIn(UnstableApi::class)
class EditorController(
    context: Context,
    val info: VideoInfo,
) {
    private val appContext = context.applicationContext

    val filmstrip: Filmstrip? = runCatching { Filmstrip(appContext, info.uri) }.getOrNull()

    val player: ExoPlayer = ExoPlayer.Builder(appContext).build().apply {
        setSeekParameters(SeekParameters.EXACT)
        setMediaItem(MediaItem.fromUri(info.uri))
        prepare()
        playWhenReady = false
    }

    var startMs by mutableLongStateOf(0L)
        private set
    var endMs by mutableLongStateOf(defaultEnd(info.durationMs))
        private set
    var viewStartMs by mutableLongStateOf(0L)
        private set
    var viewEndMs by mutableLongStateOf(info.durationMs.coerceAtLeast(1L))
        private set
    var playheadMs by mutableLongStateOf(0L)
        private set

    var activeHandle by mutableStateOf(DragTarget.START)
        private set
    var dragging by mutableStateOf(false)
        private set
    var previewing by mutableStateOf(false)
        private set
    var playing by mutableStateOf(false)
        private set
    var mode by mutableStateOf(TrimMode.PRECISE)
    var exportState by mutableStateOf<ExportState>(ExportState.Idle)
        private set

    val durationMs: Long get() = info.durationMs.coerceAtLeast(1L)
    val selectionMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
    val zoomFactor: Float get() = durationMs.toFloat() / (viewEndMs - viewStartMs).coerceAtLeast(1L)

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playing = isPlaying
        }
    }

    init {
        player.addListener(listener)
    }

    // --- Oynatma konumu -----------------------------------------------------

    /** Oynatici konumunu arayuze tasir; surukleme sirasinda karismasin diye durur. */
    fun syncPlayhead() {
        if (dragging) return
        val position = player.currentPosition.coerceAtLeast(0L)
        playheadMs = if (previewing) (startMs + position).coerceAtMost(endMs) else position
    }

    fun scrubTo(timeMs: Long) {
        stopPreview(seekBack = false)
        val target = timeMs.coerceIn(0L, durationMs)
        playheadMs = target
        player.seekTo(target)
    }

    fun onDragStateChange(active: Boolean) {
        dragging = active
        // Surukleme sirasinda en yakin anahtar kareye atlamak anlik geri bildirim
        // verir; parmak kalkinca tam kareye oturturuz.
        player.setSeekParameters(if (active) SeekParameters.CLOSEST_SYNC else SeekParameters.EXACT)
        if (!active) player.seekTo(playheadMs)
    }

    // --- Secim --------------------------------------------------------------

    fun onSelectionChange(newStart: Long, newEnd: Long, target: DragTarget) {
        stopPreview(seekBack = false)
        startMs = newStart.coerceIn(0L, durationMs)
        val floor = (startMs + MIN_SELECTION_MS).coerceAtMost(durationMs)
        endMs = newEnd.coerceIn(floor, durationMs)
        when (target) {
            DragTarget.START -> {
                activeHandle = DragTarget.START
                previewFrame(startMs)
            }
            DragTarget.END -> {
                activeHandle = DragTarget.END
                previewFrame(endMs)
            }
            DragTarget.SELECTION -> previewFrame(startMs)
            DragTarget.SCRUB -> Unit
        }
    }

    fun selectHandle(handle: DragTarget) {
        activeHandle = handle
        previewFrame(if (handle == DragTarget.START) startMs else endMs)
    }

    /** Aktif tutamaci verilen kadar oynatir. Kare butonlari bunu kullanir. */
    fun nudge(deltaMs: Long) {
        stopPreview(seekBack = false)
        if (activeHandle == DragTarget.END) {
            val floor = (startMs + MIN_SELECTION_MS).coerceAtMost(durationMs)
            endMs = (endMs + deltaMs).coerceIn(floor, durationMs)
            previewFrame(endMs)
        } else {
            val ceiling = (endMs - MIN_SELECTION_MS).coerceAtLeast(0L)
            startMs = (startMs + deltaMs).coerceIn(0L, ceiling)
            previewFrame(startMs)
        }
        ensureVisible(if (activeHandle == DragTarget.END) endMs else startMs)
    }

    fun setHandleToPlayhead() {
        if (activeHandle == DragTarget.END) {
            endMs = playheadMs.coerceIn((startMs + MIN_SELECTION_MS).coerceAtMost(durationMs), durationMs)
        } else {
            startMs = playheadMs.coerceIn(0L, (endMs - MIN_SELECTION_MS).coerceAtLeast(0L))
        }
    }

    private fun previewFrame(timeMs: Long) {
        playheadMs = timeMs
        player.seekTo(timeMs)
    }

    // --- Yakinlastirma ------------------------------------------------------

    private fun setWindow(start: Long, end: Long) {
        val minSpan = 400L
        var span = (end - start).coerceIn(minSpan, durationMs)
        if (span > durationMs) span = durationMs
        val s = start.coerceIn(0L, (durationMs - span).coerceAtLeast(0L))
        viewStartMs = s
        viewEndMs = (s + span).coerceAtMost(durationMs)
    }

    fun onWindowChange(start: Long, end: Long) = setWindow(start, end)

    fun zoom(factor: Float) {
        val span = (viewEndMs - viewStartMs).coerceAtLeast(1L)
        val anchor = when (activeHandle) {
            DragTarget.END -> endMs
            else -> startMs
        }.coerceIn(viewStartMs, viewEndMs)
        val newSpan = (span / factor).roundToLong().coerceIn(400L, durationMs)
        setWindow(anchor - newSpan / 2, anchor - newSpan / 2 + newSpan)
    }

    fun zoomToSelection() {
        val pad = (selectionMs * 0.3).toLong().coerceAtLeast(150L)
        setWindow(startMs - pad, endMs + pad)
    }

    fun zoomAll() = setWindow(0L, durationMs)

    /** Tutamac pencerenin disina cikarsa pencereyi ona getirir. */
    private fun ensureVisible(timeMs: Long) {
        val span = (viewEndMs - viewStartMs).coerceAtLeast(1L)
        val margin = span / 10
        when {
            timeMs < viewStartMs + margin -> setWindow(timeMs - span / 2, timeMs + span / 2)
            timeMs > viewEndMs - margin -> setWindow(timeMs - span / 2, timeMs + span / 2)
        }
    }

    // --- Onizleme -----------------------------------------------------------

    /**
     * Secilen araligi dongude oynatir. Hicbir dosya uretilmez: oynaticiya
     * kirpma bilgisi verilir, bu yuzden aninda baslar.
     */
    fun togglePreview() {
        if (previewing) {
            stopPreview(seekBack = true)
        } else {
            val item = MediaItem.Builder()
                .setUri(info.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
                .build()
            previewing = true
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.setMediaItem(item)
            player.prepare()
            player.playWhenReady = true
        }
    }

    fun togglePlay() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (!previewing && player.currentPosition >= durationMs - 30) player.seekTo(0)
            player.play()
        }
    }

    private fun stopPreview(seekBack: Boolean) {
        if (!previewing) return
        previewing = false
        val resumeAt = playheadMs
        player.playWhenReady = false
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.setMediaItem(MediaItem.fromUri(info.uri))
        player.prepare()
        player.seekTo(if (seekBack) resumeAt.coerceIn(0L, durationMs) else startMs)
    }

    // --- Disa aktarma -------------------------------------------------------

    suspend fun export() {
        if (exportState is ExportState.Working) return
        stopPreview(seekBack = false)
        player.pause()
        exportState = ExportState.Working(0f)
        try {
            val result = TrimEngine.run(
                context = appContext,
                info = info,
                startMs = startMs,
                endMs = endMs,
                mode = mode,
                onProgress = { exportState = ExportState.Working(it) },
            )
            exportState = ExportState.Done(result)
        } catch (c: CancellationException) {
            exportState = ExportState.Idle
            throw c
        } catch (t: Throwable) {
            exportState = ExportState.Failed(t.message ?: "Bilinmeyen bir hata olustu.")
        }
    }

    fun dismissExport() {
        exportState = ExportState.Idle
    }

    fun release() {
        runCatching { player.removeListener(listener) }
        runCatching { player.release() }
        runCatching { filmstrip?.release() }
    }

    private companion object {
        /** Ilk acilista makul bir secim: en fazla 10 saniye. */
        fun defaultEnd(durationMs: Long): Long =
            durationMs.coerceAtLeast(1L).coerceAtMost(10_000L)
    }
}
