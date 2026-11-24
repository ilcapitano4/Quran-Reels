package com.example.quranreelsstudio.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Generates background images from a text prompt using the free
 * Pollinations image API (no API key required):
 * https://image.pollinations.ai/prompt/{prompt}?width=&height=&nologo=true&seed=
 *
 * The result is saved into app storage and returned as an absolute path.
 */
object AiBackgroundGenerator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(240, TimeUnit.SECONDS)
        .build()

    suspend fun generateImage(context: Context, prompt: String, width: Int, height: Int): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(prompt.trim(), "UTF-8")
                val seed = Random.nextInt(1, 1_000_000)
                val url = "https://image.pollinations.ai/prompt/$encoded" +
                    "?width=$width&height=$height&nologo=true&seed=$seed"

                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    val bytes = response.body?.bytes() ?: error("Empty response from AI service")
                    if (bytes.size < 1_000) error("AI service returned an invalid image")

                    val dir = File(context.filesDir, "ai_generated")
                    dir.mkdirs()
                    val outFile = File(dir, "ai_${System.currentTimeMillis()}.jpg")
                    outFile.writeBytes(bytes)
                    outFile.absolutePath
                }
            }
        }
}
