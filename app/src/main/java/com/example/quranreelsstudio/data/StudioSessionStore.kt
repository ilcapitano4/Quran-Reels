package com.example.quranreelsstudio.data

import android.content.Context
import org.json.JSONObject

/**
 * Persists the user's studio selections so the app reopens exactly where
 * they left off (surah, range, reciter, background, style, aspect...).
 */
class StudioSessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("studio_session", Context.MODE_PRIVATE)

    fun save(s: Selection) {
        val json = JSONObject()
            .put("aspect", s.aspect)
            .put("surahNumber", s.surahNumber)
            .put("fromAyah", s.fromAyah)
            .put("toAyah", s.toAyah)
            .put("wantsTranslation", s.wantsTranslation)
            .put("translationCode", s.translationCode ?: "")
            .put("fontId", s.fontId)
            .put("subtitleSize", s.subtitleSize)
            .put("reciterId", s.reciterId)
            .put("backgroundType", s.backgroundType)
            .put("videoId", s.videoId)
            .put("photoId", s.photoId)
            .put("blankColor", s.blankColor)
            .put("visibility", s.visibility)
        prefs.edit().putString(KEY, json.toString()).apply()
    }

    fun load(): Selection? {
        return try {
            val raw = prefs.getString(KEY, null) ?: return null
            val json = JSONObject(raw)
            Selection(
                aspect = json.optString("aspect", "REEL_9_16"),
                surahNumber = json.optInt("surahNumber", 1),
                fromAyah = json.optInt("fromAyah", 1),
                toAyah = json.optInt("toAyah", 7),
                wantsTranslation = json.optBoolean("wantsTranslation", false),
                translationCode = json.optString("translationCode", "").ifEmpty { null },
                fontId = json.optString("fontId", "amiri"),
                subtitleSize = json.optInt("subtitleSize", 14),
                reciterId = json.optInt("reciterId", 1),
                backgroundType = json.optString("backgroundType", "MOTION_VIDEO"),
                videoId = json.optInt("videoId", 1),
                photoId = json.optInt("photoId", 101),
                blankColor = json.optLong("blankColor", 0xFFFFFFFFL),
                visibility = json.optInt("visibility", 65)
            )
        } catch (e: Exception) {
            null
        }
    }

    data class Selection(
        val aspect: String,
        val surahNumber: Int,
        val fromAyah: Int,
        val toAyah: Int,
        val wantsTranslation: Boolean,
        val translationCode: String?,
        val fontId: String,
        val subtitleSize: Int,
        val reciterId: Int,
        val backgroundType: String,
        val videoId: Int,
        val photoId: Int,
        val blankColor: Long,
        val visibility: Int
    )

    private companion object { const val KEY = "selection" }
}
