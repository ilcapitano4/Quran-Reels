# Quran Reels Studio — استوديو ريلز القرآن

> Create beautiful Quran recitation reels on Android — pick the verses, pick a reciter, pick a background, and export a vertical video ready to share.
>
> أنشئ مقاطع قرآنية جميلة على أندرويد — اختر الآيات، واختر القارئ، واختر الخلفية، وصدّر فيديو عمودي جاهز للمشاركة.

![Android](https://img.shields.io/badge/Platform-Android-green)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)
![Min SDK 24](https://img.shields.io/badge/minSdk-24-blue)
![License MIT](https://img.shields.io/badge/License-MIT-yellow)

---

## About — نبذة عن التطبيق

**Quran Reels Studio** is a native Android app (Kotlin + Jetpack Compose) for creating short vertical Quran recitation videos — "reels" — with synchronized audio, elegant Arabic calligraphy, and cinematic backgrounds.

**استوديو ريلز القرآن** تطبيق أندرويد أصلي (Kotlin + Jetpack Compose) لإنشاء مقاطع قرآنية قصيرة عمودية (ريلز) بصوت التلاوة، وخط عربي أنيق، وخلفيات سينمائية.

## Features — المميزات

- **114 Surahs + 24+ curated passages** — browse, search, and select any ayah range.
  **١١٤ سورة + أكثر من ٢٤ مقطعًا مختارًا** — تصفح وابحث وحدد أي نطاق آيات.
- **20 reciters** — streaming recitation audio from EveryAyah (Abdul Basit, Mishary Alafasy, Maher Al Mu'aiqly, As-Sudais, and more).
  **٢٠ قارئًا** — صوت التلاوة من EveryAyah (عبد الباسط، مشاري العفاسي، ماهر المعيقلي، السديس، وغيرهم).
- **Translations in 8 languages** — English, Arabic, German, Spanish, French, Indonesian, Russian, Turkish, Urdu (via AlQuran Cloud API).
  **ترجمة بـ ٨ لغات** — إنجليزي، عربي، ألماني، إسباني، فرنسي، إندونيسي، روسي، تركي، أردو.
- **Cinematic backgrounds** — bundled nature videos & photos, import your own media, or generate AI backgrounds.
  **خلفيات سينمائية** — فيديوهات وصور طبيعة مدمجة، أو استورد وسائطك، أو ولّد خلفية بالذكاء الاصطناعي.
- **25 Arabic fonts** — Amiri, Cairo, Tajawal, Scheherazade New, Noto Naskh/Kufi, and more.
  **٢٥ خطًا عربيًا** — أميري، القاهرة، تجوّل، Scheherazade New، نسخ وكوفي، وغيرها.
- **Full Uthmani text bundled** — the complete Quran text ships inside the app, offline.
  **النص العثماني كاملًا داخل التطبيق** — يعمل دون إنترنت.
- **Vertical video export** — renders the reel (video + recitation audio + verse overlays) ready for TikTok / Reels / Shorts.
  **تصدير فيديو عمودي** — يركّب المقطع (فيديو + صوت + الآيات) جاهزًا لتيك توك وريلز وشورتس.
- **Multi-language UI** — Arabic, English, German, Spanish, French, Hindi, Turkish, Urdu.
  **واجهة بعدة لغات** — عربي، إنجليزي، ألماني، إسباني، فرنسي، هندي، تركي، أردو.

## Tech Stack — التقنيات

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose (BOM 2024.02.00), Material 3 |
| Audio / Video | Media3 ExoPlayer 1.3.1 |
| Networking | OkHttp 4.12, Coroutines |
| Build | Android Gradle Plugin 8.13.2, Java 17 |
| Min / Target SDK | 24 / 34 |

## Getting Started — البدء

### Requirements — المتطلبات

- Android Studio (latest stable) with SDK 34
- JDK 17

### Build — البناء

```bash
# Debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Install on a connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

أو افتح المشروع في Android Studio: **Build ← Build Bundle(s) / APK(s) ← Build APK(s)**.

## Project Structure — بنية المشروع

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

## Media Assets & Release APK — الوسائط وملف APK

The bundled background videos/photos and fonts are large binary assets, and the signed release APK (v28, ~160 MB) exceeds GitHub's 100 MB per-file limit for files *inside* a repository — so they are **not** stored in the repo itself. They are distributed as **release assets** instead:

الوسائط (فيديوهات/صور/خطوط) وملف APK الموقّع (v28، حوالي ١٦٠ ميجا) تتجاوز حد GitHub البالغ ١٠٠ ميجا للملفات *داخل* المستودع، لذا فهي **ليست** في السورس نفسه. بل تُوزَّع كمرفقات إصدار:

- **Release v28 — الإصدار (APK + full bundle):** https://github.com/ilcapitano4/Quran-Reels/releases/tag/v28
- **Mirror — نسخة احتياطية (Drive):** https://drive.google.com/file/d/1ayREpV83gefbxOccEYng4MO-uNXA1Rn5/view?usp=drivesdk

> Note: the bundled release APK is signed with the debug key and is provided for testing/smoothness evaluation only.
> ملاحظة: نسخة APK المرفقة موقّعة بمفتاح التصحيح (debug) ومخصصة للتجربة وتقييم الأداء فقط.

## Data Sources & Credits — المصادر والشكر

- Quran text & translations: [AlQuran Cloud API](https://alquran.cloud/api) — `api.alquran.cloud`
- Recitation audio streams: [EveryAyah](https://everyayah.com)
- AI background images: [Pollinations.ai](https://image.pollinations.ai)
- Arabic fonts: Amiri, Cairo, Tajawal, Noto, Scheherazade New, and others (see `res/font/`)

## Contributing — المساهمة

Issues and pull requests are welcome. If you add a feature, please keep the app offline-first: the Quran text must always work without internet.

نرحب بالاقتراحات والمساهمات. لو أضفت ميزة، حافظ على عمل التطبيق دون إنترنت: نص القرآن لازم يشتغل دائمًا بلا اتصال.

## License — الرخصة

MIT License — see [LICENSE](LICENSE).
