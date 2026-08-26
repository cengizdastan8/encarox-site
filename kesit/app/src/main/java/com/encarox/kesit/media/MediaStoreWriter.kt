package com.encarox.kesit.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Kesilen parcayi galeride gorunecek sekilde Movies/Kesit klasorune yazar.
 * Kaynak dosyaya asla dokunulmaz; her kayit yeni bir dosyadir.
 */
object MediaStoreWriter {

    const val FOLDER = "Kesit"

    data class Saved(val uri: Uri, val displayName: String, val sizeBytes: Long)

    fun save(context: Context, source: File, displayName: String): Saved {
        val name = ensureMp4(displayName)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveScoped(context, source, name)
        } else {
            saveLegacy(context, source, name)
        }
    }

    private fun saveScoped(context: Context, source: File, name: String): Saved {
        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$FOLDER")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(collection, values)
            ?: error("Galeriye kayit girisi olusturulamadi.")
        try {
            resolver.openOutputStream(uri, "w")?.use { out ->
                source.inputStream().use { input -> input.copyTo(out, DEFAULT_BUFFER_SIZE * 8) }
            } ?: error("Kayit dosyasi acilamadi.")
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }

        resolver.update(uri, ContentValues().apply {
            put(MediaStore.Video.Media.IS_PENDING, 0)
        }, null, null)

        return Saved(uri, name, source.length())
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(context: Context, source: File, name: String): Saved {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), FOLDER)
        if (!dir.exists() && !dir.mkdirs()) error("Movies/$FOLDER klasoru olusturulamadi.")
        val target = File(dir, name)
        source.copyTo(target, overwrite = true)

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATA, target.absolutePath)
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        }
        val uri = context.contentResolver
            .insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: Uri.fromFile(target)
        return Saved(uri, name, target.length())
    }

    private fun ensureMp4(name: String): String =
        if (name.endsWith(".mp4", ignoreCase = true)) name else "$name.mp4"
}
