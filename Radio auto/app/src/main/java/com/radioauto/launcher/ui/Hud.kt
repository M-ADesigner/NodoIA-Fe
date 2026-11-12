package com.radioauto.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.radioauto.launcher.R
import com.radioauto.launcher.data.FontChoice

/**
 * Dark gothic-futuristic look: near-black violet panels, angular corners and hairline outlines in one accent colour
 * that the driver picks in Settings. Red ([Hot]) is kept for meaning (speeding, the route, the current track).
 * Accent and fonts are snapshot state, so changing them in Settings redraws everything that reads them.
 */
object Hud {
    private var accentState by mutableStateOf(Color(0xFFB48CFF))
    private var displayState by mutableStateOf<FontFamily>(FontFamily.Default)

    private var blackState by mutableStateOf(false)

    val Bg: Color get() = if (blackState) Color(0xFF000000) else Color(0xFF060509)
    val Panel: Color get() = if (blackState) Color(0xFF0A0A0A) else Color(0xFF0C0A12)
    val Mid: Color get() = if (blackState) Color(0xFF151515) else Color(0xFF15121D)
    val Muted = Color(0xFF9A90AC)
    val Hot = Color(0xFFFF2E5B)
    val Radius = 4.dp

    val Accent: Color get() = accentState
    val Line: Color get() = accentState.copy(alpha = 0.28f)

    /** Blackletter for the big titles (the track, the next turn); plain when the driver chose the system font. */
    val Display: FontFamily get() = displayState

    /** Called by the theme; writes only when something changed. */
    fun apply(accent: Color, font: FontChoice, black: Boolean = false) {
        if (accentState != accent) accentState = accent
        if (blackState != black) blackState = black
        val d = if (font == FontChoice.SISTEMA) FontFamily.Default else Blackletter
        if (displayState != d) displayState = d
    }
}

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int): FontFamily = FontFamily(
    listOf(400, 500, 600, 700).map { w ->
        Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

private val Orbitron by lazy { variable(R.font.orbitron) }
private val Cinzel by lazy { variable(R.font.cinzel) }
private val Blackletter by lazy {
    FontFamily(Font(R.font.unifrakturcook, FontWeight.Normal), Font(R.font.unifrakturcook, FontWeight.Bold))
}

/** The family used for all the text; null keeps the system font. */
fun FontChoice.family(): FontFamily? = when (this) {
    FontChoice.FUTURISTA -> Orbitron
    FontChoice.CLASICA -> Cinzel
    FontChoice.GOTICA -> Blackletter
    FontChoice.SISTEMA -> null
}

/** A dark panel with a hairline outline. */
fun Modifier.hudPanel(radius: Dp = Hud.Radius, fill: Color = Hud.Panel): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.clip(shape).background(fill).border(1.dp, Hud.Line, shape)
}

/** The steering wheel buttons also work while this window is open (a dialog has its own window and key handling). */
@androidx.compose.runtime.Composable
fun Modifier.steeringKeys(): Modifier {
    val context = androidx.compose.ui.platform.LocalContext.current
    // A dialog starts with nothing focused, and key events only travel through what has focus.
    val focus = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    return this.focusRequester(focus).focusable().onPreviewKeyEvent { e ->
        com.radioauto.launcher.data.SteeringKeys.handle(
            context, e.nativeKeyEvent.keyCode, e.type == androidx.compose.ui.input.key.KeyEventType.KeyDown, e.nativeKeyEvent.repeatCount,
        )
    }
}
