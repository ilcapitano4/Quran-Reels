package com.example.quranreelsstudio.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AyahText(val numberInSurah: Int, val text: String)

/**
 * Loads the full Uthmani Quran text from the bundled asset
 * (assets/quran-uthmani.json, sourced from api.alquran.cloud).
 * The file is parsed once on a background thread and cached in memory,
 * so verse text works completely offline.
 */
class QuranTextRepository(context: Context) {

    private val appContext = context.applicationContext
    private val surahCache = arrayOfNulls<List<AyahText>>(115)
    @Volatile
    private var loaded = false

    suspend fun surahAyahs(surahNumber: Int): List<AyahText> = withContext(Dispatchers.IO) {
        ensureLoaded()
        surahCache.getOrNull(surahNumber) ?: emptyList()
    }

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return

        val raw = appContext.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val surahs = JSONObject(raw).getJSONObject("data").getJSONArray("surahs")

        // Al-Fatihah 1:1 is the basmala itself; the same string is prefixed to
        // the first ayah of every other surah in this edition. Strip it so
        // verse 1 renders as the actual verse when shown standalone.
        var basmala: String? = null

        for (i in 0 until surahs.length()) {
            val surahJson = surahs.getJSONObject(i)
            val surahNumber = surahJson.getInt("number")
            val ayahsJson = surahJson.getJSONArray("ayahs")
            val list = ArrayList<AyahText>(ayahsJson.length())

            for (j in 0 until ayahsJson.length()) {
                val ayahJson = ayahsJson.getJSONObject(j)
                var text = ayahJson.getString("text")
                if (surahNumber == 1 && j == 0) {
                    basmala = text
                } else if (j == 0) {
                    basmala?.let { prefix ->
                        if (text.startsWith(prefix)) {
                            text = text.removePrefix(prefix).trim()
                        }
                    }
                }
                list += AyahText(numberInSurah = ayahJson.getInt("numberInSurah"), text = text.trim())
            }
            surahCache[surahNumber] = list
        }
        loaded = true
    }

    private companion object {
        const val ASSET_NAME = "quran-uthmani.json"
    }
}
