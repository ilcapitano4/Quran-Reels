# Quran Reels Studio

> Create beautiful Quran recitation reels on Android — pick the verses, pick a reciter, pick a background, and export a vertical video ready to share.

![Android](https://img.shields.io/badge/Platform-Android-green)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)
![Min SDK 24](https://img.shields.io/badge/minSdk-24-blue)
![License MIT](https://img.shields.io/badge/License-MIT-yellow)

---

## About

**Quran Reels Studio** is a native Android app (Kotlin + Jetpack Compose) for creating short vertical Quran recitation videos — "reels" — with synchronized audio, elegant Arabic calligraphy, and cinematic backgrounds.

## Features

- **114 Surahs + 24+ curated passages** — browse, search, and select any ayah range.
- **20 reciters** — streaming recitation audio from EveryAyah (Abdul Basit, Mishary Alafasy, Maher Al Mu'aiqly, As-Sudais, and more).
- **Translations in 8 languages** — English, Arabic, German, Spanish, French, Indonesian, Russian, Turkish, Urdu (via AlQuran Cloud API).
- **Cinematic backgrounds** — bundled nature videos & photos, import your own media, or generate AI backgrounds.
- **25 Arabic fonts** — Amiri, Cairo, Tajawal, Scheherazade New, Noto Naskh/Kufi, and more.
- **Full Uthmani text bundled** — the complete Quran text ships inside the app, offline.
- **Vertical video export** — renders the reel (video + recitation audio + verse overlays) ready for TikTok / Reels / Shorts.
- **Multi-language UI** — Arabic, English, German, Spanish, French, Hindi, Turkish, Urdu.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose (BOM 2024.02.00), Material 3 |
| Audio / Video | Media3 ExoPlayer 1.3.1 |
| Networking | OkHttp 4.12, Coroutines |
| Build | Android Gradle Plugin 8.13.2, Java 17 |
| Min / Target SDK | 24 / 34 |

## Getting Started

### Requirements

- Android Studio (latest stable) with SDK 34
- JDK 17

### Build

```bash
# Debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Install on a connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

## Download

- **Release APK (v28, ~160 MB):** [QuranReelsStudio-v28-release.apk](https://github.com/ilcapitano4/Quran-Reels/releases/download/v28/QuranReelsStudio-v28-release.apk)
- **Release page:** https://github.com/ilcapitano4/Quran-Reels/releases/tag/v28

> Note: the release APK is signed with the debug key and is provided for testing/smoothness evaluation only.

## Media Assets

The bundled background videos/photos and fonts are large binary assets, and the release APK (v28, ~160 MB) exceeds GitHub's 100 MB per-file limit for files *inside* a repository — so they are **not** stored in the repo itself. The APK is attached to [Release v28](https://github.com/ilcapitano4/Quran-Reels/releases/tag/v28), and the full bundle (source + APK + all media) is available here:

- **Full bundle — v28:** https://drive.google.com/file/d/1ayREpV83gefbxOccEYng4MO-uNXA1Rn5/view?usp=drivesdk

## Project Structure

```
QuranReelsStudio-v5/
├── app/src/main/java/com/example/quranreelsstudio/
│   ├── MainActivity.kt            # Entry point
│   ├── audio/RecitationPlayer.kt  # ExoPlayer recitation playback
│   ├── data/                      # Surah, VerseSelection, Reciter, fonts, languages
│   ├── export/                    # VideoComposer, VideoRenderEngine, AudioTranscoder
│   ├── media/                     # Media loading & thumbnails
│   ├── network/                   # Translations (AlQuran Cloud), AI backgrounds
│   └── ui/                        # Studio, verses, export, onboarding, theme
├── app/src/main/assets/
│   ├── quran-uthmani.json         # Complete Uthmani Quran text
│   ├── photos/ & videos/          # Bundled background media
├── app/src/main/res/font/         # 25 Arabic fonts
└── app/src/main/res/values*/      # Strings in 8 languages
```

## Data Sources & Credits

- Quran text & translations: [AlQuran Cloud API](https://alquran.cloud/api) — `api.alquran.cloud`
- Recitation audio streams: [EveryAyah](https://everyayah.com)
- AI background images: [Pollinations.ai](https://image.pollinations.ai)
- Arabic fonts: Amiri, Cairo, Tajawal, Noto, Scheherazade New, and others (see `res/font/`)

## Contributing

Issues and pull requests are welcome. If you add a feature, please keep the app offline-first: the Quran text must always work without internet.

## License

MIT License — see [LICENSE](LICENSE).

---

# استوديو ريلز القرآن

> أنشئ مقاطع قرآنية جميلة على أندرويد — اختر الآيات، واختر القارئ، واختر الخلفية، وصدّر فيديو عمودي جاهز للمشاركة.

## نبذة عن التطبيق

**استوديو ريلز القرآن** تطبيق أندرويد أصلي (Kotlin + Jetpack Compose) لإنشاء مقاطع قرآنية قصيرة عمودية (ريلز) بصوت التلاوة، وخط عربي أنيق، وخلفيات سينمائية.

## المميزات

- **١١٤ سورة + أكثر من ٢٤ مقطعًا مختارًا** — تصفح وابحث وحدد أي نطاق آيات.
- **٢٠ قارئًا** — صوت التلاوة من EveryAyah (عبد الباسط، مشاري العفاسي، ماهر المعيقلي، السديس، وغيرهم).
- **ترجمة بـ ٨ لغات** — إنجليزي، عربي، ألماني، إسباني، فرنسي، إندونيسي، روسي، تركي، أردو (عبر AlQuran Cloud API).
- **خلفيات سينمائية** — فيديوهات وصور طبيعة مدمجة، أو استورد وسائطك، أو ولّد خلفية بالذكاء الاصطناعي.
- **٢٥ خطًا عربيًا** — أميري، القاهرة، تجوّل، Scheherazade New، نسخ وكوفي، وغيرها.
- **النص العثماني كاملًا داخل التطبيق** — يعمل دون إنترنت.
- **تصدير فيديو عمودي** — يركّب المقطع (فيديو + صوت + الآيات) جاهزًا لتيك توك وريلز وشورتس.
- **واجهة بعدة لغات** — عربي، إنجليزي، ألماني، إسباني، فرنسي، هندي، تركي، أردو.

## التقنيات

| الطبقة | التقنية |
|---|---|
| اللغة | Kotlin 1.9.22 |
| الواجهة | Jetpack Compose (BOM 2024.02.00)، Material 3 |
| الصوت / الفيديو | Media3 ExoPlayer 1.3.1 |
| الشبكة | OkHttp 4.12، Coroutines |
| البناء | Android Gradle Plugin 8.13.2، Java 17 |
| الحد الأدنى / المستهدف SDK | 24 / 34 |

## البدء

### المتطلبات

- Android Studio (أحدث نسخة مستقرة) مع SDK 34
- JDK 17

### البناء

```bash
# Debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# التثبيت على جهاز متصل
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

أو افتح المشروع في Android Studio: **Build ← Build Bundle(s) / APK(s) ← Build APK(s)**.

## التحميل

- **ملف APK للإصدار (v28، حوالي ١٦٠ ميجا):** [QuranReelsStudio-v28-release.apk](https://github.com/ilcapitano4/Quran-Reels/releases/download/v28/QuranReelsStudio-v28-release.apk)
- **صفحة الإصدار:** https://github.com/ilcapitano4/Quran-Reels/releases/tag/v28

> ملاحظة: نسخة APK موقّعة بمفتاح التصحيح (debug) ومخصصة للتجربة وتقييم الأداء فقط.

## الوسائط

الوسائط (فيديوهات/صور/خطوط) وملف APK (v28، حوالي ١٦٠ ميجا) تتجاوز حد GitHub البالغ ١٠٠ ميجا للملفات *داخل* المستودع، لذا فهي **ليست** في السورس نفسه. الـAPK مرفق في [الإصدار v28](https://github.com/ilcapitano4/Quran-Reels/releases/tag/v28)، والحزمة الكاملة (السورس + APK + كل الوسائط) متاحة هنا:

- **الحزمة الكاملة — v28:** https://drive.google.com/file/d/1ayREpV83gefbxOccEYng4MO-uNXA1Rn5/view?usp=drivesdk

## بنية المشروع

```
QuranReelsStudio-v5/
├── app/src/main/java/com/example/quranreelsstudio/
│   ├── MainActivity.kt            # نقطة الدخول
│   ├── audio/RecitationPlayer.kt  # تشغيل التلاوة عبر ExoPlayer
│   ├── data/                      # السور، اختيار الآيات، القرّاء، الخطوط، اللغات
│   ├── export/                    # VideoComposer, VideoRenderEngine, AudioTranscoder
│   ├── media/                     # تحميل الوسائط والمصغرات
│   ├── network/                   # الترجمات (AlQuran Cloud)، الخلفيات بالذكاء الاصطناعي
│   └── ui/                        # الاستوديو، الآيات، التصدير، التهيئة، السمة
├── app/src/main/assets/
│   ├── quran-uthmani.json         # نص القرآن العثماني كاملًا
│   ├── photos/ & videos/          # وسائط الخلفية المدمجة
├── app/src/main/res/font/         # 25 خطًا عربيًا
└── app/src/main/res/values*/      # النصوص بـ 8 لغات
```

## المصادر والشكر

- نص القرآن والترجمات: [AlQuran Cloud API](https://alquran.cloud/api) — `api.alquran.cloud`
- صوت التلاوة: [EveryAyah](https://everyayah.com)
- صور الخلفية بالذكاء الاصطناعي: [Pollinations.ai](https://image.pollinations.ai)
- الخطوط العربية: أميري، القاهرة، تجوّل، Noto، Scheherazade New، وغيرها (انظر `res/font/`)

## المساهمة

نرحب بالاقتراحات والمساهمات. لو أضفت ميزة، حافظ على عمل التطبيق دون إنترنت: نص القرآن لازم يشتغل دائمًا بلا اتصال.

## الرخصة

رخصة MIT — انظر [LICENSE](LICENSE).
