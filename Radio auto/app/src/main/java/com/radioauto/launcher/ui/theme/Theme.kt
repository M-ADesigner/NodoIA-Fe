package com.radioauto.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Typography
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import com.radioauto.launcher.data.FontChoice
import com.radioauto.launcher.ui.Hud
import com.radioauto.launcher.ui.family
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.radioauto.launcher.data.DayNightMode
import com.radioauto.launcher.data.Palette
import java.util.Calendar

private fun Palette.accent(night: Boolean): Color = if (night) Color(neon) else when (this) {
    Palette.BLUE -> Color(0xFF0277BD)
    Palette.VIOLET -> Color(0xFF6200EA)
    Palette.GREEN -> Color(0xFF2E7D32)
    Palette.ORANGE -> Color(0xFFE65100)
    Palette.RED -> Color(0xFFC62828)
    Palette.BLACK -> Color(0xFF111820)
}

/** All styles of the theme with the chosen family; the system choice leaves them as they are. */
private fun Typography.withFamily(f: FontFamily?): Typography = if (f == null) this else copy(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f), headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

/** AUTO follows the clock (dark from 19:00 to 07:00) since a head unit has no system dark-mode signal. */
fun isNight(mode: DayNightMode, hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): Boolean =
    when (mode) {
        DayNightMode.DAY -> false
        DayNightMode.NIGHT -> true
        DayNightMode.AUTO -> hour >= 19 || hour < 7
    }

@Composable
fun LauncherTheme(
    mode: DayNightMode,
    palette: Palette,
    font: FontChoice = FontChoice.FUTURISTA,
    night: Boolean = isNight(mode),
    content: @Composable () -> Unit,
) {
    val accent = palette.accent(night)
    // Before the content is composed, so the first frame already has the chosen colour and font.
    val black = night && palette == Palette.BLACK
    Hud.apply(accent, font, black)
    val typography = remember(font) { Typography().withFamily(font.family()) }
    val scheme = if (night) {
        darkColorScheme(
            primary = accent,
            background = if (black) Color(0xFF000000) else Color(0xFF060509),
            surface = if (black) Color(0xFF0A0A0A) else Color(0xFF0C0A12),
            surfaceVariant = if (black) Color(0xFF1A1A1A) else Color(0xFF1C1828),
            onBackground = Color(0xFFEDE6F2),
            onSurface = Color(0xFFEDE6F2),
        )
    } else {
        lightColorScheme(
            primary = accent,
            background = Color(0xFFEFF3F7),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFDDE5ED),
            onBackground = Color(0xFF111820),
            onSurface = Color(0xFF111820),
        )
    }
    MaterialTheme(colorScheme = scheme, typography = typography) {
        // No Surface wraps the home screen, so set the default text/icon colour explicitly.
        CompositionLocalProvider(LocalContentColor provides scheme.onBackground, content = content)
    }
}
