package com.example.quranreelsstudio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists AI-generated background media (photos & motion clips) so they
 * become a permanent part of the app library with delete support.
 */
class AiLibraryStore(context: Context) {

    data class Entry(
        val id: Int,
        val title: String,
        val arabicTitle: String,
        val path: String,
        val motion: Boolean,
        val createdAt: Long
    )

    private val prefs = context.getSharedPreferences("ai_library", Context.MODE_PRIVATE)

    fun load(): List<Entry> {
        return try {
            val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val path = obj.optString("path")
                if (path.isNotEmpty() && File(path).exists()) {
                    Entry(
                        id = obj.getInt("id"),
                        title = obj.getString("title"),
                        arabicTitle = obj.getString("arabicTitle"),
                        path = path,
                        motion = obj.getBoolean("motion"),
                        createdAt = obj.getLong("createdAt")
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(entry: Entry) {
        val items = load().toMutableList()
        items.add(0, entry)
        persist(items)
    }

    fun remove(id: Int) {
        val items = load().toMutableList()
        items.firstOrNull { it.id == id }?.let { runCatching { File(it.path).delete() } }
        persist(items.filter { it.id != id })
    }

    fun toMediaItem(entry: Entry): BackgroundMediaItem {
        val isImageFile = entry.path.endsWith(".jpg", true) ||
            entry.path.endsWith(".jpeg", true) ||
            entry.path.endsWith(".png", true)
        // Motion entries from the current version are real MP4 clips (played
        // with the video player); legacy motion entries were still images.
        val treatAsVideo = entry.motion && !isImageFile
        return BackgroundMediaItem(
            id = entry.id,
            title = entry.title,
            arabicTitle = entry.arabicTitle,
            category = "AI",
            isVideo = treatAsVideo,
            contentUri = "file://${entry.path}",
            motion = entry.motion && !treatAsVideo
        )
    }

    private fun persist(items: List<Entry>) {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("arabicTitle", it.arabicTitle)
                    .put("path", it.path)
                    .put("motion", it.motion)
                    .put("createdAt", it.createdAt)
            )
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    private companion object {
        const val KEY_ITEMS = "ai_items"
    }
}
