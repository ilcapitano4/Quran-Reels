package com.example.quranreelsstudio.data

enum class ReelAspectRatio(val label: String, val dimensions: String, val width: Int, val height: Int) {
    REEL_9_16("9:16 Reel", "1080×1920", 9, 16),
    POST_1_1("1:1 Post", "1080×1080", 1, 1),
    WIDE_16_9("16:9 Wide", "1920×1080", 16, 9)
}
