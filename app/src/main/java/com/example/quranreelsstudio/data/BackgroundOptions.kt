package com.example.quranreelsstudio.data

enum class BackgroundType { MOTION_VIDEO, NATURAL_PHOTO, BLANK_COLOR }
enum class MediaSourceTab { APP_LIBRARY, IMPORT_DEVICE, AI_GENERATOR }
enum class VideoPlaybackMode { SINGLE_LOOP, MULTI_SEQUENCE }

/**
 * A background media source. Exactly one of the location fields is set:
 * - [assetPath]: file bundled inside the APK (e.g. "videos/ocean_waves.mp4")
 * - [contentUri]: picked from the device or copied into app storage ("file://...")
 * - [remoteUrl]: direct web URL (reserved for future AI video results)
 *
 * [motion] marks an AI-generated still image animated as a motion clip
 * (slow Ken Burns zoom/pan) — used by the "AI Motion" generator.
 */
data class BackgroundMediaItem(
    val id: Int,
    val title: String,
    val arabicTitle: String,
    val category: String,
    val isVideo: Boolean,
    val assetPath: String? = null,
    val contentUri: String? = null,
    val remoteUrl: String? = null,
    val motion: Boolean = false
)

object BackgroundLibrary {

    // 20 bundled 720p clips (Mixkit free license) shipped inside the APK.
    val videos = listOf(
        BackgroundMediaItem(1, "Deep Ocean Swell", "أمواج المحيط الهادئة", "Oceans", isVideo = true, assetPath = "videos/ocean_waves.mp4"),
        BackgroundMediaItem(2, "Blooming Flora & Garden", "حدائق وزهور طبيعية", "Nature", isVideo = true, assetPath = "videos/blooming_flora.mp4"),
        BackgroundMediaItem(3, "Deep Galaxy & Celestial Sky", "السماء المرصعة بالنجوم", "Cosmos", isVideo = true, assetPath = "videos/starry_sky.mp4"),
        BackgroundMediaItem(4, "Drifting Clouds", "سحب متحركة", "Clouds", isVideo = true, assetPath = "videos/drifting_clouds.mp4"),
        BackgroundMediaItem(5, "Mountain Waterfall", "شلال جبلي", "Water", isVideo = true, assetPath = "videos/waterfall.mp4"),
        BackgroundMediaItem(6, "Golden Sunset on the Sea", "غروب ذهبي على البحر", "Oceans", isVideo = true, assetPath = "videos/golden_sunset.mp4"),
        BackgroundMediaItem(7, "Warm Campfire Evening", "سهرة حول نار المخيم", "Cozy", isVideo = true, assetPath = "videos/campfire.mp4"),
        BackgroundMediaItem(8, "Pink Blossom Garden", "حديقة الأزهار الوردية", "Nature", isVideo = true, assetPath = "videos/blossom_garden.mp4"),
        BackgroundMediaItem(9, "White Sand Beach", "شاطئ الرمال البيضاء", "Oceans", isVideo = true, assetPath = "videos/white_sand_beach.mp4"),
        BackgroundMediaItem(10, "Countryside Meadow", "مرج ريفي", "Nature", isVideo = true, assetPath = "videos/countryside_meadow.mp4"),
        BackgroundMediaItem(11, "Mountain Road Journey", "طريق بين الجبال", "Scenic", isVideo = true, assetPath = "videos/mountain_road.mp4"),
        BackgroundMediaItem(12, "Palm Tree in the Sun", "نخلة تحت الشمس", "Nature", isVideo = true, assetPath = "videos/palm_tree.mp4"),
        BackgroundMediaItem(13, "Field of Sunflowers", "حقل دوّار الشمس", "Nature", isVideo = true, assetPath = "videos/sunflowers.mp4"),
        BackgroundMediaItem(14, "Rocky Coast Waves", "أمواج على ساحل صخري", "Oceans", isVideo = true, assetPath = "videos/rocky_coast.mp4"),
        BackgroundMediaItem(15, "Tranquil Creek", "جدول مائي هادئ", "Water", isVideo = true, assetPath = "videos/tranquil_creek.mp4"),
        BackgroundMediaItem(16, "Curved Mountain Highway", "طريق جبلي متعرج", "Scenic", isVideo = true, assetPath = "videos/curved_mountain_highway.mp4"),
        BackgroundMediaItem(17, "Into the Tranquil Forest", "وسط الغابة الهادئة", "Nature", isVideo = true, assetPath = "videos/tranquil_forest_slide.mp4"),
        BackgroundMediaItem(18, "Sunny Forest Stream", "جدول الغابة في الشمس", "Water", isVideo = true, assetPath = "videos/forest_stream.mp4"),
        BackgroundMediaItem(19, "Moon Over Snowy Forest", "القمر فوق غابة مثلجة", "Cosmos", isVideo = true, assetPath = "videos/moon_snowy_forest.mp4"),
        BackgroundMediaItem(20, "Mountain Range Landscape", "سلسلة جبال شامخة", "Mountains", isVideo = true, assetPath = "videos/mountain_range.mp4")
    )

