package com.example.quranreelsstudio.export

import android.graphics.Bitmap
import android.media.Image

/**
 * Bitmap <-> YUV conversions (BT.601) for the encoder/decoder pipelines.
 */
object Yuv {

    fun imageToBitmap(image: Image): Bitmap {
        val width = image.width
        val height = image.height
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuf = planeBytes(yPlane)
        val uBuf = planeBytes(uPlane)
        val vBuf = planeBytes(vPlane)
        val yStride = yPlane.rowStride
        val uStride = uPlane.rowStride
        val vStride = vPlane.rowStride
        val uPx = uPlane.pixelStride
        val vPx = vPlane.pixelStride

        val pixels = IntArray(width * height)
        var p = 0
        for (row in 0 until height) {
            val yRow = row * yStride
            val uvRow = row / 2
            val uRow = uvRow * uStride
            val vRow = uvRow * vStride
            for (col in 0 until width) {
                val y = yBuf[yRow + col].toInt() and 0xFF
                val uvCol = col / 2
                val u = (uBuf[uRow + uvCol * uPx].toInt() and 0xFF) - 128
                val v = (vBuf[vRow + uvCol * vPx].toInt() and 0xFF) - 128
                var r = y + ((91881 * v) shr 16)
                var g = y - ((22554 * u + 46802 * v) shr 16)
                var b = y + ((116130 * u) shr 16)
                if (r < 0) r = 0 else if (r > 255) r = 255
                if (g < 0) g = 0 else if (g > 255) g = 255
                if (b < 0) b = 0 else if (b > 255) b = 255
                pixels[p++] = (255 shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    /**
     * Converts an ARGB bitmap into a packed NV12 (YUV420 semi-planar) buffer:
     * [Y plane w*h][interleaved UV plane w*h/2] — the format accepted by
     * virtually every hardware AVC encoder via COLOR_FormatYUV420SemiPlanar.
     */
    fun bitmapToNv12(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val ySize = width * height
        val out = ByteArray(ySize + ySize / 2)
        var yi = 0
        var ui = ySize
        var p = 0
        for (row in 0 until height) {
            val evenRow = row % 2 == 0
            for (col in 0 until width) {
                val argb = pixels[p++]
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF
                var y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                if (y < 0) y = 0 else if (y > 255) y = 255
                out[yi++] = y.toByte()
                if (evenRow && col % 2 == 0) {
                    var u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    var v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    if (u < 0) u = 0 else if (u > 255) u = 255
                    if (v < 0) v = 0 else if (v > 255) v = 255
                    out[ui++] = u.toByte()
                    out[ui++] = v.toByte()
                }
            }
        }
        return out
    }

    /** NV12 bytes for a solid color (used by the Blank Color background). */
    fun solidToNv12(color: Int, width: Int, height: Int): ByteArray {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        var y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
        var u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
        var v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
        if (y < 0) y = 0 else if (y > 255) y = 255
        if (u < 0) u = 0 else if (u > 255) u = 255
        if (v < 0) v = 0 else if (v > 255) v = 255

        val ySize = width * height
        val out = ByteArray(ySize + ySize / 2)
        java.util.Arrays.fill(out, 0, ySize, y.toByte())
        var ui = ySize
        while (ui < out.size) {
            out[ui++] = u.toByte()
            out[ui++] = v.toByte()
        }
        return out
    }

    private fun planeBytes(plane: Image.Plane): ByteArray {
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        buffer.rewind()
        return bytes
    }
}
