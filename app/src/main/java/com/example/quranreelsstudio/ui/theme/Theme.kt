package com.example.quranreelsstudio.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun lightScheme(
    background: androidx.compose.ui.graphics.Color,
    surface: androidx.compose.ui.graphics.Color,
    surfaceHigh: androidx.compose.ui.graphics.Color,
    outline: androidx.compose.ui.graphics.Color,
    primary: androidx.compose.ui.graphics.Color,
    onPrimary: androidx.compose.ui.graphics.Color,
    container: androidx.compose.ui.graphics.Color,
    onContainer: androidx.compose.ui.graphics.Color,
    text: androidx.compose.ui.graphics.Color,
    textMuted: androidx.compose.ui.graphics.Color
): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    secondary = primary,
    onSecondary = onPrimary,
    background = background,
    onBackground = text,
    surface = surface,
    onSurface = text,
    surfaceVariant = surfaceHigh,
    onSurfaceVariant = textMuted,
    primaryContainer = container,
    onPrimaryContainer = onContainer,
    secondaryContainer = surfaceHigh,
    onSecondaryContainer = onContainer,
    outline = outline,
    error = androidx.compose.ui.graphics.Color(0xFFC0392B)
)

/** The six selectable app color themes. */
data class AppColorTheme(
    val id: String,
    val scheme: ColorScheme,
    /** Two swatch colors used in the picker UI. */
    val swatchBackground: androidx.compose.ui.graphics.Color,
    val swatchPrimary: androidx.compose.ui.graphics.Color
)

object AppColorThemes {
    val all = listOf(
        AppColorTheme(
            id = "rose",
            scheme = lightScheme(RoseBackground, RoseSurface, RoseSurfaceHigh, RoseOutline, RosePrimary, RoseOnPrimary, RoseContainer, RoseOnContainer, RoseText, RoseTextMuted),
            swatchBackground = RoseBackground,
            swatchPrimary = RosePrimary
        ),
        AppColorTheme(
            id = "navy",
            scheme = darkColorScheme(
                primary = NavyPrimary,
                onPrimary = NavyOnPrimary,
                secondary = NavyPrimary,
                onSecondary = NavyOnPrimary,
                background = NavyBackground,
                onBackground = NavyText,
                surface = NavySurface,
                onSurface = NavyText,
                surfaceVariant = NavySurfaceHigh,
                onSurfaceVariant = NavyTextMuted,
                primaryContainer = NavyContainer,
                onPrimaryContainer = NavyOnContainer,
                secondaryContainer = NavySurfaceHigh,
                onSecondaryContainer = NavyOnContainer,
                outline = NavyOutline,
                error = androidx.compose.ui.graphics.Color(0xFFFF7A6B)
            ),
            swatchBackground = NavyBackground,
            swatchPrimary = NavyPrimary
        ),
        AppColorTheme(
            id = "emerald",
            scheme = lightScheme(EmeraldBackground, EmeraldSurface, EmeraldSurfaceHigh, EmeraldOutline, EmeraldPrimary, EmeraldOnPrimary, EmeraldContainer, EmeraldOnContainer, EmeraldText, EmeraldTextMuted),
            swatchBackground = EmeraldBackground,
            swatchPrimary = EmeraldPrimary
        ),
        AppColorTheme(
            id = "purple",
            scheme = lightScheme(PurpleBackground, PurpleSurface, PurpleSurfaceHigh, PurpleOutline, PurplePrimary, PurpleOnPrimary, PurpleContainer, PurpleOnContainer, PurpleText, PurpleTextMuted),
            swatchBackground = PurpleBackground,
            swatchPrimary = PurplePrimary
        ),
        AppColorTheme(
            id = "gold",
            scheme = lightScheme(GoldBackground, GoldSurface, GoldSurfaceHigh, GoldOutline, GoldPrimary, GoldOnPrimary, GoldContainer, GoldOnContainer, GoldText, GoldTextMuted),
            swatchBackground = GoldBackground,
            swatchPrimary = GoldPrimary
        ),
        AppColorTheme(
            id = "slate",
            scheme = lightScheme(SlateBackground, SlateSurface, SlateSurfaceHigh, SlateOutline, SlatePrimary, SlateOnPrimary, SlateContainer, SlateOnContainer, SlateText, SlateTextMuted),
            swatchBackground = SlateBackground,
            swatchPrimary = SlatePrimary
        )
    )

    fun byId(id: String): AppColorTheme = all.firstOrNull { it.id == id } ?: all.first()
}

/** Persists the user's chosen app color theme. */
class AppColorPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("app_color_prefs", Context.MODE_PRIVATE)
    fun get(): String = prefs.getString(KEY_THEME, "rose") ?: "rose"
    fun save(id: String) = prefs.edit().putString(KEY_THEME, id).apply()
    private companion object { const val KEY_THEME = "theme_id" }
}

private val QuranAppTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp)
)

@Composable
fun QuranReelsStudioTheme(themeId: String = "rose", content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorThemes.byId(themeId).scheme,
        typography = QuranAppTypography,
        content = content
    )
}
