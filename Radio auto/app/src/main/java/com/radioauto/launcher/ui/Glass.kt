package com.radioauto.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Frosted-glass panel: translucent fill plus a light rim that fades toward the bottom edge. */
@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(28.dp), strength: Float = 0.55f): Modifier {
    val c = MaterialTheme.colorScheme
    return this
        .clip(shape)
        .background(c.surface.copy(alpha = strength))
        .border(
            1.dp,
            Brush.linearGradient(listOf(c.onSurface.copy(alpha = 0.28f), c.onSurface.copy(alpha = 0.04f))),
            shape,
        )
}
