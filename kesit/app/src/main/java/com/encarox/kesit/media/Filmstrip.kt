package com.encarox.kesit.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Zaman cizgisindeki kucuk kare onizlemelerini uretir.
 *
 * Hiz icin iki numara: (1) her zaman en yakin anahtar kareyi isteriz, cozucu
 * boylece GOP'u bastan cozmek zorunda kalmaz; (2) kucuk boyutta olceklenmis
 * bitmap isteriz, tam cozunurlukte kare uretip sonra kucultmeyiz.
 */
class Filmstrip(context: Context, uri: Uri) {

    private val retriever = MediaMetadataRetriever().apply { setDataSource(context, uri) }
    private val lock = Mutex()
    private val cache = object : LruCache<Long, Bitmap>(96) {
        override fun sizeOf(key: Long, value: Bitmap) = 1
    }

    @Volatile
    private var released = false

    fun cached(timeMs: Long): Bitmap? = cache.get(timeMs)

    suspend fun frame(timeMs: Long, targetWidth: Int, targetHeight: Int): Bitmap? {
        cache.get(timeMs)?.let { return it }
        if (released) return null
        val bitmap = withContext(Dispatchers.IO) {
            lock.withLock {
                if (released) return@withLock null
                runCatching { grab(timeMs, targetWidth, targetHeight) }.getOrNull()
            }
        }
        return bitmap?.also { cache.put(timeMs, it) }
    }

    private fun grab(timeMs: Long, targetWidth: Int, targetHeight: Int): Bitmap? {
        val us = timeMs * 1000L
        val option = MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(us, option, targetWidth, targetHeight)
        } else {
            retriever.getFrameAtTime(us, option)?.let { full ->
                Bitmap.createScaledBitmap(full, targetWidth, targetHeight, true)
                    .also { if (it !== full) full.recycle() }
            }
        }
    }

    fun release() {
        released = true
        cache.evictAll()
        runCatching { retriever.release() }
    }
}
