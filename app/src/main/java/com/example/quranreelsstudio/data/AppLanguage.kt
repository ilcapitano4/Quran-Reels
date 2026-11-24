package com.example.quranreelsstudio.data

/**
 * One entry per language shown on the onboarding screen.
 * `code` is a standard language code, used later to load translated strings.
 * `flagEmoji` follows the request: Arabic uses the Egypt flag.
 */
data class AppLanguage(
    val code: String,
    val nativeName: String,
    val flagEmoji: String,
    val isRtl: Boolean
)

object AppLanguages {
    val all = listOf(
        AppLanguage(code = "ar", nativeName = "العربية", flagEmoji = "\uD83C\uDDEA\uD83C\uDDEC", isRtl = true), // Egypt flag
        AppLanguage(code = "en", nativeName = "English", flagEmoji = "\uD83C\uDDEC\uD83C\uDDE7", isRtl = false),
        AppLanguage(code = "de", nativeName = "Deutsch", flagEmoji = "\uD83C\uDDE9\uD83C\uDDEA", isRtl = false),
        AppLanguage(code = "fr", nativeName = "Français", flagEmoji = "\uD83C\uDDEB\uD83C\uDDF7", isRtl = false),
        AppLanguage(code = "tr", nativeName = "Türkçe", flagEmoji = "\uD83C\uDDF9\uD83C\uDDF7", isRtl = false),
        AppLanguage(code = "es", nativeName = "Español", flagEmoji = "\uD83C\uDDEA\uD83C\uDDF8", isRtl = false),
        AppLanguage(code = "ur", nativeName = "اردو", flagEmoji = "\uD83C\uDDF5\uD83C\uDDF0", isRtl = true),
        AppLanguage(code = "hi", nativeName = "हिन्दी", flagEmoji = "\uD83C\uDDEE\uD83C\uDDF3", isRtl = false)
    )
}
