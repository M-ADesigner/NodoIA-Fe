package com.radioauto.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val TICKS = 150

/**
 * Round dial like a knurled knob: a thin bright outer ring, a band of fine radial ticks, and a dark inner
 * disc holding only the time and the short date. It redraws once a minute, not every second.
 */
@Composable
fun ClockDial(diameter: Dp, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000)
        }
    }
    val time = remember(now / 60_000) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now)) }
    val date = remember(now / 60_000) {
        SimpleDateFormat("EEE, d MMM", Locale.forLanguageTag("es")).format(Date(now)).replace(".", "")
    }
    val c = MaterialTheme.colorScheme
    val d = diameter.value
    val current = rememberCover()

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension / 2
            // Dark base so the ticks read over the panel behind the dial
            drawCircle(c.background.copy(alpha = 0.9f), radius = r)
            drawCircle(c.onSurface.copy(alpha = 0.75f), radius = r - 1.5.dp.toPx(), style = Stroke(2.dp.toPx()))
            for (i in 0 until TICKS) {
                val hour = i % (TICKS / 12) == 0
                val a = Math.toRadians(i * 360.0 / TICKS)
                val dx = sin(a).toFloat()
                val dy = -cos(a).toFloat()
                val outer = r * 0.93f
                val inner = r * (if (hour) 0.80f else 0.84f)
                drawLine(
                    color = if (hour) c.primary else c.onSurface.copy(alpha = 0.5f),
                    start = Offset(center.x + dx * inner, center.y + dy * inner),
                    end = Offset(center.x + dx * outer, center.y + dy * outer),
                    strokeWidth = (if (hour) 2.5.dp else 1.dp).toPx(),
                    cap = StrokeCap.Butt,
                )
            }
        }
        // Inner disc: the same dark colour with a faint rim, holding only the time and the date.
        Box(
            Modifier
                .size(diameter * 0.70f)
                .clip(CircleShape)
                .background(c.background.copy(alpha = 0.95f))
                .border(1.5.dp, c.primary.copy(alpha = 0.55f), CircleShape)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            // The cover sits behind the time; a dark gradient on top keeps the digits readable.
            val cover = rememberShownCover(current)
            val artAlpha = coverAlpha(current, playing = 0.55f, paused = 0.25f)
            if (cover != null && artAlpha > 0.01f) {
                CoverImage(cover, blurred = false, modifier = Modifier.fillMaxSize().alpha(artAlpha))
                Box(
                    Modifier.fillMaxSize().alpha(if (current.active) 1f else artAlpha).background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.7f))),
                    ),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(time, fontSize = (d * 0.19f).sp, color = c.onSurface, maxLines = 1)
                Text(date, fontSize = (d * 0.062f).sp, color = c.onSurface.copy(alpha = 0.85f), maxLines = 1)
                if (current.active) {
                    val textWidth = diameter * 0.5f
                    if (current.title.isNotEmpty()) {
                        Text(
                            current.title, Modifier.widthIn(max = textWidth), fontSize = (d * 0.052f).sp,
                            color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (current.artist.isNotEmpty()) {
                        Text(
                            current.artist, Modifier.widthIn(max = textWidth), fontSize = (d * 0.045f).sp,
                            color = c.onSurface.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Equalizer(current.playing, c.primary, Modifier.padding(top = 2.dp).size(diameter * 0.12f, diameter * 0.04f))
                }
            }
        }
    }
}

/**
 * Four small bars that dance while music plays and rest low when it is paused. Like the spectrum in the media bar,
 * it steps ~15 times a second and only exists while visible, to go easy on weak head-unit GPUs.
 */
@Composable
private fun Equalizer(playing: Boolean, color: Color, modifier: Modifier = Modifier) {
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        while (playing) {
            delay(66)
            phase.floatValue = (phase.floatValue + 0.6f) % (2 * PI).toFloat()
        }
    }
    Canvas(modifier) {
        val bars = 4
        val gap = 2.dp.toPx()
        val w = (size.width - gap * (bars - 1)) / bars
        val p = phase.floatValue // read here, so only this draw is redone each step
        for (i in 0 until bars) {
            val level = if (playing) 0.25f + 0.75f * abs(sin(p * (1 + i % 2) + i * 1.3f)) else 0.2f
            val h = size.height * level
            drawRoundRect(
                color,
                topLeft = Offset(i * (w + gap), size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(w / 2),
            )
        }
    }
}
