package com.example.quranreelsstudio.network

import com.example.quranreelsstudio.data.AyahText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches a translated surah from api.alquran.cloud (free, no API key):
 * https://api.alquran.cloud/v1/surah/{surahNumber}/{edition}
 *
 * Results are cached in memory per (edition, surah).
 */
object TranslationRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val cache = HashMap<String, List<AyahText>>()

    suspend fun fetch(edition: String, surahNumber: Int): Result<List<AyahText>> =
        withContext(Dispatchers.IO) {
            val key = "$edition:$surahNumber"
            cache[key]?.let { return@withContext Result.success(it) }

            runCatching {
                val request = Request.Builder()
                    .url("https://api.alquran.cloud/v1/surah/$surahNumber/$edition")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    val body = response.body?.string() ?: error("Empty response body")

                    val ayahsJson = JSONObject(body).getJSONObject("data").getJSONArray("ayahs")
                    val result = ArrayList<AyahText>(ayahsJson.length())
                    for (i in 0 until ayahsJson.length()) {
                        val ayahJson = ayahsJson.getJSONObject(i)
                        result += AyahText(
                            numberInSurah = ayahJson.getInt("numberInSurah"),
                            text = ayahJson.getString("text").trim()
                        )
                    }
                    synchronized(cache) { cache[key] = result }
                    result
                }
            }
        }
}
