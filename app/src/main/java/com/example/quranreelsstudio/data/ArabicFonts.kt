package com.example.quranreelsstudio.data

import androidx.compose.runtime.Immutable
import com.example.quranreelsstudio.R

/**
 * Arabic fonts bundled in res/font (SIL OFL licensed, from Google Fonts).
 * `resId == 0` means the system default font.
 */
@Immutable
data class ArabicFont(val id: String, val displayName: String, val resId: Int)

object ArabicFonts {
    val all = listOf(
        ArabicFont("default", "Default", 0),
        ArabicFont("amiri", "Amiri أميري", R.font.font_amiri),
        ArabicFont("scheherazade", "Scheherazade شهرزاد", R.font.font_scheherazade),
        ArabicFont("aref_ruqaa", "Aref Ruqaa عارف رقعة", R.font.font_aref_ruqaa),
        ArabicFont("lateef", "Lateef لطيف", R.font.font_lateef),
        ArabicFont("cairo", "Cairo القاهرة", R.font.font_cairo),
        ArabicFont("tajawal", "Tajawal تجوال", R.font.font_tajawal),
        ArabicFont("almarai", "Almarai المراعي", R.font.font_almarai),
        ArabicFont("el_messiri", "El Messiri المسيري", R.font.font_el_messiri),
        ArabicFont("reem_kufi", "Reem Kufi ريم كوفي", R.font.font_reem_kufi),
        ArabicFont("changa", "Changa تشانجا", R.font.font_changa),
        ArabicFont("lalezar", "Lalezar لاله زار", R.font.font_lalezar),
        ArabicFont("markazi_text", "Markazi Text مركزي", R.font.font_markazi_text),
        ArabicFont("mada", "Mada مدى", R.font.font_mada),
        ArabicFont("mirza", "Mirza ميرزا", R.font.font_mirza),
        ArabicFont("rakkas", "Rakkas راقص", R.font.font_rakkas),
        ArabicFont("katibeh", "Katibeh كتيبة", R.font.font_katibeh),
        ArabicFont("jomhuria", "Jomhuria جمهورية", R.font.font_jomhuria),
        ArabicFont("naskh", "Noto Naskh نسخ", R.font.font_noto_naskh),
        ArabicFont("kufi", "Noto Kufi كوفي", R.font.font_noto_kufi),
        ArabicFont("sans_arabic", "Noto Sans Arabic", R.font.font_noto_sans_arabic),
        ArabicFont("ibm_plex", "IBM Plex Arabic", R.font.font_ibm_plex_arabic),
        ArabicFont("gulzar", "Gulzar گلزار", R.font.font_gulzar),
        ArabicFont("baloo", "Baloo Bhaijaan", R.font.font_baloo_bhaijaan),
        ArabicFont("vibes", "Vibes فيبس", R.font.font_vibes),
        ArabicFont("blaka", "Blaka بلاكا", R.font.font_blaka)
    )

    fun byId(id: String): ArabicFont = all.firstOrNull { it.id == id } ?: all.first()
}
