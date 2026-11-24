package com.example.quranreelsstudio.ui.studio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.example.quranreelsstudio.R
import com.example.quranreelsstudio.audio.PlaybackState
import com.example.quranreelsstudio.audio.RecitationPlayer
import com.example.quranreelsstudio.data.*
import com.example.quranreelsstudio.data.StudioSessionStore
import com.example.quranreelsstudio.ui.export.PendingRender
import com.example.quranreelsstudio.ui.theme.AppColorThemes
import com.example.quranreelsstudio.media.MediaLoader
import com.example.quranreelsstudio.media.MediaThumbs
import com.example.quranreelsstudio.network.AiBackgroundGenerator
import com.example.quranreelsstudio.network.TranslationRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class StudioTab(val labelRes: Int) {
    VERSES(R.string.tab_verses),
    RECITERS(R.string.tab_reciters),
    BACKGROUND(R.string.tab_background),
    STYLE(R.string.tab_style)
}

sealed interface TranslationState {
    object Idle : TranslationState
    object Loading : TranslationState
    data class Loaded(val ayahs: List<AyahText>) : TranslationState
    data class Error(val message: String) : TranslationState
}

private val TitleFont = FontFamily(Font(R.font.font_el_messiri))

private fun themeNameRes(id: String): Int = when (id) {
    "rose" -> R.string.theme_rose
    "navy" -> R.string.theme_navy
    "emerald" -> R.string.theme_emerald
    "purple" -> R.string.theme_purple
    "gold" -> R.string.theme_gold
    else -> R.string.theme_slate
}

