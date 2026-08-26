package com.encarox.kesit.media

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Kare hassasiyetinde kesme. Media3 Transformer'in "trim optimization" ozelligi
 * acik: yalnizca istenen baslangic ile ondan sonraki ilk anahtar kare arasindaki
 * GOP yeniden kodlanir, videonun geri kalani oldugu gibi kopyalanir.
 *
 * Sonuc: baslangic tam istedigin yerde, kalite kaybi neredeyse sifir, sure ise
 * bastan sona yeniden kodlamaya kiyasla kat kat kisa.
 */
@OptIn(markerClass = [UnstableApi::class])
object PreciseTrimmer {

    data class Outcome(
        /** Kullaniciya gosterilecek kisa aciklama; soylenecek bir sey yoksa null. */
        val note: String?,
        val durationMs: Long,
        val fileSizeBytes: Long,
    )

    suspend fun trim(
        context: Context,
        uri: Uri,
        startMs: Long,
        endMs: Long,
        outPath: String,
        onProgress: (Float) -> Unit = {},
    ): Outcome = withContext(Dispatchers.Main) {
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
            .build()

        // Hicbir efekt/olcekleme eklemiyoruz: kodek ve cozunurluk kaynakla ayni
        // kalsin ki Transformer akisin buyuk bolumunu kopyalayabilsin.
        val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()

        var transformer: Transformer? = null
        try {
            val result = coroutineScope {
                val poller = launch {
                    val holder = ProgressHolder()
                    while (isActive) {
                        val active = transformer
                        if (active != null &&
                            active.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE
                        ) {
                            onProgress((holder.progress / 100f).coerceIn(0f, 1f))
                        }
                        delay(180)
                    }
                }
                try {
                    suspendCancellableCoroutine { cont ->
                        val built = Transformer.Builder(context)
                            .experimentalSetTrimOptimizationEnabled(true)
                            .addListener(object : Transformer.Listener {
                                override fun onCompleted(
                                    composition: Composition,
                                    exportResult: ExportResult,
                                ) {
                                    if (cont.isActive) cont.resume(exportResult)
                                }

                                override fun onError(
                                    composition: Composition,
                                    exportResult: ExportResult,
                                    exportException: ExportException,
                                ) {
                                    if (cont.isActive) cont.resumeWithException(exportException)
                                }
                            })
                            .build()

                        transformer = built
                        cont.invokeOnCancellation { runCatching { built.cancel() } }
                        built.start(editedMediaItem, outPath)
                    }
                } finally {
                    poller.cancel()
                }
            }

            onProgress(1f)
            Outcome(
                note = describeOptimization(runCatching { result.optimizationResult }.getOrNull()),
                durationMs = result.durationMs.coerceAtLeast(0L),
                fileSizeBytes = result.fileSizeBytes.coerceAtLeast(0L),
            )
        } finally {
            runCatching { transformer?.cancel() }
        }
    }

    /**
     * Yalnizca olumlu durumu bildiriyoruz. Optimizasyonun uygulanmadigi
     * durumlarin bir kismi (kesme noktasi zaten anahtar kareye denk gelmisse)
     * aslinda tam kopyalamadir; "tam kodlama yapildi" demek yanlis olurdu.
     */
    private fun describeOptimization(result: Int?): String? = when (result) {
        ExportResult.OPTIMIZATION_SUCCEEDED ->
            "Hizli kesme uygulandi: yalnizca bastaki kare grubu yeniden kodlandi."
        else -> null
    }
}
