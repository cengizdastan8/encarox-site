package com.encarox.kesit.media

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TrimMode {
    /** Kare hassasiyetinde. Yalnizca ilk GOP yeniden kodlanir. */
    PRECISE,

    /** Hicbir kare yeniden kodlanmaz. Baslangic anahtar kareye yuvarlanir. */
    LOSSLESS,
}

data class TrimResult(
    val uri: Uri,
    val displayName: String,
    val mode: TrimMode,
    val requestedStartMs: Long,
    val actualStartMs: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val elapsedMs: Long,
    val note: String?,
) {
    /** Kayipsiz modda baslangicin ne kadar geriye kaydigi. */
    val startShiftMs: Long get() = (requestedStartMs - actualStartMs).coerceAtLeast(0L)
}

/**
 * Kesme islemini bastan sona yurutur: gecici dosyaya uret, sonra galeriye tasi.
 * Kaynak video hicbir asamada degistirilmez veya silinmez.
 */
object TrimEngine {

    suspend fun run(
        context: Context,
        info: VideoInfo,
        startMs: Long,
        endMs: Long,
        mode: TrimMode,
        onProgress: (Float) -> Unit,
    ): TrimResult {
        require(endMs > startMs) { "Bitis noktasi baslangictan sonra olmali." }

        val startedAt = System.currentTimeMillis()
        val workDir = File(context.cacheDir, "kesit-work").apply { mkdirs() }
        val temp = File(workDir, "cut-${System.nanoTime()}.mp4")

        try {
            var actualStartMs = startMs
            var effectiveMode = mode
            var note: String? = null

            when (mode) {
                TrimMode.PRECISE -> {
                    try {
                        val outcome = PreciseTrimmer.trim(
                            context = context,
                            uri = info.uri,
                            startMs = startMs,
                            endMs = endMs,
                            outPath = temp.absolutePath,
                            onProgress = onProgress,
                        )
                        note = outcome.note
                    } catch (t: Throwable) {
                        if (t is kotlinx.coroutines.CancellationException) throw t
                        // Bazi kodek/kap birlesimlerinde Transformer calismaz.
                        // Kullaniciyi bos elle birakmak yerine kayipsiz moda gecip
                        // ne oldugunu acikca soyluyoruz.
                        runCatching { temp.delete() }
                        val fallback = runLossless(context, info, startMs, endMs, temp, onProgress)
                        actualStartMs = fallback
                        effectiveMode = TrimMode.LOSSLESS
                        note = "Bu video hassas modu desteklemedi, kayipsiz moda gecildi."
                    }
                }

                TrimMode.LOSSLESS -> {
                    actualStartMs = runLossless(context, info, startMs, endMs, temp, onProgress)
                }
            }

            check(temp.exists() && temp.length() > 0) { "Cikti dosyasi olusturulamadi." }

            val name = buildName(info.displayName, startMs, endMs)
            val saved = withContext(Dispatchers.IO) {
                MediaStoreWriter.save(context, temp, name)
            }

            return TrimResult(
                uri = saved.uri,
                displayName = saved.displayName,
                mode = effectiveMode,
                requestedStartMs = startMs,
                actualStartMs = actualStartMs,
                durationMs = endMs - actualStartMs,
                sizeBytes = saved.sizeBytes,
                elapsedMs = System.currentTimeMillis() - startedAt,
                note = note,
            )
        } finally {
            runCatching { temp.delete() }
        }
    }

    private suspend fun runLossless(
        context: Context,
        info: VideoInfo,
        startMs: Long,
        endMs: Long,
        temp: File,
        onProgress: (Float) -> Unit,
    ): Long = withContext(Dispatchers.IO) {
        val outcome = LosslessTrimmer.trim(
            context = context,
            uri = info.uri,
            startUs = startMs * 1000L,
            endUs = endMs * 1000L,
            rotationDegrees = info.rotationDegrees,
            outFile = temp,
            onProgress = onProgress,
        )
        outcome.actualStartUs / 1000L
    }

    private fun buildName(sourceName: String, startMs: Long, endMs: Long): String {
        val base = sourceName.substringBeforeLast('.', sourceName)
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(48)
            .ifBlank { "video" }
        val stamp = SimpleDateFormat("HHmmss", Locale.US).format(Date())
        return "${base}_kesit_${startMs / 1000}-${endMs / 1000}sn_$stamp.mp4"
    }
}
