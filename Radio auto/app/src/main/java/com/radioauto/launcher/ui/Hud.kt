package com.radioauto.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Futuristic monochrome look: pure black, a few greys, one ice-white accent and hairline outlines.
 * Colour is kept for meaning only (warnings, the speed limit), never for decoration.
 */
object Hud {
    val Bg = Color(0xFF000000)
    val Panel = Color(0xFF0A0B0D)
    val Mid = Color(0xFF15181B)
    val Line = Color(0x24FFFFFF)
    val Accent = Color(0xFFCFE8F2)
    val Muted = Color(0xFF6F767D)
    val Radius = 16.dp
}

/** A dark panel with a hairline outline. */
fun Modifier.hudPanel(radius: Dp = Hud.Radius, fill: Color = Hud.Panel): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.clip(shape).background(fill).border(1.dp, Hud.Line, shape)
}
