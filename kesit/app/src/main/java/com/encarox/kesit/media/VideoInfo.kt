package com.encarox.kesit.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlin.math.roundToLong

/** Kesilecek videonun teknik ozeti. Editorun tamami bu bilgiye gore olculur. */
data class VideoInfo(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val frameRate: Float,
    val bitRate: Long,
    val videoMime: String?,
    val audioMime: String?,
    val sizeBytes: Long,
) {
    val hasAudio: Boolean get() = audioMime != null

    /** Bir karenin milisaniye cinsinden suresi. "Kare kare" oynatmanin temeli. */
    val frameDurationMs: Double
        get() = if (frameRate > 1f) 1000.0 / frameRate else 1000.0 / 30.0

    val frameDurationMsRounded: Long get() = frameDurationMs.roundToLong().coerceAtLeast(1L)

    /** Ekranda gorunen en/boy (dondurulmus videolarda genislik ve yukseklik yer degistirir). */
    val displayWidth: Int get() = if (rotationDegrees % 180 == 90) height else width
    val displayHeight: Int get() = if (rotationDegrees % 180 == 90) width else height

    val aspectRatio: Float
        get() = if (displayHeight > 0) displayWidth.toFloat() / displayHeight else 16f / 9f

    /** Kesilen parcanin yaklasik dosya boyutu (kayipsiz kopyalamada gercege cok yakin). */
    fun estimatedBytesFor(durationMs: Long): Long =
        if (bitRate > 0) (bitRate / 8.0 * (durationMs / 1000.0)).toLong() else 0L

    companion object {
        /**
         * Videoyu tek seferde tanir. MediaMetadataRetriever sure/dondurme icin,
         * MediaExtractor ise kare hizi ve kodek bilgisi icin kullanilir.
         */
        fun probe(context: Context, uri: Uri): VideoInfo {
            var displayName = "video"
            var sizeBytes = 0L
            runCatching {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null, null, null,
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0 && !c.isNull(nameIdx)) displayName = c.getString(nameIdx)
                        if (sizeIdx >= 0 && !c.isNull(sizeIdx)) sizeBytes = c.getLong(sizeIdx)
                    }
                }
            }

            var durationMs = 0L
            var width = 0
            var height = 0
            var rotation = 0
            var bitRate = 0L
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                durationMs = retriever.meta(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                width = retriever.meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                height = retriever.meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                rotation = retriever.meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                bitRate = retriever.meta(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            } finally {
                runCatching { retriever.release() }
            }

            var frameRate = 0f
            var videoMime: String? = null
            var audioMime: String? = null
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, uri, null)
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    when {
                        mime.startsWith("video/") && videoMime == null -> {
                            videoMime = mime
                            frameRate = format.optFrameRate()
                            if (width == 0) width = format.optInt(MediaFormat.KEY_WIDTH)
                            if (height == 0) height = format.optInt(MediaFormat.KEY_HEIGHT)
                            if (durationMs == 0L) {
                                durationMs = format.optLong(MediaFormat.KEY_DURATION) / 1000L
                            }
                        }
                        mime.startsWith("audio/") && audioMime == null -> audioMime = mime
                    }
                }
            } catch (_: Exception) {
                // Bazi kaynaklar MediaExtractor ile acilamaz; retriever verisiyle devam ederiz.
            } finally {
                runCatching { extractor.release() }
            }

            return VideoInfo(
                uri = uri,
                displayName = displayName,
                durationMs = durationMs.coerceAtLeast(0L),
                width = width,
                height = height,
                rotationDegrees = ((rotation % 360) + 360) % 360,
                frameRate = if (frameRate > 1f) frameRate else 30f,
                bitRate = bitRate,
                videoMime = videoMime,
                audioMime = audioMime,
                sizeBytes = sizeBytes,
            )
        }

        private fun MediaMetadataRetriever.meta(key: Int): String? =
            runCatching { extractMetadata(key) }.getOrNull()

        private fun MediaFormat.optInt(key: String): Int =
            runCatching { getInteger(key) }.getOrDefault(0)

        private fun MediaFormat.optLong(key: String): Long =
            runCatching { getLong(key) }.getOrDefault(0L)

        private fun MediaFormat.optFrameRate(): Float {
            runCatching { return getInteger(MediaFormat.KEY_FRAME_RATE).toFloat() }
            runCatching { return getFloat(MediaFormat.KEY_FRAME_RATE) }
            runCatching { return getString(MediaFormat.KEY_FRAME_RATE)?.toFloatOrNull() ?: 0f }
            return 0f
        }
    }
}
