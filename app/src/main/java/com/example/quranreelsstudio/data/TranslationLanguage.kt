package com.example.quranreelsstudio.data

/**
 * `edition` is the translation edition identifier on api.alquran.cloud,
 * e.g. https://api.alquran.cloud/v1/surah/112/en.sahih
 */
data class TranslationLanguage(val code: String, val displayName: String, val edition: String)

object TranslationLanguages {
    val all = listOf(
        TranslationLanguage("en", "English", "en.sahih"),
        TranslationLanguage("fr", "Français", "fr.hamidullah"),
        TranslationLanguage("es", "Español", "es.cortes"),
        TranslationLanguage("de", "Deutsch", "de.aburida"),
        TranslationLanguage("ur", "اردو", "ur.jalandhry"),
        TranslationLanguage("tr", "Türkçe", "tr.diyanet"),
        TranslationLanguage("id", "Bahasa Indonesia", "id.indonesian"),
        TranslationLanguage("ru", "Русский", "ru.kuliev")
    )
}