@Composable
fun StudioScreen(
    onOpenLanguages: () -> Unit,
    appColorId: String,
    onAppColorChange: (String) -> Unit,
    onRenderClick: (PendingRender) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val quranRepository = remember { QuranTextRepository(context) }
    val aiStore = remember { AiLibraryStore(context) }
    val player = remember { RecitationPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    // Restore the previous session (surah, background, style...) on open.
    val sessionStore = remember { StudioSessionStore(context) }
    val restored = remember { sessionStore.load() }

    var aspectRatio by remember {
        mutableStateOf(ReelAspectRatio.values().firstOrNull { it.name == (restored?.aspect ?: "REEL_9_16") } ?: ReelAspectRatio.REEL_9_16)
    }
    var activeTab by remember { mutableStateOf(StudioTab.VERSES) }
    var showMenu by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Verses & text state
    var selectedSurah by remember {
        mutableStateOf(QuranSurahs.all.firstOrNull { it.number == (restored?.surahNumber ?: 1) } ?: QuranSurahs.all.first())
    }
    var isBrowsingDirectory by remember { mutableStateOf(false) }
    var surahSearch by remember { mutableStateOf("") }
    var showCuratedOnly by remember { mutableStateOf(false) }
    var fromAyah by remember { mutableStateOf(restored?.fromAyah ?: 1) }
    var toAyah by remember { mutableStateOf(restored?.toAyah ?: (selectedSurah?.verseCount ?: 1)) }
    var wantsTranslation by remember { mutableStateOf(restored?.wantsTranslation ?: false) }
    var translationCode by remember { mutableStateOf(restored?.translationCode) }
    var selectedFontId by remember { mutableStateOf(restored?.fontId ?: "amiri") }
    var subtitleSize by remember { mutableStateOf(restored?.subtitleSize ?: 14) }

    // Real verse text (bundled Uthmani script, offline)
    var arabicAyahs by remember { mutableStateOf<List<AyahText>>(emptyList()) }
    var isLoadingText by remember { mutableStateOf(true) }
    LaunchedEffect(selectedSurah?.number) {
        val surahNumber = selectedSurah?.number ?: return@LaunchedEffect
        isLoadingText = true
        arabicAyahs = emptyList()
        arabicAyahs = quranRepository.surahAyahs(surahNumber)
        isLoadingText = false
    }

    // Translation (fetched live from api.alquran.cloud, cached per surah+edition)
    var translationState by remember { mutableStateOf<TranslationState>(TranslationState.Idle) }
    var translationReloadKey by remember { mutableStateOf(0) }
    LaunchedEffect(selectedSurah?.number, translationCode, wantsTranslation, translationReloadKey) {
        val surahNumber = selectedSurah?.number
        if (!wantsTranslation || translationCode == null || surahNumber == null) {
            translationState = TranslationState.Idle
            return@LaunchedEffect
        }
        val edition = TranslationLanguages.all.firstOrNull { it.code == translationCode }?.edition
            ?: return@LaunchedEffect
        translationState = TranslationState.Loading
        translationState = TranslationRepository.fetch(edition, surahNumber)
            .fold(
                onSuccess = { TranslationState.Loaded(it) },
                onFailure = { TranslationState.Error(it.message ?: "Network error") }
            )
    }

    // Reciter state
    var selectedReciter by remember {
        mutableStateOf(Reciters.all.firstOrNull { it.id == (restored?.reciterId ?: 1) } ?: Reciters.all.first())
    }

    // Background state
    var backgroundType by remember {
        mutableStateOf(BackgroundType.values().firstOrNull { it.name == (restored?.backgroundType ?: "MOTION_VIDEO") } ?: BackgroundType.MOTION_VIDEO)
    }
    var mediaSourceTab by remember { mutableStateOf(MediaSourceTab.APP_LIBRARY) }
    var selectedVideoItem by remember {
        mutableStateOf<BackgroundMediaItem?>(BackgroundLibrary.videos.firstOrNull { it.id == (restored?.videoId ?: 1) } ?: BackgroundLibrary.videos.first())
    }
    var selectedPhotoItem by remember {
        mutableStateOf<BackgroundMediaItem?>(BackgroundLibrary.photos.firstOrNull { it.id == (restored?.photoId ?: 101) } ?: BackgroundLibrary.photos.first())
    }
    var selectedBlankColor by remember {
        mutableStateOf(BackgroundLibrary.blankColors.firstOrNull { it == (restored?.blankColor ?: 0xFFFFFFFFL) } ?: BackgroundLibrary.blankColors.first())
    }
    var visibility by remember { mutableStateOf(restored?.visibility ?: 65) }
    var playbackMode by remember { mutableStateOf(VideoPlaybackMode.SINGLE_LOOP) }
    var sequenceItems by remember { mutableStateOf<List<BackgroundMediaItem>>(listOf(BackgroundLibrary.videos.first())) }
    val useSequence = playbackMode == VideoPlaybackMode.MULTI_SEQUENCE

    // Persist selections whenever they change, so the app reopens as left.
    LaunchedEffect(
        aspectRatio, selectedSurah?.number, fromAyah, toAyah, wantsTranslation, translationCode,
        selectedFontId, subtitleSize, selectedReciter?.id, backgroundType,
        selectedVideoItem?.id, selectedPhotoItem?.id, selectedBlankColor, visibility
    ) {
        val surah = selectedSurah ?: return@LaunchedEffect
        sessionStore.save(
            StudioSessionStore.Selection(
                aspect = aspectRatio.name,
                surahNumber = surah.number,
                fromAyah = fromAyah,
                toAyah = toAyah,
                wantsTranslation = wantsTranslation,
                translationCode = translationCode,
                fontId = selectedFontId,
                subtitleSize = subtitleSize,
                reciterId = selectedReciter?.id ?: 1,
                backgroundType = backgroundType.name,
                videoId = selectedVideoItem?.id ?: 1,
                photoId = selectedPhotoItem?.id ?: 101,
                blankColor = selectedBlankColor,
                visibility = visibility
            )
        )
    }

    // AI library (persistent)
    var aiItems by remember { mutableStateOf(aiStore.load()) }
    val aiMotionItems = aiItems.filter { it.motion }.map { aiStore.toMediaItem(it) }
    val aiPhotoItems = aiItems.filter { !it.motion }.map { aiStore.toMediaItem(it) }

    fun pickLibraryItem(item: BackgroundMediaItem) {
        if (item.isVideo) {
            selectedVideoItem = item
            backgroundType = BackgroundType.MOTION_VIDEO
        } else {
            selectedPhotoItem = item
            backgroundType = if (item.motion) BackgroundType.MOTION_VIDEO else BackgroundType.NATURAL_PHOTO
        }
    }

    fun deleteAiItem(entryId: Int) {
        aiStore.remove(entryId)
        aiItems = aiStore.load()
        if (selectedVideoItem?.id == entryId) selectedVideoItem = null
        if (selectedPhotoItem?.id == entryId) selectedPhotoItem = null
        sequenceItems = sequenceItems.filterNot { it.id == entryId }
    }

    var aiPrompt by remember { mutableStateOf("") }
    var isGeneratingAi by remember { mutableStateOf(false) }
    var aiStage by remember { mutableStateOf("") }
    var aiError by remember { mutableStateOf<String?>(null) }

    fun addAiEntry(path: String, motion: Boolean) {
        val entry = AiLibraryStore.Entry(
            id = path.hashCode(),
            title = "AI ${if (motion) "Motion" else "Photo"}: ${aiPrompt.trim().take(20)}",
            arabicTitle = if (motion) "مقطع فيديو مولّد بالذكاء الاصطناعي" else "صورة مولّدة بالذكاء الاصطناعي",
            path = path,
            motion = motion,
            createdAt = System.currentTimeMillis()
        )
        aiStore.add(entry)
        aiItems = aiStore.load()
        pickLibraryItem(aiStore.toMediaItem(entry))
        aiPrompt = ""
    }

    fun generateAiBackground(motion: Boolean) {
        val promptText = aiPrompt.trim()
        if (promptText.isEmpty() || isGeneratingAi) return
        isGeneratingAi = true
        aiError = null
        scope.launch {
            try {
                aiStage = if (motion) "Generating AI image…" else ""
                val result = AiBackgroundGenerator.generateImage(context, promptText, 720, 1280)
                result.fold(
                    onSuccess = { imagePath ->
                        if (!motion) {
                            addAiEntry(imagePath, motion = false)
                        } else {
                            // Turn the AI image into a real 5-second MP4 clip
                            aiStage = "Encoding 5-second video…"
                            val source = android.graphics.BitmapFactory.decodeFile(imagePath)
                            if (source == null) {
                                aiError = "Could not process the AI image"
                            } else {
                                val dir = java.io.File(context.filesDir, "ai_motion")
                                dir.mkdirs()
                                val clipFile = java.io.File(dir, "ai_motion_${System.currentTimeMillis()}.mp4")
                                val encoded = com.example.quranreelsstudio.export.MotionClipEncoder.encode(
                                    source, clipFile, 720, 1280, 5000, 24
                                )
                                encoded.fold(
                                    onSuccess = {
                                        addAiEntry(clipFile.absolutePath, motion = true)
                                    },
                                    onFailure = { aiError = it.message ?: "Encoding failed" }
                                )
                                source.recycle()
                                java.io.File(imagePath).delete()
                            }
                        }
                    },
                    onFailure = { aiError = it.message ?: "Generation failed" }
                )
            } finally {
                isGeneratingAi = false
                aiStage = ""
            }
        }
    }

    var isImporting by remember { mutableStateOf(false) }

    fun importPickedMedia(uri: Uri?, isVideo: Boolean) {
        if (uri == null || isImporting) return
        isImporting = true
        scope.launch {
            val path = MediaLoader.importMedia(context, uri, isVideo)
            isImporting = false
            if (path != null) {
                pickLibraryItem(
                    BackgroundMediaItem(
                        id = path.hashCode(),
                        title = if (isVideo) "Imported Video" else "Imported Photo",
                        arabicTitle = if (isVideo) "فيديو مستورد" else "صورة مستوردة",
                        category = "Device",
                        isVideo = isVideo,
                        contentUri = "file://$path"
                    )
                )
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        importPickedMedia(uri, isVideo = true)
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        importPickedMedia(uri, isVideo = false)
    }

    val playback by player.state.collectAsState()
    var lastPlayedConfig by remember { mutableStateOf("") }
    val playbackConfig = "${selectedSurah?.number}:${selectedReciter?.id}:$fromAyah-$toAyah"

    // The preview background video only moves when the USER presses Play —
    // it mirrors the recitation playback state.
    var previewMediaPlaying by remember { mutableStateOf(false) }
    var wasRecitationActive by remember { mutableStateOf(false) }
    LaunchedEffect(playback.isPlaying, playback.isBuffering) {
        val nowActive = playback.isPlaying || playback.isBuffering
        if (wasRecitationActive && !nowActive) previewMediaPlaying = false
        wasRecitationActive = nowActive
    }

    fun startPlayback() {
        val surah = selectedSurah ?: return
        val reciter = selectedReciter ?: return
        lastPlayedConfig = playbackConfig
        player.playRange(reciter, surah, fromAyah, toAyah)
    }

    LaunchedEffect(playbackConfig) {
        val active = playback.isPlaying || playback.isBuffering
        if (active && playbackConfig != lastPlayedConfig) startPlayback()
    }

    val rangeAyahs = remember(arabicAyahs, fromAyah, toAyah) {
        arabicAyahs.filter { it.numberInSurah in fromAyah..toAyah }
    }

    val selectedTypeface = remember(selectedFontId) {
        val font = ArabicFonts.byId(selectedFontId)
        if (font.resId != 0) ResourcesCompat.getFont(context, font.resId) else null
    }
    val selectedFontFamily = remember(selectedFontId) {
        val font = ArabicFonts.byId(selectedFontId)
        if (font.resId != 0) FontFamily(Font(font.resId)) else FontFamily.Default
    }

    // Pause the looping preview video while it is scrolled off-screen —
    // a hidden 720p video decode running during scroll was the main lag source.
    val listState = rememberLazyListState()
    val previewVisible by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.any { it.index == 1 } }
    }

    // Warm the thumbnail cache in the background when the user opens the
    // Background tab, so scrolling the library never blocks on decodes.
    LaunchedEffect(activeTab) {
        if (activeTab == StudioTab.BACKGROUND) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                (BackgroundLibrary.videos + BackgroundLibrary.photos + aiItems.map { aiStore.toMediaItem(it) })
                    .forEach { MediaThumbs.get(context, it) }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TopBar(
                    aspectRatio = aspectRatio,
                    onAspectRatioChange = { aspectRatio = it },
                    onMenuClick = { showMenu = true }
                )
            }

            item {
                val activeIndex = if (playback.isPlaying || playback.isBuffering) {
                    playback.currentAyahIndex.coerceAtLeast(0)
                } else 0
                val displayAyah = rangeAyahs.getOrNull(activeIndex) ?: rangeAyahs.firstOrNull()
                val displayTranslation = (translationState as? TranslationState.Loaded)?.ayahs
                    ?.firstOrNull { it.numberInSurah == displayAyah?.numberInSurah }?.text

                Column {
                    Text(
                        text = stringResource(R.string.live_preview_fmt, aspectRatio.label.uppercase()),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    ReelPreview(
                        aspectRatio = aspectRatio,
                        backgroundType = backgroundType,
                        backgroundItem = when (backgroundType) {
                            BackgroundType.MOTION_VIDEO -> selectedVideoItem
                            BackgroundType.NATURAL_PHOTO -> selectedPhotoItem
                            BackgroundType.BLANK_COLOR -> null
                        },
                        sequenceItems = sequenceItems,
                        useSequence = useSequence,
                        blankColor = selectedBlankColor,
                        visibility = visibility,
                        surah = selectedSurah,
                        fromAyah = fromAyah,
                        toAyah = toAyah,
                        displayAyah = displayAyah,
                        displayTranslation = displayTranslation,
                        isLoadingText = isLoadingText,
                        playback = playback,
                        arabicFontFamily = selectedFontFamily,
                        subtitleSize = subtitleSize,
                        reciterName = selectedReciter?.nameAr ?: selectedReciter?.name,
                        playVideo = previewMediaPlaying && previewVisible,
                        ayahNumber = displayAyah?.numberInSurah ?: 0,
                        onTogglePlay = {
                            when {
                                playback.isPlaying -> {
                                    player.pause()
                                    previewMediaPlaying = false
                                }
                                playback.isBuffering -> {
                                    player.stop()
                                    previewMediaPlaying = false
                                }
                                playback.error != null -> {
                                    startPlayback()
                                    previewMediaPlaying = true
                                }
                                playbackConfig == lastPlayedConfig && playback.totalAyahs > 0 -> {
                                    player.resume()
                                    previewMediaPlaying = true
                                }
                                else -> {
                                    startPlayback()
                                    previewMediaPlaying = true
                                }
                            }
                        },
                        onSeekToVerse = { verseIndex ->
                            player.seekToAyahIndex(verseIndex)
                        }
                    )
                }
            }

            item {
                StudioTabsHeader(activeTab = activeTab, onTabSelected = { activeTab = it })
            }

            item {
                // Animated crossfade between tab contents
                androidx.compose.animation.AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(250)) +
                            androidx.compose.animation.slideInVertically(
                                androidx.compose.animation.core.tween(250)
                            ) { it / 14 }) togetherWith
                            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180))
                    },
                    label = "tabTransition"
                ) { tab ->
                    when (tab) {
                    StudioTab.VERSES -> VersesTabContent(
                        selectedSurah = selectedSurah,
                        isBrowsingDirectory = isBrowsingDirectory,
                        onBrowseToggle = { isBrowsingDirectory = it },
                        searchQuery = surahSearch,
                        onSearchQueryChange = { surahSearch = it },
                        showCuratedOnly = showCuratedOnly,
                        onFilterChange = { showCuratedOnly = it },
                        onSurahPicked = { surah ->
                            selectedSurah = surah
                            fromAyah = 1
                            toAyah = surah.verseCount
                            isBrowsingDirectory = false
                        },
                        fromAyah = fromAyah,
                        toAyah = toAyah,
                        onFromChange = { newValue ->
                            fromAyah = newValue
                            if (toAyah < fromAyah) toAyah = fromAyah
                        },
                        onToChange = { toAyah = it },
                        wantsTranslation = wantsTranslation,
                        onWantsTranslationChange = {
                            wantsTranslation = it
                            if (!it) translationCode = null
                        },
                        translationCode = translationCode,
                        onTranslationCodeChange = { translationCode = it },
                        isLoadingText = isLoadingText,
                        rangeAyahs = rangeAyahs,
                        translationState = translationState,
                        onRetryTranslation = { translationReloadKey++ }
                    )
                    StudioTab.RECITERS -> RecitersTabContent(
                        selectedReciter = selectedReciter,
                        onReciterPicked = { selectedReciter = it }
                    )
                    StudioTab.BACKGROUND -> BackgroundTabContent(
                        backgroundType = backgroundType,
                        onBackgroundTypeChange = { type ->
                            backgroundType = type
                            mediaSourceTab = MediaSourceTab.APP_LIBRARY
                        },
                        mediaSourceTab = mediaSourceTab,
                        onMediaSourceTabChange = { mediaSourceTab = it },
                        selectedVideoItem = selectedVideoItem,
                        selectedPhotoItem = selectedPhotoItem,
                        onLibraryItemPicked = { pickLibraryItem(it) },
                        aiMotionItems = aiMotionItems,
                        aiPhotoItems = aiPhotoItems,
                        onAiItemDelete = { deleteAiItem(it) },
                        onImportVideoClick = { videoPicker.launch("video/*") },
                        onImportPhotoClick = { photoPicker.launch("image/*") },
                        isImporting = isImporting,
                        aiPrompt = aiPrompt,
                        onAiPromptChange = { aiPrompt = it },
                        aiStage = aiStage,
                        isGeneratingAi = isGeneratingAi,
                        aiError = aiError,
                        onGenerateAiMotion = { generateAiBackground(motion = true) },
                        onGenerateAiPhoto = { generateAiBackground(motion = false) },
                        selectedBlankColor = selectedBlankColor,
                        onBlankColorPicked = { selectedBlankColor = it },
                        visibility = visibility,
                        onVisibilityChange = { visibility = it },
                        playbackMode = playbackMode,
                        onPlaybackModeChange = { playbackMode = it },
                        sequenceItems = sequenceItems,
                        onSequenceAdd = { video ->
                            if (sequenceItems.none { it.id == video.id }) {
                                sequenceItems = sequenceItems + video
                            }
                        },
                        onSequenceRemove = { video ->
                            sequenceItems = sequenceItems.filterNot { it.id == video.id }
                        }
                    )
                    StudioTab.STYLE -> StyleTabContent(
                        selectedFontId = selectedFontId,
                        onFontChange = { selectedFontId = it },
                        subtitleSize = subtitleSize,
                        onSubtitleSizeChange = { subtitleSize = it }
                    )
                    }
                }
            }

            item {
                ExportSection(
                    onRenderClick = {
                        val surah = selectedSurah ?: return@ExportSection
                        val reciter = selectedReciter ?: Reciters.all.first()
                        val translationTexts = (translationState as? TranslationState.Loaded)?.ayahs
                            ?.filter { it.numberInSurah in fromAyah..toAyah }
                            ?.map { it.text }
                        val videos = if (useSequence && sequenceItems.isNotEmpty()) {
                            sequenceItems
                        } else {
                            listOfNotNull(selectedVideoItem)
                        }
                        onRenderClick(
                            PendingRender(
                                aspect = aspectRatio,
                                surah = surah,
                                fromAyah = fromAyah,
                                toAyah = toAyah,
                                reciter = reciter,
                                backgroundType = backgroundType,
                                videoItems = videos,
                                photoItem = selectedPhotoItem?.takeIf { backgroundType == BackgroundType.NATURAL_PHOTO },
                                blankColor = selectedBlankColor,
                                visibility = visibility,
                                arabicTexts = rangeAyahs.map { it.text },
                                translationTexts = translationTexts,
                                typeface = selectedTypeface,
                                subtitleSize = subtitleSize
                            )
                        )
                    }
                )
            }
        }
    }

    // ----- App menu -----
    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            confirmButton = {
                TextButton(onClick = { showMenu = false }) { Text(stringResource(R.string.close_label)) }
            },
            title = { Text(stringResource(R.string.app_name), fontFamily = TitleFont, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    MenuItem(stringResource(R.string.menu_languages)) {
                        showMenu = false
                        onOpenLanguages()
                    }
                    MenuItem(stringResource(R.string.menu_app_color)) {
                        showMenu = false
                        showColorDialog = true
                    }
                    MenuItem(stringResource(R.string.menu_about)) {
                        showMenu = false
                        showAboutDialog = true
                    }
                }
            }
        )
    }

    // ----- App color picker -----
    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            confirmButton = {
                TextButton(onClick = { showColorDialog = false }) { Text(stringResource(R.string.ok)) }
            },
            title = { Text(stringResource(R.string.choose_app_color)) },
            text = {
                Column {
                    AppColorThemes.all.forEach { theme ->
                        val isSelected = theme.id == appColorId
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onAppColorChange(theme.id)
                                    showColorDialog = false
                                }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(theme.swatchBackground)
                                    .border(width = 1.5.dp, color = MaterialTheme.colorScheme.outline, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(theme.swatchPrimary)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(themeNameRes(theme.id)),
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        )
    }

    // ----- About the developer -----
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text(stringResource(R.string.ok)) }
            },
            title = { Text(stringResource(R.string.menu_about)) },
            text = { Text(stringResource(R.string.about_content)) }
        )
    }
}

