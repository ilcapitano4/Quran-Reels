package com.example.quranreelsstudio

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.example.quranreelsstudio.data.AppLanguage
import com.example.quranreelsstudio.data.LanguagePreferences
import com.example.quranreelsstudio.export.RenderSpec
import com.example.quranreelsstudio.export.VideoRenderEngine
import com.example.quranreelsstudio.ui.export.ExportResultScreen
import com.example.quranreelsstudio.ui.export.ExportSettingsScreen
import com.example.quranreelsstudio.ui.export.PendingRender
import com.example.quranreelsstudio.ui.export.RenderUiState
import com.example.quranreelsstudio.ui.onboarding.LanguageSelectionScreen
import com.example.quranreelsstudio.ui.studio.StudioScreen
import com.example.quranreelsstudio.ui.theme.AppColorPrefs
import com.example.quranreelsstudio.ui.theme.AppColorThemes
import com.example.quranreelsstudio.ui.theme.QuranReelsStudioTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed class Screen {
    data object Splash : Screen()
    data object Language : Screen()
    data object Studio : Screen()
    data object ExportSettings : Screen()
    data object ExportResult : Screen()
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val languagePreferences = LanguagePreferences(this)
        languagePreferences.getSavedLanguageCode()?.let { code ->
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
        }
        val colorPrefs = AppColorPrefs(this)

        setContent {
            var themeId by remember { mutableStateOf(colorPrefs.get()) }
            var savedLanguageCode by remember {
                mutableStateOf(languagePreferences.getSavedLanguageCode())
            }
            var screen by remember { mutableStateOf<Screen>(Screen.Splash) }
            var pendingRender by remember { mutableStateOf<PendingRender?>(null) }
            var renderState by remember { mutableStateOf(RenderUiState()) }
            val scope = rememberCoroutineScope()
            val engine = remember { VideoRenderEngine(this) }

            QuranReelsStudioTheme(themeId = themeId) {
                fun startRender(qualityP: Int) {
                    val pending = pendingRender ?: return
                    renderState = RenderUiState(running = true, label = "Starting…", percent = 0)
                    scope.launch {
                        val spec = RenderSpec(
                            qualityP = qualityP,
                            aspect = pending.aspect,
                            surah = pending.surah,
                            fromAyah = pending.fromAyah,
                            toAyah = pending.toAyah,
                            reciter = pending.reciter,
                            backgroundType = pending.backgroundType,
                            videoItems = pending.videoItems,
                            photoItem = pending.photoItem,
                            blankColor = pending.blankColor,
                            visibility = pending.visibility,
                            arabicTexts = pending.arabicTexts,
                            translationTexts = pending.translationTexts,
                            typeface = pending.typeface,
                            subtitleSize = pending.subtitleSize
                        )
                        engine.render(spec) { label, percent ->
                            renderState = renderState.copy(label = label, percent = percent)
                        }.fold(
                            onSuccess = { path ->
                                renderState = RenderUiState(outputPath = path, label = "Done", percent = 100)
                                screen = Screen.ExportResult
                            },
                            onFailure = { error ->
                                renderState = RenderUiState(error = error.message ?: "Unknown error")
                            }
                        )
                    }
                }

                // Animated screen transitions (fade + gentle slide up)
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(280)) +
                            slideInVertically(animationSpec = tween(280)) { it / 12 }) togetherWith
                            (fadeOut(animationSpec = tween(200)))
                    },
                    label = "screenTransition"
                ) { currentScreen ->
                    when {
                        // ---- Splash: app name + 2s loading line ----
                        currentScreen == Screen.Splash -> {
                            SplashPage()
                            LaunchedEffect(Unit) {
                                delay(2000)
                                screen = if (savedLanguageCode == null) Screen.Language else Screen.Studio
                            }
                        }

                        // ---- Language picker (first launch or from menu) ----
                        currentScreen == Screen.Language -> {
                            LanguageSelectionScreen(
                                showBack = savedLanguageCode != null,
                                onBack = { screen = Screen.Studio },
                                onLanguageConfirmed = { language: AppLanguage ->
                                    languagePreferences.saveLanguageCode(language.code)
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(language.code)
                                    )
                                    savedLanguageCode = language.code
                                    screen = Screen.Studio
                                }
                            )
                        }

                        currentScreen == Screen.Studio -> {
                            StudioScreen(
                                onOpenLanguages = { screen = Screen.Language },
                                appColorId = themeId,
                                onAppColorChange = { id ->
                                    colorPrefs.save(id)
                                    themeId = id
                                },
                                onRenderClick = { pending ->
                                    pendingRender = pending
                                    renderState = RenderUiState()
                                    screen = Screen.ExportSettings
                                }
                            )
                        }

                        currentScreen == Screen.ExportSettings -> {
                            val pending = pendingRender
                            if (pending == null) {
                                screen = Screen.Studio
                            } else {
                                ExportSettingsScreen(
                                    pending = pending,
                                    uiState = renderState,
                                    onStartRender = { quality -> startRender(quality) },
                                    onBack = {
                                        if (!renderState.running) screen = Screen.Studio
                                    }
                                )
                            }
                        }

                        else -> {
                        val outputPath = renderState.outputPath
                        if (outputPath == null) {
                            screen = Screen.Studio
                        } else {
                            ExportResultScreen(
                                outputPath = outputPath,
                                onBackToStudio = {
                                    screen = Screen.Studio
                                    renderState = RenderUiState()
                                }
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun SplashPage() {
    val transition = rememberInfiniteTransition(label = "splash")
    val slide by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "slide"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "alpha"
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier
                    .width(120.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .graphicsLayer { this.alpha = alpha }
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily(Font(R.font.font_el_messiri)),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))
            // Modern loading line: a glowing pill sliding across a rounded track
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val trackWidth = 200.dp
                val pillWidth = 56.dp
                Box(
                    modifier = Modifier
                        .width(trackWidth)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Box(
                    modifier = Modifier
                        .offset(x = (trackWidth - pillWidth) * slide)
                        .width(pillWidth)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                )
            }
        }
    }
}
