package com.encarox.kesit.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * %100 kayipsiz kesme: hicbir kare yeniden kodlanmaz, sikistirilmis ornekler
 * kaynak dosyadan okunup dogrudan yeni bir MP4 kabina yazilir.
 *
 * Bedeli: video ancak bir anahtar kareden (keyframe) baslatilabilir. Bu yuzden
 * gercek baslangic, istenen noktadan biraz geride olabilir; ne kadar kaydigini
 * [Outcome.actualStartUs] ile geri bildiriyoruz.
 */
object LosslessTrimmer {

    data class Outcome(
        val actualStartUs: Long,
        val actualEndUs: Long,
        val videoCopied: Boolean,
        val audioCopied: Boolean,
    )

    private const val MIN_BUFFER = 1 shl 21 // 2 MB, 4K anahtar kareleri icin yeterli
    private const val MAX_BUFFER = 1 shl 25 // 32 MB tavan

    fun trim(
        context: Context,
        uri: Uri,
        startUs: Long,
        endUs: Long,
        rotationDegrees: Int,
        outFile: File,
        isCancelled: () -> Boolean = { false },
        onProgress: (Float) -> Unit = {},
    ): Outcome {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var started = false
        try {
            extractor.setDataSource(context, uri, null)

            var videoTrack = -1
            var audioTrack = -1
            for (i in 0 until extractor.trackCount) {
                val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
                if (videoTrack < 0 && mime.startsWith("video/")) videoTrack = i
                if (audioTrack < 0 && mime.startsWith("audio/")) audioTrack = i
            }
            require(videoTrack >= 0) { "Dosyada video izi bulunamadi." }

            // 1) Istenen baslangicin gerisindeki en yakin anahtar kareyi bul.
            extractor.selectTrack(videoTrack)
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val actualStartUs = extractor.sampleTime.let { if (it < 0) 0L else it }

            // 2) Sesi de ayni noktadan hizala; boylece ses-goruntu senkronu bozulmaz.
            if (audioTrack >= 0) extractor.selectTrack(audioTrack)
            extractor.seekTo(actualStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer.setOrientationHint(rotationDegrees)

            var maxInput = MIN_BUFFER
            val trackMap = HashMap<Int, Int>()
            for (source in listOfNotNull(videoTrack, audioTrack.takeIf { it >= 0 })) {
                val format = extractor.getTrackFormat(source)
                runCatching {
                    maxInput = max(maxInput, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                }
                trackMap[source] = muxer.addTrack(format)
            }

            muxer.start()
            started = true

            var buffer = ByteBuffer.allocateDirect(maxInput.coerceAtMost(MAX_BUFFER))
            val bufferInfo = MediaCodec.BufferInfo()
            val span = (endUs - actualStartUs).coerceAtLeast(1L)
            var wroteVideo = false
            var wroteAudio = false
            var lastReported = -1f
            // Bir iz bitis noktasini gectiginde onu "tamamlandi" isaretleriz.
            // unselectTrack cagirmak yerine ornekleri atlamak, kalan izin
            // okuma konumunu bozmadigi icin daha guvenli.
            val finished = HashSet<Int>()

            while (true) {
                if (isCancelled()) throw InterruptedException("Islem iptal edildi.")
                if (finished.size >= trackMap.size) break

                val sourceTrack = extractor.sampleTrackIndex
                if (sourceTrack < 0) break
                val destTrack = trackMap[sourceTrack]
                if (destTrack == null || sourceTrack in finished) {
                    if (!extractor.advance()) break
                    continue
                }

                val sampleTime = extractor.sampleTime
                if (sampleTime < 0) break
                if (sampleTime > endUs) {
                    finished += sourceTrack
                    if (finished.size >= trackMap.size) break
                    if (!extractor.advance()) break
                    continue
                }

                var size = readSample(extractor, buffer)
                if (size == BUFFER_TOO_SMALL) {
                    val grown = (buffer.capacity() * 2).coerceAtMost(MAX_BUFFER)
                    check(grown > buffer.capacity()) { "Kare arabellek sinirini asti." }
                    buffer = ByteBuffer.allocateDirect(grown)
                    size = readSample(extractor, buffer)
                }
                if (size <= 0) break

                bufferInfo.offset = 0
                bufferInfo.size = size
                bufferInfo.presentationTimeUs = (sampleTime - actualStartUs).coerceAtLeast(0L)
                bufferInfo.flags = extractor.sampleFlagsToBufferFlags()

                muxer.writeSampleData(destTrack, buffer, bufferInfo)
                if (sourceTrack == videoTrack) wroteVideo = true else wroteAudio = true

                val progress = ((sampleTime - actualStartUs).toFloat() / span).coerceIn(0f, 1f)
                if (progress - lastReported > 0.01f) {
                    lastReported = progress
                    onProgress(progress)
                }

                if (!extractor.advance()) break
            }

            check(wroteVideo) { "Secilen araliktan hic video karesi okunamadi." }
            onProgress(1f)
            return Outcome(actualStartUs, endUs, wroteVideo, wroteAudio)
        } finally {
            if (started) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
        }
    }

    private const val BUFFER_TOO_SMALL = -2

    private fun readSample(extractor: MediaExtractor, buffer: ByteBuffer): Int = try {
        buffer.clear()
        extractor.readSampleData(buffer, 0)
    } catch (_: IllegalArgumentException) {
        BUFFER_TOO_SMALL
    }

    /** MediaExtractor bayraklarini MediaCodec.BufferInfo bayraklarina cevirir. */
    private fun MediaExtractor.sampleFlagsToBufferFlags(): Int {
        var flags = 0
        if (sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
            flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
        return flags
    }
}