    // 30 bundled AI-generated nature photos (Pollinations, 720×1280).
    val photos = listOf(
        BackgroundMediaItem(101, "Sunrise Over the Sea", "شروق الشمس فوق البحر", "Oceans", isVideo = false, assetPath = "photos/sunrise_sea.jpg"),
        BackgroundMediaItem(102, "Green Valley", "وادٍ أخضر", "Nature", isVideo = false, assetPath = "photos/green_valley.jpg"),
        BackgroundMediaItem(103, "Snow-Capped Peaks", "قمم جبلية مغطاة بالثلوج", "Mountains", isVideo = false, assetPath = "photos/snow_peaks.jpg"),
        BackgroundMediaItem(104, "Desert Dunes at Dawn", "كثبان صحراوية عند الفجر", "Desert", isVideo = false, assetPath = "photos/desert_dunes.jpg"),
        BackgroundMediaItem(105, "Starry Night Sky", "سماء ليلية مرصعة بالنجوم", "Cosmos", isVideo = false, assetPath = "photos/starry_night.jpg"),
        BackgroundMediaItem(106, "Tranquil Forest", "غابة هادئة", "Nature", isVideo = false, assetPath = "photos/tranquil_forest.jpg"),
        BackgroundMediaItem(107, "Lotus Pond", "بركة اللوتس", "Nature", isVideo = false, assetPath = "photos/lotus_pond.jpg"),
        BackgroundMediaItem(108, "Autumn Forest", "غابة الخريف", "Nature", isVideo = false, assetPath = "photos/autumn_forest.jpg"),
        BackgroundMediaItem(109, "Cherry Blossoms", "أزهار الكرز", "Nature", isVideo = false, assetPath = "photos/cherry_blossom.jpg"),
        BackgroundMediaItem(110, "Lavender Field", "حقل الخزامى", "Nature", isVideo = false, assetPath = "photos/lavender_field.jpg"),
        BackgroundMediaItem(111, "Northern Lights", "الشفق القطبي", "Cosmos", isVideo = false, assetPath = "photos/northern_lights.jpg"),
        BackgroundMediaItem(112, "Ocean Cliff Sunset", "جرف المحيط عند الغروب", "Oceans", isVideo = false, assetPath = "photos/ocean_cliff.jpg"),
        BackgroundMediaItem(113, "Tropical Beach", "شاطئ استوائي", "Oceans", isVideo = false, assetPath = "photos/tropical_beach.jpg"),
        BackgroundMediaItem(114, "Full Moon Over the Sea", "بدر فوق البحر", "Cosmos", isVideo = false, assetPath = "photos/full_moon_sea.jpg"),
        BackgroundMediaItem(115, "Misty Pine Forest", "غابة صنوبر ضبابية", "Nature", isVideo = false, assetPath = "photos/misty_pines.jpg"),
        BackgroundMediaItem(116, "Desert Oasis", "واحة الصحراء", "Desert", isVideo = false, assetPath = "photos/desert_oasis.jpg"),
        BackgroundMediaItem(117, "Rice Terraces", "مدرجات الأرز", "Nature", isVideo = false, assetPath = "photos/rice_terraces.jpg"),
        BackgroundMediaItem(118, "Canyon Sunrise", "شروق الشمس في الوادي", "Desert", isVideo = false, assetPath = "photos/canyon_sunrise.jpg"),
        BackgroundMediaItem(119, "Waterfall Lagoon", "بحيرة الشلال", "Water", isVideo = false, assetPath = "photos/waterfall_lagoon.jpg"),
        BackgroundMediaItem(120, "Golden Wheat Field", "حقل قمح ذهبي", "Nature", isVideo = false, assetPath = "photos/wheat_field.jpg"),
        BackgroundMediaItem(121, "Dramatic Storm Clouds", "سحب عاصفة", "Sky", isVideo = false, assetPath = "photos/storm_clouds.jpg"),
        BackgroundMediaItem(122, "Autumn Lake Reflection", "انعكاس الخريف على البحيرة", "Nature", isVideo = false, assetPath = "photos/autumn_lake.jpg"),
        BackgroundMediaItem(123, "Tulip Garden", "حديقة التوليب", "Nature", isVideo = false, assetPath = "photos/tulip_garden.jpg"),
        BackgroundMediaItem(124, "Mirror Mountain Lake", "بحيرة الجبال المرآة", "Mountains", isVideo = false, assetPath = "photos/mountain_lake.jpg"),
        BackgroundMediaItem(125, "Coral Reef", "الشعاب المرجانية", "Oceans", isVideo = false, assetPath = "photos/coral_reef.jpg"))

    val blankColors = listOf(
        0xFFFFFFFF, // white
        0xFFF5F5F0, // off white
        0xFFE8F4FD, // pale sky
        0xFFAEE0F5, // light blue
        0xFF87CEEB, // sky blue
        0xFF4A90D9, // ocean blue
        0xFF10222B, // deep navy
        0xFF0D1B24, // midnight
        0xFF000000, // black
        0xFF37474F, // slate gray
        0xFF0D3B45, // deep teal
        0xFF1B5E5A, // teal green
        0xFF2E7D32, // forest green
        0xFFA5D6A7, // soft green
        0xFFFFF8E1, // cream
        0xFFFFE0B2, // sand
        0xFFD7A86E, // desert gold
        0xFF8D6E63, // earth brown
        0xFFF8BBD0, // rose
        0xFFCE93D8  // lavender
    )
}
