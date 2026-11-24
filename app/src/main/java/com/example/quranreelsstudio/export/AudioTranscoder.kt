package com.example.quranreelsstudio.export

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

/**
 * Concatenates per-verse mp3 recitations into one AAC .m4a track,
 * recording the exact start time (µs) of every verse so the video
 * composer can switch verse text on the beat of the recitation.
 */
object AudioTranscoder {

    data class Result(val outputPath: String, val verseStartUs: LongArray, val totalUs: Long)

    fun transcode(audioFiles: List<File>, outFile: File): Result {
        if (audioFiles.isEmpty()) error("No audio files to transcode")

        // Read format from the first file
        var sampleRate = 0
        var channels = 0
        val firstExtractor = MediaExtractor()
        try {
            firstExtractor.setDataSource(audioFiles[0].absolutePath)
            for (t in 0 until firstExtractor.trackCount) {
                val format = firstExtractor.getTrackFormat(t)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    break
                }
            }
        } finally {
            firstExtractor.release()
        }
        if (sampleRate == 0) error("Invalid audio files")

        val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val encoderFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65_536)
        }
        encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        var audioTrack = -1
        var ptsUs = 0L
        val verseStartUs = LongArray(audioFiles.size)
        val encInfo = MediaCodec.BufferInfo()

        fun drainEncoder(waitForever: Boolean) {
            while (true) {
                val index = encoder.dequeueOutputBuffer(encInfo, if (waitForever) 10_000L else 0L)
                when {
                    index == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!waitForever) return
                    index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        audioTrack = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                    }
                    index >= 0 -> {
                        if (encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            encInfo.size = 0
                        }
                        if (encInfo.size > 0 && audioTrack >= 0) {
                            val buffer = encoder.getOutputBuffer(index)!!
                            buffer.position(encInfo.offset)
                            buffer.limit(encInfo.offset + encInfo.size)
                            muxer.writeSampleData(audioTrack, buffer, encInfo)
                        }
                        encoder.releaseOutputBuffer(index, false)
                        if (encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                    }
                }
            }
        }

        /**
         * Copies PCM bytes into one encoder input buffer.
         * Returns bytes consumed; 0 when no input buffer is free yet
         * (the caller drains the encoder and retries — no data is ever lost).
         */
        fun queuePcm(chunk: ByteArray, offset: Int, length: Int): Int {
            val encIndex = encoder.dequeueInputBuffer(10_000L)
            if (encIndex < 0) return 0
            val encBuffer = encoder.getInputBuffer(encIndex)!!
            val toCopy = minOf(length, encBuffer.capacity())
            encBuffer.clear()
            encBuffer.put(chunk, offset, toCopy)
            val samples = toCopy / (2 * channels)
            encoder.queueInputBuffer(encIndex, 0, toCopy, ptsUs, 0)
            ptsUs += samples * 1_000_000L / sampleRate
            return toCopy
        }

        audioFiles.forEachIndexed { fileIndex, file ->
            verseStartUs[fileIndex] = ptsUs

            val extractor = MediaExtractor()
            var decoder: MediaCodec? = null
            try {
                extractor.setDataSource(file.absolutePath)
                var audioTrackIndex = -1
                var inputFormat: MediaFormat? = null
                for (t in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(t)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        extractor.selectTrack(t)
                        audioTrackIndex = t
                        inputFormat = format
                        break
                    }
                }
                if (audioTrackIndex < 0) error("No audio track in ${file.name}")

                decoder = MediaCodec.createDecoderByType(inputFormat!!.getString(MediaFormat.KEY_MIME)!!)
                decoder.configure(inputFormat, null, null, 0)
                decoder.start()

                var extractorDone = false
                var decoderDone = false
                val decInfo = MediaCodec.BufferInfo()

                while (!decoderDone) {
                    // Feed decoder
                    if (!extractorDone) {
                        val inIndex = decoder.dequeueInputBuffer(10_000L)
                        if (inIndex >= 0) {
                            val inBuffer = decoder.getInputBuffer(inIndex)!!
                            val size = extractor.readSampleData(inBuffer, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                extractorDone = true
                            } else {
                                decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    // Drain decoder PCM into the encoder, retrying until all bytes are queued
                    var outIndex = decoder.dequeueOutputBuffer(decInfo, 10_000L)
                    while (outIndex >= 0) {
                        val pcm = decoder.getOutputBuffer(outIndex)!!
                        val size = decInfo.size
                        if (size > 0) {
                            val pcmBytes = ByteArray(size)
                            pcm.position(decInfo.offset)
                            pcm.limit(decInfo.offset + size)
                            pcm.get(pcmBytes, 0, size)
                            var off = 0
                            while (off < size) {
                                val consumed = queuePcm(pcmBytes, off, size - off)
                                if (consumed == 0) drainEncoder(false) else off += consumed
                            }
                        }
                        decoder.releaseOutputBuffer(outIndex, false)
                        if (decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            decoderDone = true
                            break
                        }
                        outIndex = decoder.dequeueOutputBuffer(decInfo, 0L)
                    }
                    drainEncoder(false)
                }
            } finally {
                decoder?.stop()
                decoder?.release()
                extractor.release()
            }
        }

        // Queue EOS with retry — dropping it here hung the previous version at 40%.
        while (true) {
            val encIndex = encoder.dequeueInputBuffer(10_000L)
            if (encIndex >= 0) {
                encoder.queueInputBuffer(encIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                break
            }
            drainEncoder(false)
        }
        drainEncoder(true)

        encoder.stop()
        encoder.release()
        muxer.stop()
        muxer.release()

        return Result(outFile.absolutePath, verseStartUs, ptsUs)
    }
}
