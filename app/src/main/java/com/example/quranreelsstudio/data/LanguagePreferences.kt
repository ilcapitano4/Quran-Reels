package com.example.quranreelsstudio.data

import android.content.Context

/**
 * Persists the user's chosen app language so the onboarding
 * screen only appears once (until the user changes it from Settings).
 */
class LanguagePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSavedLanguageCode(): String? = prefs.getString(KEY_LANGUAGE_CODE, null)

    fun saveLanguageCode(code: String) {
        prefs.edit().putString(KEY_LANGUAGE_CODE, code).apply()
    }

    companion object {
        private const val PREFS_NAME = "quran_reels_prefs"
        private const val KEY_LANGUAGE_CODE = "selected_language_code"
    }
}