private fun toArabicDigits(n: Int): String = buildString {
    for (c in n.toString()) {
        append(
            when (c) {
                '0' -> '٠'; '1' -> '١'; '2' -> '٢'; '3' -> '٣'; '4' -> '٤'
                '5' -> '٥'; '6' -> '٦'; '7' -> '٧'; '8' -> '٨'; '9' -> '٩'
                else -> c
            }
        )
    }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(12.dp)
    )
}

// ---------- Top bar ----------

@Composable
private fun TopBar(
    aspectRatio: ReelAspectRatio,
    onAspectRatioChange: (ReelAspectRatio) -> Unit,
    onMenuClick: () -> Unit
) {
    Column {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onMenuClick, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = TitleFont,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReelAspectRatio.values().forEach { ratio ->
                val isSelected = ratio == aspectRatio
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onAspectRatioChange(ratio) }
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(ratio.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    Text(ratio.dimensions, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ---------- Reel preview ----------

@Composable
private fun ReelPreview(
    aspectRatio: ReelAspectRatio,
    backgroundType: BackgroundType,
    backgroundItem: BackgroundMediaItem?,
    sequenceItems: List<BackgroundMediaItem>,
    useSequence: Boolean,
    blankColor: Long,
    visibility: Int,
    surah: Surah?,
    fromAyah: Int,
    toAyah: Int,
    displayAyah: AyahText?,
    displayTranslation: String?,
    isLoadingText: Boolean,
    playback: PlaybackState,
    arabicFontFamily: FontFamily,
    subtitleSize: Int,
    reciterName: String?,
    playVideo: Boolean,
    ayahNumber: Int,
    onTogglePlay: () -> Unit,
    onSeekToVerse: (Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val maxW = maxWidth
        val targetHeight = minOf(maxW * aspectRatio.height / aspectRatio.width, 560.dp)
        val targetWidth = targetHeight * aspectRatio.width / aspectRatio.height

        Box(
            modifier = Modifier
                .width(targetWidth)
                .height(targetHeight)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(24.dp))
        ) {
            BackgroundMediaCanvas(
                backgroundType = backgroundType,
                item = backgroundItem,
                sequenceItems = sequenceItems,
                useSequence = useSequence,
                blankColor = blankColor,
                visibility = visibility,
                playVideo = playVideo,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (surah != null) {
                    Text(
                        text = surah.englishName.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = surah.arabicName,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                when {
                    isLoadingText -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.loading_verses), color = Color.White, fontSize = 12.sp)
                    }
                    displayAyah != null -> {
                        Text(
                            text = displayAyah.text,
                            color = Color.White,
                            fontSize = 22.sp,
                            lineHeight = 42.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontFamily = arabicFontFamily,
                            style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.8f), blurRadius = 12f)),
                            modifier = Modifier.fillMaxWidth()
                        )
                        displayTranslation?.let {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = it,
                                color = Color(0xFFF2F7FC),
                                fontSize = subtitleSize.sp,
                                lineHeight = (subtitleSize * 1.4).sp,
                                textAlign = TextAlign.Center,
                                style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.8f), blurRadius = 10f)),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    else -> Text(
                        text = stringResource(R.string.select_range_hint),
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Text(
                                text = (reciterName ?: stringResource(R.string.no_reciter)).uppercase(),
                                color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold
                            )
                            Text(text = stringResource(R.string.preview_verses_fmt, fromAyah, toAyah), color = MaterialTheme.colorScheme.primary, fontSize = 9.sp)
                        }
                    }
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(58.dp)
                            .clip(RoundedCornerShape(29.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        if (playback.isBuffering) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(
                                imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playback.isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                if ((playback.isPlaying || playback.isBuffering) && playback.totalAyahs > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Scrubber: drag to jump between verses
                    var scrubValue by remember(playback.currentAyahIndex) {
                        mutableStateOf((playback.currentAyahIndex + 1f) / playback.totalAyahs)
                    }
                    Slider(
                        value = scrubValue,
                        onValueChange = { scrubValue = it },
                        onValueChangeFinished = {
                            val target = (scrubValue * playback.totalAyahs).roundToInt() - 1
                            onSeekToVerse(target.coerceIn(0, playback.totalAyahs - 1))
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.Black.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.verse_of_fmt, playback.currentAyahIndex + 1, playback.totalAyahs),
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                playback.error?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = Color(0xFFFFB4A9), fontSize = 11.sp)
                }
            }
        }
    }
}

// ---------- Tabs header ----------

@Composable
private fun StudioTabsHeader(activeTab: StudioTab, onTabSelected: (StudioTab) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.studio_controls),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(14.dp))
                .padding(4.dp)
        ) {
            StudioTab.values().forEach { tab ->
                val isSelected = tab == activeTab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = when (tab) {
                            StudioTab.VERSES -> Icons.Filled.MenuBook
                            StudioTab.RECITERS -> Icons.Filled.People
                            StudioTab.BACKGROUND -> Icons.Filled.Image
                            StudioTab.STYLE -> Icons.Filled.Palette
                        },
                        contentDescription = stringResource(tab.labelRes),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(tab.labelRes),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------- Verses tab ----------

@Composable
private fun VersesTabContent(
    selectedSurah: Surah?,
    isBrowsingDirectory: Boolean,
    onBrowseToggle: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showCuratedOnly: Boolean,
    onFilterChange: (Boolean) -> Unit,
    onSurahPicked: (Surah) -> Unit,
    fromAyah: Int,
    toAyah: Int,
    onFromChange: (Int) -> Unit,
    onToChange: (Int) -> Unit,
    wantsTranslation: Boolean,
    onWantsTranslationChange: (Boolean) -> Unit,
    translationCode: String?,
    onTranslationCodeChange: (String) -> Unit,
    isLoadingText: Boolean,
    rangeAyahs: List<AyahText>,
    translationState: TranslationState,
    onRetryTranslation: () -> Unit
) {
    Column {
        Text(stringResource(R.string.verses_section), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (selectedSurah != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${selectedSurah.number}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("${selectedSurah.englishName}  ${selectedSurah.arabicName}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "${if (selectedSurah.isMeccan) stringResource(R.string.meccan) else stringResource(R.string.medinan)} • ${stringResource(R.string.verses_count_fmt, selectedSurah.verseCount)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.place_revelation_fmt, if (selectedSurah.isMeccan) "Makkah" else "Madinah"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { onBrowseToggle(!isBrowsingDirectory) }) {
                    Text(
                        if (isBrowsingDirectory) stringResource(R.string.close_label) else stringResource(R.string.browse),
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (selectedSurah == null || isBrowsingDirectory) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showCuratedOnly,
                    onClick = { onFilterChange(false) },
                    label = { Text(stringResource(R.string.all_surahs)) }
                )
                FilterChip(
                    selected = showCuratedOnly,
                    onClick = { onFilterChange(true) },
                    label = { Text(stringResource(R.string.curated_passages)) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_hint), fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            val filtered = remember(searchQuery, showCuratedOnly) {
                QuranSurahs.all
                    .filter { !showCuratedOnly || it.isCurated }
                    .filter {
                        it.arabicName.contains(searchQuery.trim()) ||
                            it.englishName.contains(searchQuery.trim(), ignoreCase = true) ||
                            it.number.toString() == searchQuery.trim()
                    }
            }
            filtered.take(30).chunked(2).forEach { rowSurahs ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rowSurahs.forEach { surah ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onSurahPicked(surah) }
                                .padding(8.dp)
                        ) {
                            Text("${surah.number}.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(surah.arabicName, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text("${surah.englishName} · ${stringResource(R.string.verses_count_fmt, surah.verseCount)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                    if (rowSurahs.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        if (selectedSurah != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.ayah_range_title),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.verse_range_fmt, fromAyah, toAyah, toAyah - fromAyah + 1),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VerseStepper(
                    label = stringResource(R.string.start_verse),
                    value = fromAyah,
                    range = 1..selectedSurah.verseCount,
                    modifier = Modifier.weight(1f)
                ) { onFromChange(it) }
                VerseStepper(
                    label = stringResource(R.string.end_verse),
                    value = toAyah,
                    range = fromAyah..selectedSurah.verseCount,
                    modifier = Modifier.weight(1f)
                ) { onToChange(it) }
            }
            Slider(
                value = fromAyah.toFloat(),
                onValueChange = { onFromChange(it.toInt()) },
                valueRange = 1f..selectedSurah.verseCount.toFloat()
            )
            Slider(
                value = toAyah.toFloat(),
                onValueChange = { onToChange(it.toInt()) },
                valueRange = fromAyah.toFloat()..selectedSurah.verseCount.toFloat()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.add_translation_title), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
                    Text(
                        if (wantsTranslation) stringResource(R.string.yes_enabled) else stringResource(R.string.no_disabled),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (wantsTranslation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = wantsTranslation, onCheckedChange = onWantsTranslationChange)
            }
            if (wantsTranslation) {
                Spacer(modifier = Modifier.height(8.dp))
                TranslationLanguages.all.chunked(2).forEach { rowLangs ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        rowLangs.forEach { lang ->
                            val isSelected = translationCode == lang.code
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onTranslationCodeChange(lang.code) }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(lang.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    Text(lang.edition, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (rowLangs.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                when (val state = translationState) {
                    is TranslationState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.fetching_translation), style = MaterialTheme.typography.bodyMedium)
                    }
                    is TranslationState.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.translation_failed_fmt, state.message),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRetryTranslation) { Text(stringResource(R.string.retry)) }
                    }
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.loaded_verses_title), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(6.dp))
            when {
                isLoadingText -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.loading_verses), style = MaterialTheme.typography.bodyMedium)
                }
                rangeAyahs.isEmpty() -> Text(stringResource(R.string.no_verses_range), style = MaterialTheme.typography.bodyMedium)
                else -> {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rangeAyahs.forEach { ayah ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "Verse ${ayah.numberInSurah}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = ayah.text,
                                    fontSize = 20.sp,
                                    lineHeight = 34.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                (translationState as? TranslationState.Loaded)?.ayahs
                                    ?.firstOrNull { it.numberInSurah == ayah.numberInSurah }?.let { translated ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = translated.text,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseStepper(
    label: String,
    value: Int,
    range: IntRange,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { onChange((value - 1).coerceIn(range.first, range.last)) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(34.dp)
            ) { Text("−") }
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedTextField(
                value = value.toString(),
                onValueChange = { text -> text.toIntOrNull()?.let { onChange(it.coerceIn(range.first, range.last)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedButton(
                onClick = { onChange((value + 1).coerceIn(range.first, range.last)) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(34.dp)
            ) { Text("+") }
        }
    }
}

// ---------- Style & Theme tab ----------

@Composable
private fun StyleTabContent(
    selectedFontId: String,
    onFontChange: (String) -> Unit,
    subtitleSize: Int,
    onSubtitleSizeChange: (Int) -> Unit
) {
    Column {
        Text(stringResource(R.string.arabic_font_title), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
        Text(stringResource(R.string.arabic_font_hint), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ArabicFonts.all) { font ->
                val isSelected = font.id == selectedFontId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onFontChange(font.id) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = font.displayName,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = if (font.resId != 0) FontFamily(Font(font.resId)) else FontFamily.Default
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.subtitle_size_title), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.subtitle_size_fmt, subtitleSize), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = subtitleSize.toFloat(),
            onValueChange = { onSubtitleSizeChange(it.toInt()) },
            valueRange = 10f..24f
        )
        Text(stringResource(R.string.subtitle_hint), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------- Reciters tab ----------

@Composable
private fun RecitersTabContent(selectedReciter: Reciter?, onReciterPicked: (Reciter) -> Unit) {
    Column {
        Text(stringResource(R.string.recitation_section), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Reciters.all.forEach { reciter ->
            val isSelected = reciter.id == selectedReciter?.id
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onReciterPicked(reciter) }
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${reciter.nameAr}  •  ${reciter.name}",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                    Text(reciter.style, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.reciter_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- Background tab ----------

@Composable
private fun BackgroundTabContent(
    backgroundType: BackgroundType,
    onBackgroundTypeChange: (BackgroundType) -> Unit,
    mediaSourceTab: MediaSourceTab,
    onMediaSourceTabChange: (MediaSourceTab) -> Unit,
    selectedVideoItem: BackgroundMediaItem?,
    selectedPhotoItem: BackgroundMediaItem?,
    onLibraryItemPicked: (BackgroundMediaItem) -> Unit,
    aiMotionItems: List<BackgroundMediaItem>,
    aiPhotoItems: List<BackgroundMediaItem>,
    onAiItemDelete: (Int) -> Unit,
    onImportVideoClick: () -> Unit,
    onImportPhotoClick: () -> Unit,
    isImporting: Boolean,
    aiPrompt: String,
    onAiPromptChange: (String) -> Unit,
    aiStage: String,
    isGeneratingAi: Boolean,
    aiError: String?,
    onGenerateAiMotion: () -> Unit,
    onGenerateAiPhoto: () -> Unit,
    selectedBlankColor: Long,
    onBlankColorPicked: (Long) -> Unit,
    visibility: Int,
    onVisibilityChange: (Int) -> Unit,
    playbackMode: VideoPlaybackMode,
    onPlaybackModeChange: (VideoPlaybackMode) -> Unit,
    sequenceItems: List<BackgroundMediaItem>,
    onSequenceAdd: (BackgroundMediaItem) -> Unit,
    onSequenceRemove: (BackgroundMediaItem) -> Unit
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                BackgroundType.MOTION_VIDEO to stringResource(R.string.motion_video),
                BackgroundType.NATURAL_PHOTO to stringResource(R.string.natural_photos),
                BackgroundType.BLANK_COLOR to stringResource(R.string.blank_color)
            ).forEach { (type, label) ->
                FilterChip(selected = backgroundType == type, onClick = { onBackgroundTypeChange(type) }, label = { Text(label) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.visibility_title), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
        Text(stringResource(R.string.visibility_fmt, visibility), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Slider(
            value = visibility.toFloat(),
            onValueChange = { onVisibilityChange(it.toInt()) },
            valueRange = 0f..100f
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.dark_label), fontSize = 11.sp)
            Text(stringResource(R.string.balanced_label), fontSize = 11.sp)
            Text(stringResource(R.string.full_light), fontSize = 11.sp)
        }

        if (backgroundType == BackgroundType.MOTION_VIDEO) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.playback_mode_title), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = playbackMode == VideoPlaybackMode.SINGLE_LOOP,
                    onClick = { onPlaybackModeChange(VideoPlaybackMode.SINGLE_LOOP) },
                    label = { Text(stringResource(R.string.single_loop)) }
                )
                FilterChip(
                    selected = playbackMode == VideoPlaybackMode.MULTI_SEQUENCE,
                    onClick = { onPlaybackModeChange(VideoPlaybackMode.MULTI_SEQUENCE) },
                    label = { Text(stringResource(R.string.multi_sequence)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MediaSourceTab.values().forEach { tab ->
                    val label = when (tab) {
                        MediaSourceTab.APP_LIBRARY -> stringResource(R.string.app_library)
                        MediaSourceTab.IMPORT_DEVICE -> stringResource(R.string.import_tab)
                        MediaSourceTab.AI_GENERATOR -> stringResource(R.string.ai_generator_tab)
                    }
                    FilterChip(selected = mediaSourceTab == tab, onClick = { onMediaSourceTabChange(tab) }, label = { Text(label) })
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (mediaSourceTab) {
                MediaSourceTab.APP_LIBRARY -> {
                    if (playbackMode == VideoPlaybackMode.MULTI_SEQUENCE) {
                        Text(
                            stringResource(R.string.sequence_hint),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    BackgroundLibrary.videos.forEach { video ->
                        val isSelected = video.id == selectedVideoItem?.id
                        MediaRow(
                            item = video,
                            badge = video.category,
                            isSelected = isSelected,
                            onClick = { onLibraryItemPicked(video) },
                            trailing = {
                                if (playbackMode == VideoPlaybackMode.MULTI_SEQUENCE) {
                                    val inSequence = sequenceItems.any { it.id == video.id }
                                    IconButton(
                                        onClick = {
                                            if (inSequence) onSequenceRemove(video) else onSequenceAdd(video)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (inSequence) Icons.Filled.Check else Icons.Filled.Add,
                                            contentDescription = null,
                                            tint = if (inSequence) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )
                    }
                    aiMotionItems.forEach { item ->
                        MediaRow(
                            item = item,
                            badge = "AI Motion",
                            isSelected = item.id == selectedVideoItem?.id,
                            onClick = { onLibraryItemPicked(item) },
                            trailing = {
                                IconButton(onClick = { onAiItemDelete(item.id) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }

                    if (playbackMode == VideoPlaybackMode.MULTI_SEQUENCE) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(stringResource(R.string.sequence_title_fmt, sequenceItems.size), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (sequenceItems.isEmpty()) {
                            Text(stringResource(R.string.no_videos_yet), style = MaterialTheme.typography.bodyMedium)
                        } else {
                            sequenceItems.forEachIndexed { index, video ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        "${index + 1}.",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    MediaThumb(item = video, modifier = Modifier.size(width = 48.dp, height = 34.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(video.title, modifier = Modifier.weight(1f), maxLines = 1)
                                    IconButton(onClick = { onSequenceRemove(video) }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                MediaSourceTab.IMPORT_DEVICE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onImportVideoClick,
                            enabled = !isImporting,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.import_video)) }
                        OutlinedButton(
                            onClick = onImportPhotoClick,
                            enabled = !isImporting,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.import_photo)) }
                    }
                    if (isImporting) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.importing_media), style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.import_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                MediaSourceTab.AI_GENERATOR -> {
                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = onAiPromptChange,
                        label = { Text(stringResource(R.string.ai_prompt_motion_label)) },
                        placeholder = { Text(stringResource(R.string.prompt_hint_motion), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        enabled = !isGeneratingAi
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onGenerateAiMotion,
                        enabled = !isGeneratingAi && aiPrompt.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        if (isGeneratingAi) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(stringResource(R.string.generating))
                        } else {
                            Text(stringResource(R.string.generate_motion_btn), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (isGeneratingAi && aiStage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(aiStage, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    aiError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.generation_failed_fmt, it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (aiMotionItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(stringResource(R.string.created_motion_fmt, aiMotionItems.size), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        aiMotionItems.forEach { item ->
                            MediaRow(
                                item = item,
                                badge = "AI Motion",
                                isSelected = item.id == selectedVideoItem?.id,
                                onClick = { onLibraryItemPicked(item) },
                                trailing = {
                                    IconButton(onClick = { onAiItemDelete(item.id) }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Filled.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.ai_motion_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (backgroundType == BackgroundType.NATURAL_PHOTO) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    MediaSourceTab.APP_LIBRARY to stringResource(R.string.app_library),
                    MediaSourceTab.IMPORT_DEVICE to stringResource(R.string.import_tab),
                    MediaSourceTab.AI_GENERATOR to stringResource(R.string.ai_generator_tab)
                ).forEach { (tab, label) ->
                    FilterChip(selected = mediaSourceTab == tab, onClick = { onMediaSourceTabChange(tab) }, label = { Text(label) })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            when (mediaSourceTab) {
                MediaSourceTab.APP_LIBRARY -> {
                    Text(stringResource(R.string.photo_library_fmt, BackgroundLibrary.photos.size), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    BackgroundLibrary.photos.forEach { photo ->
                        MediaRow(
                            item = photo,
                            badge = photo.category,
                            isSelected = photo.id == selectedPhotoItem?.id,
                            onClick = { onLibraryItemPicked(photo) }
                        )
                    }
                    aiPhotoItems.forEach { item ->
                        MediaRow(
                            item = item,
                            badge = "AI Photo",
                            isSelected = item.id == selectedPhotoItem?.id,
                            onClick = { onLibraryItemPicked(item) },
                            trailing = {
                                IconButton(onClick = { onAiItemDelete(item.id) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }

                MediaSourceTab.IMPORT_DEVICE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onImportVideoClick, enabled = !isImporting, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.import_video))
                        }
                        Button(onClick = onImportPhotoClick, enabled = !isImporting, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.import_photo))
                        }
                    }
                    if (isImporting) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.importing_media), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                MediaSourceTab.AI_GENERATOR -> {
                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = onAiPromptChange,
                        label = { Text(stringResource(R.string.ai_prompt_photo_label)) },
                        placeholder = { Text(stringResource(R.string.prompt_hint_photo), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        enabled = !isGeneratingAi
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onGenerateAiPhoto,
                        enabled = !isGeneratingAi && aiPrompt.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        if (isGeneratingAi) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(stringResource(R.string.generating))
                        } else {
                            Text(stringResource(R.string.generate_photo_btn), fontWeight = FontWeight.Bold)
                        }
                    }
                    aiError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.generation_failed_fmt, it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (aiPhotoItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(stringResource(R.string.created_photos_fmt, aiPhotoItems.size), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        aiPhotoItems.forEach { item ->
                            MediaRow(
                                item = item,
                                badge = "AI Photo",
                                isSelected = item.id == selectedPhotoItem?.id,
                                onClick = { onLibraryItemPicked(item) },
                                trailing = {
                                    IconButton(onClick = { onAiItemDelete(item.id) }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Filled.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.ai_photo_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.blank_colors_fmt, BackgroundLibrary.blankColors.size), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(6.dp))
            BackgroundLibrary.blankColors.chunked(5).forEach { rowColors ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowColors.forEach { colorLong ->
                        val isSelected = colorLong == selectedBlankColor
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(colorLong))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onBlankColorPicked(colorLong) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun MediaRow(
    item: BackgroundMediaItem,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        MediaThumb(
            item = item,
            modifier = Modifier.size(width = 62.dp, height = 44.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
            Text(item.arabicTitle, style = MaterialTheme.typography.bodyMedium, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(badge, color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        trailing?.invoke(this)
    }
}

// ---------- Export ----------

@Composable
private fun ExportSection(onRenderClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.export_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.export_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRenderClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(stringResource(R.string.export_button), fontWeight = FontWeight.Bold)
        }
    }
}
