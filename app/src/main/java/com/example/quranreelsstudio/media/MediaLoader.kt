package com.example.quranreelsstudio.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.quranreelsstudio.data.BackgroundMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Decodes background media into bitmaps from any supported source:
 * bundled assets, device content URIs, local files, or remote URLs.
 */
object MediaLoader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun loadBitmap(context: Context, item: BackgroundMediaItem): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                when {
                    item.assetPath != null ->
                        context.assets.open(item.assetPath).use { BitmapFactory.decodeStream(it) }

                    item.contentUri != null ->
                        context.contentResolver.openInputStream(Uri.parse(item.contentUri))
                            ?.use { BitmapFactory.decodeStream(it) }

                    item.remoteUrl != null -> {
                        val request = Request.Builder().url(item.remoteUrl).build()
                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) null
                            else response.body?.bytes()?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                        }
                    }

                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Small downsampled decode (1/4 resolution) for library-row thumbnails.
     */
    suspend fun loadThumbBitmap(context: Context, item: BackgroundMediaItem): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                openStream(context, item)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                openStream(context, item)?.use { BitmapFactory.decodeStream(it, null, options) }
            } catch (e: Exception) {
                null
            }
        }

    private fun openStream(context: Context, item: BackgroundMediaItem) = when {
        item.assetPath != null -> context.assets.open(item.assetPath)
        item.contentUri != null -> context.contentResolver.openInputStream(Uri.parse(item.contentUri))
        item.remoteUrl != null -> {
            val request = Request.Builder().url(item.remoteUrl).build()
            client.newCall(request).execute().body?.byteStream()
        }
        else -> null
    }

    /**
     * Copies a device-picked media file into app storage so it stays readable
     * for the preview session and for the future export stage.
     * Returns the absolute file path, or null when the copy fails.
     */
    suspend fun importMedia(context: Context, uri: Uri, isVideo: Boolean): String? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, if (isVideo) "imported_videos" else "imported_photos")
                dir.mkdirs()
                val extension = when {
                    isVideo -> "mp4"
                    else -> "jpg"
                }
                val outFile = File(dir, "import_${System.currentTimeMillis()}.$extension")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext null
                outFile.absolutePath
            } catch (e: Exception) {
                null
            }
        }
}
