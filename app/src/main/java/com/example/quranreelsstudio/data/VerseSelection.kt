package com.example.quranreelsstudio.data

data class VerseSelection(
    val surah: Surah,
    val fromAyah: Int,
    val toAyah: Int,
    val translationLanguageCode: String?
)
