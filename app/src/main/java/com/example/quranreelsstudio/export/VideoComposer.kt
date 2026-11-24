package com.example.quranreelsstudio.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import java.io.File

/** Duration (ms) of the fade between verses in the rendered video. */
private const val VERSE_FADE_MS = 400

/**
 * Renders the final video frames (background + verse text overlays) into a
 * video-only mp4, frame by frame, without OpenGL:
 * - decodes background frames (looping through one or more video files, on a
 *   parallel decoder thread so decoding overlaps with encoding),
 * - draws them center-cropped with the recitation-synced Arabic text,
 * - feeds NV12 bytes into a MediaCodec H.264 encoder (COLOR_FormatYUV420SemiPlanar,
 *   the format universally accepted by hardware encoders).
 * Static backgrounds (plain photo / blank color) convert to NV12 once and
 * reuse the same buffer for every frame — a large speedup.
 */
class VideoComposer(
    private val width: Int,
    private val height: Int,
    private val fps: Int = 30
) {

    /** One decodable background video file. */
    private class SourceVideoReader(file: File) {
        private val extractor = MediaExtractor()
        private val decoder: MediaCodec
        private var extractorDone = false
        private var lastFrame: Bitmap? = null

        init {
            extractor.setDataSource(file.absolutePath)
            var format: MediaFormat? = null
            var index = -1
            for (t in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(t)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    index = t
                    format = f
                    extractor.selectTrack(t)
                    break
                }
            }
            if (index < 0) error("No video track in ${file.name}")
            decoder = MediaCodec.createDecoderByType(format!!.getString(MediaFormat.KEY_MIME)!!)
            decoder.configure(format, null, null, 0)
            decoder.start()
        }

        /** Returns the next frame, or null when the file ends. */
        fun next(): Bitmap? {
            val info = MediaCodec.BufferInfo()
            var guard = 0
            while (guard++ < 200) {
                if (!extractorDone) {
                    val inIndex = decoder.dequeueInputBuffer(10_000L)
                    if (inIndex >= 0) {
                        val buffer = decoder.getInputBuffer(inIndex)!!
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            extractorDone = true
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = decoder.dequeueOutputBuffer(info, if (extractorDone) 1_000L else 10_000L)
                if (outIndex >= 0) {
                    val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    val image = decoder.getOutputImage(outIndex)
                    if (image != null) {
                        val bitmap = try {
                            Yuv.imageToBitmap(image)
                        } finally {
                            decoder.releaseOutputBuffer(outIndex, false)
                        }
                        if (bitmap != null && !eos) {
                            lastFrame = bitmap
                            return bitmap
                        }
                    } else {
                        decoder.releaseOutputBuffer(outIndex, false)
                    }
                    if (eos) return null
                } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // Clip exhausted — bail out immediately instead of spinning.
                    if (extractorDone) return null
                }
            }
            return null
        }

        fun lastKnown(): Bitmap? = lastFrame

        fun close() {
            try { decoder.stop() } catch (e: Exception) {}
            try { decoder.release() } catch (e: Exception) {}
            try { extractor.release() } catch (e: Exception) {}
        }
    }

    private class VerseOverlays(
        val arabic: StaticLayout,
        val translation: StaticLayout?,
        val number: Int = 0,
        val paint: TextPaint? = null,
        val basmala: StaticLayout? = null
    )

    fun compose(
        durationUs: Long,
        verseStartUs: LongArray,
        arabicTexts: List<String>,
        translationTexts: List<String>?,
        header: String,
        typeface: Typeface?,
        scrimAlpha: Float,
        audioFile: File? = null,
        subtitleScale: Float = 1f,
        ayahNumbers: List<Int>? = null,
        medallionBitmap: Bitmap? = null,
        showBasmala: Boolean = false,
        videoFiles: List<File>?,
        stillBitmap: Bitmap?,
        stillMotion: Boolean,
        solidColor: Int?,
        outFile: File,
        onProgress: (framesRendered: Int, totalFrames: Int) -> Unit
    ) {
        val totalFrames = ((durationUs * fps) / 1_000_000L).toInt().coerceAtLeast(1)
        val useVideo = videoFiles != null && videoFiles.isNotEmpty()

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            )
            setInteger(MediaFormat.KEY_BIT_RATE, if (width >= 1080 || height >= 1080) 10_000_000 else 6_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false

        // Audio track is muxed straight into the SAME file as the video
        // (no intermediate remux step — the old double-remux produced
        // corrupted files on some devices).
        var audioExtractor: MediaExtractor? = null
        var audioTrackIndex = -1
        if (audioFile != null) {
            try {
                audioExtractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }
                for (t in 0 until audioExtractor.trackCount) {
                    val mime = audioExtractor.getTrackFormat(t).getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = t
                        break
                    }
                }
            } catch (e: Exception) {
                try { audioExtractor?.release() } catch (ignored: Exception) {}
                audioExtractor = null
            }
        }

        val target = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(target)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val scrimPaint = Paint().apply { color = Color.BLACK }
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = height * 0.026f
            this.typeface = typeface
            isFakeBoldText = true
        }
        val headerLayout = buildLayout(header, headerPaint, (width * 0.9f).toInt())
        val verseOverlaysCache = HashMap<Int, VerseOverlays>()
        val fadePaint = Paint()
        val ornamentFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val medallionBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val ornamentTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2B2620.toInt() // dark number, like the mushaf sign
            textSize = height * 0.026f
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        /** Draws the verse block centered: basmala well above (first verse), text, medallion at the END (left). */
        fun drawOverlaysCentered(canvas: Canvas, ov: VerseOverlays) {
            val gap = (height * 0.03f).toInt()
            // Basmala sits clearly separated: triple the normal gap
            val basmalaGap = gap * 3
            val basmalaHeight = ov.basmala?.height?.plus(basmalaGap) ?: 0
            val blockHeight = basmalaHeight + ov.arabic.height + (ov.translation?.height?.let { it + gap } ?: 0)
            val blockTop = (height - blockHeight) / 2f

            ov.basmala?.let { bas ->
                drawCenteredLayout(canvas, bas, blockTop)
            }
            val arabicTop = blockTop + basmalaHeight
            drawCenteredLayout(canvas, ov.arabic, arabicTop)

            ov.translation?.let { tr ->
                drawCenteredLayout(canvas, tr, arabicTop + ov.arabic.height + gap)
            }
        }

        // ---- Serial background decoder (simple, proven path) ----
        var reader: SourceVideoReader? = null
        var sourceIndex = -1
        var lastVideoFrame: Bitmap? = null

        fun openNextSourceFirstFrame(): Bitmap? {
            reader?.close()
            reader = null
            sourceIndex = (sourceIndex + 1) % videoFiles!!.size
            val newReader = SourceVideoReader(videoFiles[sourceIndex])
            reader = newReader
            return newReader.next()
        }

        fun nextVideoFrame(): Bitmap? {
            var frame = if (reader == null) openNextSourceFirstFrame() else reader!!.next()
            if (frame == null) {
                // Clip ended — move to the next one (loops back around).
                frame = openNextSourceFirstFrame()
            }
            return frame
        }

        var encFramesRejected = 0

        val addAudioTrack: () -> Int = {
            if (audioExtractor != null && audioTrackIndex >= 0) {
                muxer.addTrack(audioExtractor!!.getTrackFormat(audioTrackIndex))
            } else -1
        }

        fun copyAudioSamples(muxTrack: Int) {
            val ex = audioExtractor ?: return
            val buf = ByteArray(1 shl 20)
            val aInfo = MediaCodec.BufferInfo()
            ex.selectTrack(audioTrackIndex)
            while (true) {
                val size = ex.readSampleData(java.nio.ByteBuffer.wrap(buf), 0)
                if (size < 0) break
                aInfo.offset = 0
                aInfo.size = size
                aInfo.presentationTimeUs = ex.sampleTime
                aInfo.flags = if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else 0
                muxer.writeSampleData(muxTrack, java.nio.ByteBuffer.wrap(buf, 0, size), aInfo)
                ex.advance()
            }
        }

        // videoMuxTrack lives in compose() scope on purpose: the muxed track
        // index must persist across drain calls (a per-call local reset it to
        // -1 after FORMAT_CHANGED, which silently dropped every later frame —
        // the bug that produced 5-frame videos).
        var videoMuxTrack = -1
        fun drainEnc(info: MediaCodec.BufferInfo, endOfStream: Boolean) {
            while (true) {
                val index = encoder.dequeueOutputBuffer(info, if (endOfStream) 10_000L else 0L)
                when {
                    index == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!endOfStream) return
                    index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        videoMuxTrack = muxer.addTrack(encoder.outputFormat)
                        val audioMuxTrack = addAudioTrack()
                        muxer.start()
                        muxerStarted = true
                        if (audioMuxTrack >= 0) copyAudioSamples(audioMuxTrack)
                    }
                    index >= 0 -> {
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            info.size = 0
                        }
                        if (info.size > 0 && videoMuxTrack >= 0) {
                            val buffer = encoder.getOutputBuffer(index)!!
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            muxer.writeSampleData(videoMuxTrack, buffer, info)
                        }
                        encoder.releaseOutputBuffer(index, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                    }
                }
            }
        }

        try {
            val info = MediaCodec.BufferInfo()
            var frameIndex = 0L
            var currentVerse = -1
            var overlays: VerseOverlays? = null
            var prevOverlays: VerseOverlays? = null
            var fadeStartFrame = 0L

            while (true) {
                val ptsUs = frameIndex * 1_000_000L / fps
                if (ptsUs >= durationUs) break

                val verseIndex = findVerse(verseStartUs, ptsUs, durationUs)
                if (verseIndex != currentVerse) {
                    currentVerse = verseIndex
                    prevOverlays = overlays
                    overlays = verseOverlaysCache.getOrPut(verseIndex) {
                        buildVerseOverlays(
                            verseIndex, arabicTexts, translationTexts, typeface, subtitleScale,
                            ayahNumbers?.getOrElse(verseIndex) { 0 } ?: 0,
                            showBasmala && verseIndex == 0
                        )
                    }
                    fadeStartFrame = frameIndex
                }

                // ---- Background (redrawn fully every frame) ----
                when {
                    useVideo -> {
                        val frame = nextVideoFrame() ?: lastVideoFrame
                        if (frame != null) {
                            lastVideoFrame = frame
                        }
                        canvas.drawColor(Color.BLACK)
                        val toDraw = frame ?: lastVideoFrame
                        if (toDraw != null) drawCover(canvas, toDraw, paint)
                    }
                    stillBitmap != null -> {
                        val t = ptsUs / 1_000_000f
                        canvas.drawColor(Color.BLACK)
                        if (stillMotion) {
                            val scale = 1.15f + 0.10f * Math.sin(2.0 * Math.PI * t / 14.0).toFloat()
                            val pan = (0.02f * width) * Math.sin(2.0 * Math.PI * t / 17.0).toFloat()
                            drawCover(canvas, stillBitmap, paint, scale, pan)
                        } else {
                            drawCover(canvas, stillBitmap, paint)
                        }
                    }
                    solidColor != null -> canvas.drawColor(solidColor)
                    else -> canvas.drawColor(Color.BLACK)
                }

                // ---- Scrim & text ----
                if (scrimAlpha > 0f) {
                    scrimPaint.alpha = (scrimAlpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
                }
                drawCenteredLayout(canvas, headerLayout, height * 0.055f)

                overlays?.let { ov ->
                    // Smooth crossfade between verses over VERSE_FADE_MS.
                    val fadeFrames = fps * VERSE_FADE_MS / 1000
                    val framesIntoFade = (frameIndex - fadeStartFrame).toInt()
                    if (framesIntoFade < fadeFrames && prevOverlays != null) {
                        val t = framesIntoFade.toFloat() / fadeFrames
                        fadePaint.alpha = ((1f - t) * 255).toInt()
                        canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), fadePaint)
                        drawOverlaysCentered(canvas, prevOverlays!!)
                        canvas.restore()
                        fadePaint.alpha = (t * 255).toInt()
                        canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), fadePaint)
                        drawOverlaysCentered(canvas, ov)
                        canvas.restore()
                        fadePaint.alpha = 255
                    } else {
                        drawOverlaysCentered(canvas, ov)
                    }
                }

                // ---- Encode (fresh NV12 from the fully composed frame) ----
                val nv12 = Yuv.bitmapToNv12(target)
                val inIndex = encoder.dequeueInputBuffer(10_000L)
                if (inIndex >= 0) {
                    val encBuffer = encoder.getInputBuffer(inIndex)!!
                    if (encBuffer.capacity() >= nv12.size) {
                        encBuffer.clear()
                        encBuffer.put(nv12)
                        encoder.queueInputBuffer(inIndex, 0, nv12.size, ptsUs, 0)
                    } else {
                        encFramesRejected++
                    }
                }
                drainEnc(info, false)

                frameIndex++
                if (frameIndex % fps == 0L) onProgress(frameIndex.toInt(), totalFrames)
            }

            // EOS
            val endInfo = MediaCodec.BufferInfo()
            while (true) {
                val inIndex = encoder.dequeueInputBuffer(10_000L)
                if (inIndex >= 0) {
                    encoder.queueInputBuffer(inIndex, 0, 0, frameIndex * 1_000_000L / fps, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    break
                }
                drainEnc(endInfo, false)
            }
            drainEnc(info, true)
            onProgress(totalFrames, totalFrames)

            if (!muxerStarted || encFramesRejected > 0) {
                error(
                    "Device encoder rejected frame input (muxerStarted=$muxerStarted, rejected=$encFramesRejected)"
                )
            }

            // Finalize explicitly so any muxer failure surfaces as a render error
            // instead of silently producing a broken file.
            try { encoder.stop() } catch (e: Exception) {}
            try { encoder.release() } catch (e: Exception) {}
            muxer.stop()
            muxer.release()
            verifyOutput(outFile, durationUs, audioFile != null)
        } finally {
            reader?.close()
            reader = null
            try { audioExtractor?.release() } catch (e: Exception) {}
            try { encoder.stop() } catch (e: Exception) {}
            try { encoder.release() } catch (e: Exception) {}
            try { muxer.stop() } catch (e: Exception) {}
            try { muxer.release() } catch (e: Exception) {}
        }
    }

    /** Re-opens the finished file and asserts it is a playable, complete mp4. */
    private fun verifyOutput(outFile: File, expectedDurationUs: Long, expectAudio: Boolean) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(outFile.absolutePath)
            var hasVideo = false
            var hasAudio = false
            var videoDurationUs = 0L
            var audioDurationUs = 0L
            for (t in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(t)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                val trackDuration = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                    format.getLong(MediaFormat.KEY_DURATION)
                } else 0L
                if (mime.startsWith("video/")) {
                    hasVideo = true
                    videoDurationUs = trackDuration
                }
                if (mime.startsWith("audio/")) {
                    hasAudio = true
                    audioDurationUs = trackDuration
                }
            }
            if (!hasVideo) error("Output verification failed: the file has no video track")
            if (expectAudio && !hasAudio) error("Output verification failed: the file has no audio track")
            // The video track itself must span the recitation — this catches
            // silently-dropped frames (the 5-frame/0.2s-video bug).
            if (videoDurationUs in 1 until expectedDurationUs / 10) {
                error(
                    "Output verification failed: video track too short " +
                        "(${videoDurationUs / 1000}ms of ${expectedDurationUs / 1000}ms)"
                )
            }
            if (audioDurationUs in 1 until expectedDurationUs / 10) {
                error("Output verification failed: audio track too short")
            }
        } finally {
            extractor.release()
        }
    }

    /** Horizontally + vertically centered draw of the verse block. */
    private fun drawCenteredLayout(canvas: Canvas, layout: StaticLayout, y: Float) {
        canvas.save()
        canvas.translate((width - layout.width) / 2f, y)
        layout.draw(canvas)
        canvas.restore()
    }

    /** Scales the bitmap to fit (contain) within a fraction of the canvas, centered. */
    private fun drawContained(canvas: Canvas, bitmap: Bitmap, paint: Paint, fraction: Float) {
        val scale = minOf(
            canvas.width * fraction / bitmap.width,
            canvas.height * fraction / bitmap.height
        )
        val dx = (canvas.width - bitmap.width * scale) / 2f
        val dy = (canvas.height - bitmap.height * scale) / 2f
        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.restore()
    }

    private fun toArabicDigits(n: Int): String = buildString {
        for (c in n.toString()) {
            append(
                when (c) {
                    '0' -> '٠'; '1' -> '١'; '2' -> '٢'; '3' -> '٣'; '4' -> '٤'
                    '5' -> '٥'; '6' -> '٦'; '7' -> '٧'; '8' -> '٨'; '9' -> '٩'
                    else -> c
                }
            )
        }
    }

    private fun findVerse(verseStartUs: LongArray, ptsUs: Long, durationUs: Long): Int {
        if (verseStartUs.isEmpty()) return 0
        var idx = 0
        for (i in verseStartUs.indices) {
            if (verseStartUs[i] <= ptsUs) idx = i else break
        }
        return idx
    }

    private fun buildLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(100))
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(true)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .build()

    private fun buildVerseOverlays(
        verseIndex: Int,
        arabicTexts: List<String>,
        translationTexts: List<String>?,
        typeface: Typeface?,
        subtitleScale: Float,
        ayahNumber: Int = 0,
        withBasmala: Boolean = false
    ): VerseOverlays {
        val textWidth = (width * 0.86f).toInt()
        val arabicPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = height * 0.042f
            this.typeface = typeface
        }
        val arabic = buildLayout(
            arabicTexts.getOrElse(verseIndex) { "" },
            arabicPaint,
            textWidth
        )
        val translation = translationTexts?.getOrNull(verseIndex)?.takeIf { it.isNotBlank() }?.let {
            val trPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFDFF2FC.toInt()
                textSize = height * 0.023f * subtitleScale
                this.typeface = typeface
            }
            buildLayout(it, trPaint, textWidth)
        }
        // Basmala shown above the FIRST verse of a surah (mushaf convention)
        val basmala = if (withBasmala) {
            val basmalaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = height * 0.034f
                this.typeface = typeface
            }
            buildLayout("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", basmalaPaint, (width * 0.8f).toInt())
        } else null
        return VerseOverlays(arabic, translation, ayahNumber, arabicPaint, basmala)
    }

    private fun drawCover(canvas: Canvas, bitmap: Bitmap, paint: Paint, extraScale: Float = 1f, extraPanX: Float = 0f) {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        val scale = maxOf(canvas.width / bw, canvas.height / bh) * extraScale
        val dx = (canvas.width - bw * scale) / 2f + extraPanX
        val dy = (canvas.height - bh * scale) / 2f
        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.restore()
    }

    private fun StaticLayout.draw(canvas: Canvas, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        draw(canvas)
        canvas.restore()
    }
}
