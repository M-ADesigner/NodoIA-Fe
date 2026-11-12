package com.radioauto.launcher.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** The size the layout was drawn for, in dp. */
private const val BASE_W = 1024f
private const val BASE_H = 600f

/**
 * Makes the whole interface follow the screen: everything is drawn for 1024x600 dp and scaled up or down to the
 * screen's smaller proportion, so a small 800x480 radio gets a compact version of the same screen and a big one gets
 * larger text and buttons instead of tiny ones. The extra length on a wider screen goes to the right-hand panel.
 * Text and touch targets scale together, which keeps them readable and big enough for the car.
 */
@Composable
fun ScreenScale(content: @Composable () -> Unit) {
    BoxWithConstraints {
        val d = LocalDensity.current
        val wDp = maxWidth.value
        val hDp = maxHeight.value
        val s = minOf(wDp / BASE_W, hDp / BASE_H).coerceIn(0.6f, 2.5f)
        CompositionLocalProvider(LocalDensity provides Density(d.density * s, d.fontScale)) {
            content()
        }
    }
}
