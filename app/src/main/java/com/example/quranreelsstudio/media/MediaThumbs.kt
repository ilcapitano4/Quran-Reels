package com.example.quranreelsstudio.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.quranreelsstudio.data.BackgroundMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Small thumbnail loader for library rows: grabs a video frame via
 * MediaMetadataRetriever or a downsampled photo decode, cached in memory.
 */
object MediaThumbs {

    private val cache = HashMap<String, Bitmap?>()

    suspend fun get(context: Context, item: BackgroundMediaItem): Bitmap? =
        withContext(Dispatchers.IO) {
            val key = item.assetPath ?: item.contentUri ?: item.remoteUrl ?: return@withContext null
            synchronized(cache) { cache[key] }?.let { return@withContext it }

            val raw: Bitmap? = try {
                if (item.isVideo) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        var prepared = false
                        if (item.assetPath != null) {
                            context.assets.openFd(item.assetPath).use { afd ->
                                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            }
                            prepared = true
                        } else if (item.contentUri != null) {
                            retriever.setDataSource(context, Uri.parse(item.contentUri))
                            prepared = true
                        }
                        if (prepared) {
                            // Ask the decoder for a tiny thumbnail directly — decoding a
                            // full 720p frame per row was the main cause of scroll lag.
                            if (android.os.Build.VERSION.SDK_INT >= 27) {
                                retriever.getScaledFrameAtTime(2_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 160, 160)
                            } else {
                                val big = retriever.getFrameAtTime(2_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                big?.let { b ->
                                    Bitmap.createScaledBitmap(b, 160, (160.0 * b.height / b.width).toInt().coerceAtLeast(1), true)
                                }
                            }
                        } else null
                    } finally {
                        retriever.release()
                    }
                } else {
                    MediaLoader.loadThumbBitmap(context, item)
                }
            } catch (e: Exception) {
                null
            }

            synchronized(cache) { cache[key] = raw }
            raw
        }

    fun clear() = synchronized(cache) { cache.clear() }
}
