package com.example.quranreelsstudio.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.example.quranreelsstudio.data.BackgroundMediaItem
import com.example.quranreelsstudio.data.BackgroundType
import com.example.quranreelsstudio.data.ReelAspectRatio
import com.example.quranreelsstudio.data.Reciter
import com.example.quranreelsstudio.data.Surah
import com.example.quranreelsstudio.media.MediaLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RenderSpec(
    val qualityP: Int, // 720 or 1080
    val aspect: ReelAspectRatio,
    val surah: Surah,
    val fromAyah: Int,
    val toAyah: Int,
    val reciter: Reciter,
    val backgroundType: BackgroundType,
    val videoItems: List<BackgroundMediaItem>, // for MOTION_VIDEO (single or sequence)
    val photoItem: BackgroundMediaItem?,       // for NATURAL_PHOTO (incl. AI motion clips)
    val blankColor: Long,
    val visibility: Int,
    val arabicTexts: List<String>,
    val translationTexts: List<String>?,
    val typeface: Typeface?,
    val subtitleSize: Int = 12
)

/**
 * Full export pipeline:
 * 1. download the per-verse recitation mp3s from everyayah.com
 * 2. transcode them into one AAC track with per-verse start times
 * 3. render background + verse text overlays into a video-only mp4
 * 4. mux video + audio into the final .mp4
 */
class VideoRenderEngine(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun render(spec: RenderSpec, onProgress: (String, Int) -> Unit): Result<String> =
        withContext(Dispatchers.Default) {
            runCatching {
                val rendersDir = File(context.filesDir, "renders").apply { mkdirs() }
                val finalFile = File(rendersDir, "quran_reel_${System.currentTimeMillis()}.mp4")
                val tmpDir = File(context.cacheDir, "render_tmp_${System.currentTimeMillis()}").apply { mkdirs() }

                try {
                    // ---- 1. Download recitation ----
                    val ayahRange = spec.fromAyah..spec.toAyah
                    val audioFiles = ayahRange.mapIndexed { i, ayah ->
                        val url = "https://everyayah.com/data/${spec.reciter.audioFolder}/" +
                            String.format(Locale.US, "%03d%03d.mp3", spec.surah.number, ayah)
                        val file = File(tmpDir, "verse_${ayah}.mp3")
                        if (!file.exists() || file.length() < 1_000) {
                            val request = Request.Builder().url(url).build()
                            client.newCall(request).execute().use { response ->
                                if (!response.isSuccessful) error("Failed to download verse $ayah audio (HTTP ${response.code})")
                                response.body!!.byteStream().use { input ->
                                    file.outputStream().use { output -> input.copyTo(output) }
                                }
                            }
                        }
                        onProgress("Downloading recitation ${i + 1}/${ayahRange.count()}", (i + 1) * 38 / ayahRange.count())
                        file
                    }

                    // ---- 2. Transcode to one AAC track ----
                    onProgress("Preparing recitation audio…", 40)
                    val audio = AudioTranscoder.transcode(audioFiles, File(tmpDir, "audio.m4a"))

                    // ---- 3. Render video frames ----
                    val (w, h) = resolutionFor(spec.aspect, spec.qualityP)
                    onProgress("Preparing background…", 52)

                    val videoFiles: List<File>? = if (spec.backgroundType == BackgroundType.MOTION_VIDEO) {
                        spec.videoItems.mapNotNull { item -> resolveVideoFile(item, tmpDir) }
                            .ifEmpty { null }
                    } else null

                    val stillBitmap: Bitmap? = if (spec.backgroundType == BackgroundType.NATURAL_PHOTO && spec.photoItem != null) {
                        MediaLoader.loadBitmap(context, spec.photoItem)
                    } else null

                    val solidColor: Int? = if (spec.backgroundType == BackgroundType.BLANK_COLOR) {
                        spec.blankColor.toInt()
                    } else null

                    val tmpVideo = File(tmpDir, "video_only.mp4")
                    val ayahNumbers = (spec.fromAyah..spec.toAyah).toList()
                    val medallionBitmap = runCatching {
                        android.graphics.BitmapFactory.decodeStream(context.assets.open("sign_medallion.png"))
                    }.getOrNull()
                    VideoComposer(w, h).compose(
                        durationUs = audio.totalUs,
                        verseStartUs = audio.verseStartUs,
                        arabicTexts = spec.arabicTexts,
                        translationTexts = spec.translationTexts,
                        header = "${spec.surah.arabicName} • ${spec.surah.englishName}",
                        typeface = spec.typeface,
                        scrimAlpha = (100 - spec.visibility) / 100f * 0.85f,
                        audioFile = File(audio.outputPath),
                        subtitleScale = spec.subtitleSize / 12f,
                        ayahNumbers = ayahNumbers,
                        medallionBitmap = medallionBitmap,
                        showBasmala = false,
                        videoFiles = videoFiles,
                        stillBitmap = stillBitmap,
                        stillMotion = spec.photoItem?.motion == true,
                        solidColor = solidColor,
                        outFile = finalFile
                    ) { frames, total ->
                        onProgress("Rendering & mixing video…", 55 + (frames * 42 / total.coerceAtLeast(1)))
                    }
                    onProgress("Finalizing…", 98)
                    onProgress("Done", 100)
                    finalFile.absolutePath
                } finally {
                    tmpDir.deleteRecursively()
                }
            }
        }

    private fun resolveVideoFile(item: BackgroundMediaItem, tmpDir: File): File? = when {
        item.assetPath != null -> {
            val outFile = File(tmpDir, "bg_${item.id}_${File(item.assetPath).name}")
            if (!outFile.exists() || outFile.length() < 1_000) {
                context.assets.open(item.assetPath).use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            outFile
        }
        item.contentUri?.startsWith("file://") == true -> File(item.contentUri.removePrefix("file://"))
        else -> null
    }

    private fun resolutionFor(aspect: ReelAspectRatio, qualityP: Int): Pair<Int, Int> {
        val base = if (qualityP >= 1080) 1080 else 720
        return when (aspect) {
            ReelAspectRatio.REEL_9_16 -> base to (base * 16 / 9)
            ReelAspectRatio.POST_1_1 -> base to base
            ReelAspectRatio.WIDE_16_9 -> (base * 16 / 9) to base
        }
    }
}
