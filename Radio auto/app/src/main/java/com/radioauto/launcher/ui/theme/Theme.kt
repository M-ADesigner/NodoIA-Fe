package com.radioauto.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.radioauto.launcher.data.DayNightMode
import com.radioauto.launcher.data.Palette
import java.util.Calendar

private fun Palette.accent(night: Boolean): Color = when (this) {
    Palette.BLUE -> if (night) Color(0xFF4FC3F7) else Color(0xFF0277BD)
    Palette.VIOLET -> if (night) Color(0xFFB388FF) else Color(0xFF6200EA)
    Palette.GREEN -> if (night) Color(0xFF81C784) else Color(0xFF2E7D32)
    Palette.ORANGE -> if (night) Color(0xFFFFB74D) else Color(0xFFE65100)
    Palette.RED -> if (night) Color(0xFFE57373) else Color(0xFFC62828)
}

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
    night: Boolean = isNight(mode),
    content: @Composable () -> Unit,
) {
    val accent = palette.accent(night)
    val scheme = if (night) {
        darkColorScheme(
            primary = accent,
            background = Color.Black,
            surface = Color(0xFF111418),
            surfaceVariant = Color(0xFF1C2128),
            onBackground = Color(0xFFE8EEF4),
            onSurface = Color(0xFFE8EEF4),
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
    MaterialTheme(colorScheme = scheme) {
        // No Surface wraps the home screen, so set the default text/icon colour explicitly.
        CompositionLocalProvider(LocalContentColor provides scheme.onBackground, content = content)
    }
}
