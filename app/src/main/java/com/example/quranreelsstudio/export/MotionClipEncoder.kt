package com.example.quranreelsstudio.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

/**
 * Encodes a still image into a short real MP4 clip (video-only) with a
 * slow cinematic zoom/pan — used by the AI Motion generator so users get
 * an actual playable video file that previews, loops and renders.
 */
object MotionClipEncoder {

    fun encode(
        source: Bitmap,
        outFile: File,
        width: Int = 720,
        height: Int = 1280,
        durationMs: Long = 5000,
        fps: Int = 24
    ): Result<Unit> {
        return try {
            val totalFrames = ((durationMs * fps) / 1000L).toInt().coerceAtLeast(1)

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
                setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var track = -1
            val info = MediaCodec.BufferInfo()

            fun drain(endOfStream: Boolean) {
                while (true) {
                    val index = encoder.dequeueOutputBuffer(info, if (endOfStream) 10_000L else 0L)
                    when {
                        index == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!endOfStream) return
                        index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            track = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                        }
                        index >= 0 -> {
                            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                            if (info.size > 0 && track >= 0) {
                                val buffer = encoder.getOutputBuffer(index)!!
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                muxer.writeSampleData(track, buffer, info)
                            }
                            encoder.releaseOutputBuffer(index, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                        }
                    }
                }
            }

            val target = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(target)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)

            try {
                for (frame in 0 until totalFrames) {
                    val seconds = frame * 1000f / fps / 1000f
                    val scale = 1.12f + 0.10f * Math.sin(2.0 * Math.PI * seconds / 5.0).toFloat()
                    val pan = (0.02f * width) * Math.sin(2.0 * Math.PI * seconds / 6.0).toFloat()

                    canvas.drawColor(Color.BLACK)
                    val bw = source.width.toFloat()
                    val bh = source.height.toFloat()
                    val cover = maxOf(width / bw, height / bh) * scale
                    val dx = (width - bw * cover) / 2f + pan
                    val dy = (height - bh * cover) / 2f
                    canvas.save()
                    canvas.translate(dx, dy)
                    canvas.scale(cover, cover)
                    canvas.drawBitmap(source, 0f, 0f, paint)
                    canvas.restore()

                    val nv12 = Yuv.bitmapToNv12(target)
                    val inIndex = encoder.dequeueInputBuffer(10_000L)
                    if (inIndex >= 0) {
                        val encBuffer = encoder.getInputBuffer(inIndex)!!
                        if (encBuffer.capacity() >= nv12.size) {
                            encBuffer.clear()
                            encBuffer.put(nv12)
                            encoder.queueInputBuffer(inIndex, 0, nv12.size, frame * 1_000_000L / fps, 0)
                        }
                    }
                    drain(false)
                }

                // EOS
                while (true) {
                    val inIndex = encoder.dequeueInputBuffer(10_000L)
                    if (inIndex >= 0) {
                        encoder.queueInputBuffer(inIndex, 0, 0, totalFrames * 1_000_000L / fps, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        break
                    }
                    drain(false)
                }
                drain(true)

                encoder.stop()
                encoder.release()
                muxer.stop()
                muxer.release()
                Result.success(Unit)
            } catch (e: Exception) {
                try { encoder.stop() } catch (ignored: Exception) {}
                try { encoder.release() } catch (ignored: Exception) {}
                try { muxer.stop() } catch (ignored: Exception) {}
                try { muxer.release() } catch (ignored: Exception) {}
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
