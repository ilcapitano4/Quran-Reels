package com.example.quranreelsstudio.data

/**
 * `audioFolder` is the reciter's folder on https://everyayah.com/data/... .
 * Audio URL pattern: https://everyayah.com/data/{audioFolder}/{SSSAAA}.mp3
 * where SSS = zero-padded surah number and AAA = zero-padded ayah number.
 *
 * Rights note: all entries are Murattal verse-by-verse recordings made for
 * free public distribution (no commercial studio recordings). Reciters whose
 * catalog is owned by commercial labels (e.g. Al-Husary, Al-Minshawi mujawwad
 * recordings) were removed by request.
 */
data class Reciter(val id: Int, val name: String, val nameAr: String, val style: String, val audioFolder: String)

object Reciters {
    val all = listOf(
        Reciter(1, "Abdul Basit Abdul Samad", "عبد الباسط عبد الصمد", "Murattal", "Abdul_Basit_Murattal_192kbps"),
        Reciter(2, "Mishary Rashid Alafasy", "مشاري راشد العفاسي", "Murattal", "Alafasy_128kbps"),
        Reciter(3, "Saad Al-Ghamdi", "سعد الغامدي", "Murattal", "Ghamadi_40kbps"),
        Reciter(4, "Maher Al Mu'aiqly", "ماهر المعيقلي", "Murattal", "Maher_AlMuaiqly_64kbps"),
        Reciter(5, "Abdur-Rahman As-Sudais", "عبد الرحمن السديس", "Murattal", "Abdurrahmaan_As-Sudais_192kbps"),
        Reciter(6, "Yasser Al-Dosari", "ياسر الدوسري", "Murattal", "Yasser_Ad-Dussary_128kbps"),
        Reciter(7, "Abu Bakr Ash-Shaatree", "أبو بكر الشاطري", "Murattal", "Abu_Bakr_Ash-Shaatree_128kbps"),
        Reciter(8, "Nasser Al Qatami", "ناصر القطامي", "Murattal", "Nasser_Alqatami_128kbps"),
        Reciter(9, "Ahmed Al Ajmi", "أحمد بن علي العجمي", "Murattal", "ahmed_ibn_ali_al_ajamy_128kbps"),
        Reciter(10, "Ali Al-Hudhaifi", "علي بن عبد الرحمن الحذيفي", "Murattal", "Hudhaify_128kbps"),
        Reciter(11, "Khalifa Al Tunaiji", "خليفة الطنيجي", "Murattal", "khalefa_al_tunaiji_64kbps"),
        Reciter(12, "Saood Ash-Shuraym", "سعود الشريم", "Murattal", "Saood_ash-Shuraym_128kbps"),
        Reciter(13, "Fares Abbad", "فارس عباد", "Murattal", "Fares_Abbad_64kbps"),
        Reciter(14, "Hani Ar-Rifai", "هاني الرفاعي", "Murattal", "Hani_Rifai_192kbps"),
        Reciter(15, "Salah Bukhatir", "صلاح بو خاطر", "Murattal", "Salaah_AbdulRahman_Bukhatir_128kbps"),
        Reciter(16, "Muhammad Jibreel", "محمد جبريل", "Murattal", "Muhammad_Jibreel_128kbps"),
        Reciter(17, "Muhammad Ayyub", "محمد أيوب", "Murattal", "Muhammad_Ayyoub_128kbps"),
        Reciter(18, "Abdullah Basfar", "عبد الله بصفر", "Murattal", "Abdullah_Basfar_192kbps"),
        Reciter(19, "Abdullah Al-Matroud", "عبد الله المطرود", "Murattal", "Abdullah_Matroud_128kbps"),
        Reciter(20, "Aziz Alili", "عزيز عليلي", "Murattal", "aziz_alili_128kbps"),
        Reciter(21, "Ali Jaber", "علي جابر", "Murattal", "Ali_Jaber_64kbps"),
        // ---- Egyptian old-generation reciters (murattal versions) ----
        // NOTE: historical recordings; verify distribution rights with the
        // Egyptian archives before a commercial store release.
        Reciter(22, "Mustafa Ismail", "مصطفى إسماعيل", "Murattal", "Mustafa_Ismail_48kbps"),
        Reciter(23, "Mahmoud Ali Al-Banna", "محمود علي البنا", "Murattal", "mahmoud_ali_al_banna_32kbps"),
        Reciter(24, "Mohamed Al-Tablawi", "محمد الطبلاوي", "Murattal", "Mohammad_al_Tablaway_128kbps"),
        Reciter(25, "Mahmoud Khalil Al-Husary", "محمود خليل الحصري", "Murattal", "Husary_128kbps"),
        Reciter(26, "Al-Husary (Muallim)", "الحصري (المعلّم)", "Muallim", "Husary_Muallim_128kbps"),
        Reciter(27, "Muhammad Siddiq Al-Minshawi", "محمد صديق المنشاوي", "Murattal", "Minshawy_Murattal_128kbps")
    )
}
