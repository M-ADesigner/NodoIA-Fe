package com.radioauto.launcher.ui

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.radioauto.launcher.data.MediaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** What the clock and the background draw: only the fields that change the picture, not the track position. */
internal data class Cover(
    val art: Bitmap?,
    val seed: String,
    val title: String,
    val artist: String,
    val playing: Boolean,
    val active: Boolean,
)

/** The current cover. The position ticks every second but is left out, so these do not redraw each second. */
@Composable
internal fun rememberCover(): Cover {
    val flow = remember {
        MediaState.state
            .map { Cover(it.albumArt, "${it.title}|${it.artist}", it.title, it.artist, it.isPlaying, it.hasMusic) }
            .distinctUntilChanged()
    }
    val cover by flow.collectAsState(Cover(null, "", "", "", false, false))
    return cover
}

/** The cover to draw: the current one, or the last one while it fades out after the music stops. */
@Composable
internal fun rememberShownCover(cover: Cover): Cover? {
    var last by remember { mutableStateOf<Cover?>(null) }
    LaunchedEffect(cover) { if (cover.active) last = cover }
    return if (cover.active) cover else last
}

/** Target opacity for a cover layer: full when playing, faint when paused, gone with no music; eased over 700 ms. */
@Composable
internal fun coverAlpha(cover: Cover, playing: Float, paused: Float): Float {
    val target = if (!cover.active) 0f else if (cover.playing) playing else paused
    return animateFloatAsState(target, tween(700), label = "coverAlpha").value
}

/**
 * The cover filling its box. Without art it is a gradient from the song name, so the same song always looks the same.
 * [blurred] shrinks the picture to a few pixels and lets the upscaling smooth it: Modifier.blur needs Android 12.
 */
@Composable
internal fun CoverImage(cover: Cover, blurred: Boolean, modifier: Modifier = Modifier) {
    val art = cover.art
    if (art == null) {
        val colors = remember(cover.seed) { coverColors(cover.seed) }
        Box(modifier.background(Brush.linearGradient(colors)))
        return
    }
    // Keeps the previous picture until the next one is ready, so a song change does not flash.
    val image by produceState<ImageBitmap?>(null, art, blurred, cover.seed) {
        value = withContext(Dispatchers.Default) { if (blurred) blurredFor(cover.seed, art) else art.asImageBitmap() }
    }
    image?.let { Image(it, null, modifier, contentScale = ContentScale.Crop, filterQuality = FilterQuality.High) }
}

/** The cover of whatever is playing, blurred, full screen behind everything. */
@Composable
fun ArtBackground(modifier: Modifier = Modifier) {
    val current = rememberCover()
    val shown = rememberShownCover(current)
    val alpha = coverAlpha(current, playing = 0.5f, paused = 0.25f)
    if (shown != null && alpha > 0.01f) {
        CoverImage(shown, blurred = true, modifier = modifier.fillMaxSize().alpha(alpha))
    }
}

private const val BLUR_PX = 32
private val blurCache = LruCache<String, ImageBitmap>(3)

/** One blurred copy per song, made once on a background thread. */
private fun blurredFor(seed: String, source: Bitmap): ImageBitmap {
    val key = "$seed#${System.identityHashCode(source)}"
    synchronized(blurCache) { blurCache.get(key)?.let { return it } }
    val result = shrink(source).asImageBitmap()
    synchronized(blurCache) { blurCache.put(key, result) }
    return result
}

/** Halves the picture step by step (one big jump would skip pixels), ending near [BLUR_PX] on the long side. */
private fun shrink(source: Bitmap): Bitmap {
    var bmp = source
    while (max(bmp.width, bmp.height) >= BLUR_PX * 2) {
        bmp = Bitmap.createScaledBitmap(bmp, max(1, bmp.width / 2), max(1, bmp.height / 2), true)
    }
    val scale = BLUR_PX.toFloat() / max(bmp.width, bmp.height)
    return Bitmap.createScaledBitmap(
        bmp, max(1, (bmp.width * scale).roundToInt()), max(1, (bmp.height * scale).roundToInt()), true,
    )
}
