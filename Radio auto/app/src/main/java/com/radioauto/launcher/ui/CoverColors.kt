package com.radioauto.launcher.ui

import androidx.compose.ui.graphics.Color

/**
 * Colours for a song without cover, taken from the hash of its name so the same song always looks the same.
 * Returns 2 or 3 colours. The hues are computed apart from [Color] so they can be tested without Compose.
 */
internal fun coverHues(seed: String): List<Float> {
    val h = seed.hashCode()
    val base = ((h % 360) + 360) % 360
    val step = 40 + ((h ushr 9) and 0x3F) // 40..103 degrees between the colours
    val count = if ((h ushr 16) and 1 == 0) 2 else 3
    return List(count) { ((base + it * step) % 360).toFloat() }
}

internal fun coverColors(seed: String): List<Color> =
    coverHues(seed).map { Color.hsv(it, 0.55f, 0.55f) }
