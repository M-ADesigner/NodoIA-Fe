package com.radioauto.launcher.ui

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.radioauto.launcher.data.FxCaps
import com.radioauto.launcher.data.FxSettings
import com.radioauto.launcher.data.MediaState
import com.radioauto.launcher.data.SoundFx
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The sound studio, laid out like a mixing desk on one screen: styles along the top, a frequency curve over one fader
 * with a level meter per band, and the effects as knobs on the right. Only what the device supports can be moved.
 */
@Composable
fun SoundStudio(fx: FxSettings, onChange: (FxSettings) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { SoundFx.probe(context) }
    val caps by SoundFx.caps.collectAsState()
    val music by MediaState.state.collectAsState()
    val levels = if (caps.equalizer) SoundFx.faders(fx) else emptyList()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.97f).fillMaxHeight(0.96f).steeringKeys()
                .clip(CutShape).background(Hud.Panel).border(1.dp, Hud.Line, CutShape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Top bar.
            Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Estudio de sonido", fontFamily = Hud.Display, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Hud.Accent, modifier = Modifier.weight(1f))
                Pill("Restablecer", false) { onChange(FxSettings(on = fx.on)) }
                Text(if (fx.on) "ACTIVADO" else "APAGADO", fontSize = 10.sp, color = Hud.Muted, letterSpacing = 2.sp)
                Switch(fx.on, { onChange(fx.copy(on = it)) })
                Box(
                    Modifier.size(40.dp).clip(CircleShape).border(1.dp, Hud.Line, CircleShape).clickable(onClick = onDismiss),
                    Alignment.Center,
                ) { Icon(Icons.Default.Close, "Cerrar", Modifier.size(20.dp), Hud.Accent) }
            }

            if (caps.probed && !caps.any) {
                Text("Este equipo no permite efectos de sonido, así que no hay nada que ajustar.", fontSize = 14.sp, color = Hud.Muted)
                return@Column
            }

            // Styles: one tap moves every fader.
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoundFx.PRESETS.keys.forEach { name ->
                    Pill(name, fx.preset == name) { onChange(fx.copy(on = true, preset = name, bands = SoundFx.levelsFor(name, caps))) }
                }
            }

            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // The desk.
                Column(
                    Modifier.weight(1f).fillMaxHeight().clip(CutShape).background(Hud.Mid).border(1.dp, Hud.Line, CutShape).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Curve(levels, caps, Modifier.fillMaxWidth().height(78.dp))
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        levels.forEachIndexed { i, level ->
                            Channel(
                                level, caps, i, music.isPlaying,
                                freq(SoundFx.GRAPHIC_HZ[i]), Modifier.weight(1f),
                            ) { v -> onChange(fx.copy(on = true, preset = "Personalizado", bands = levels.toMutableList().also { it[i] = v })) }
                        }
                    }
                }

                // Effects.
                Column(
                    Modifier.width(270.dp).fillMaxHeight().clip(CutShape).background(Hud.Mid).border(1.dp, Hud.Line, CutShape).verticalScroll(rememberScrollState()).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("EFECTOS", fontSize = 10.sp, color = Hud.Muted, letterSpacing = 2.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Knob("BAJOS", fx.bass / 1000f, caps.bass, Hud.Hot) { onChange(fx.copy(on = true, bass = (it * 1000).toInt())) }
                        Knob("ENVOLV.", fx.virtualizer / 1000f, caps.virtualizer, Hud.Accent) { onChange(fx.copy(on = true, virtualizer = (it * 1000).toInt())) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Knob("VOL. EXTRA", fx.loudness / 1500f, caps.loudness, Hud.Accent) { onChange(fx.copy(on = true, loudness = (it * 1500).toInt())) }
                        VolumeKnob(context)
                    }
                    Text("REVERBERACIÓN", fontSize = 10.sp, color = Hud.Muted, letterSpacing = 2.sp, modifier = Modifier.padding(top = 2.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SoundFx.REVERBS.forEachIndexed { i, name ->
                            if (caps.reverb) Pill(name, fx.reverb == i, small = true) { onChange(fx.copy(on = true, reverb = i)) }
                        }
                    }
                }
            }
        }
    }
}

private fun freq(hz: Int): String = if (hz >= 1000) String.format("%.1f kHz", hz / 1000f).replace(".0", "") else "$hz Hz"

@Composable
private fun Pill(text: String, selected: Boolean, small: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.height(if (small) 30.dp else 38.dp).clip(CutShape)
            .background(if (selected) Hud.Accent.copy(alpha = 0.3f) else Hud.Bg)
            .border(1.dp, if (selected) Hud.Accent else Hud.Line, CutShape)
            .clickable(onClick = onClick).padding(horizontal = if (small) 10.dp else 14.dp),
        Alignment.Center,
    ) { Text(text, fontSize = if (small) 11.sp else 12.sp, color = if (selected) Color.White else Color(0xFFEDE6F2), maxLines = 1) }
}

/** The response of the equaliser drawn as a smooth curve, like the graph of a mixing program. */
@Composable
private fun Curve(levels: List<Int>, caps: FxCaps, modifier: Modifier) {
    val accent = Hud.Accent
    val line = Hud.Line
    Canvas(modifier.clip(CutShape).background(Hud.Bg)) {
        val n = levels.size.coerceAtLeast(1)
        val range = (caps.maxLevel - caps.minLevel).coerceAtLeast(1).toFloat()
        // Grid: the 0 dB line and the quarters.
        for (k in 1..3) drawLine(line.copy(alpha = 0.15f), Offset(0f, size.height * k / 4), Offset(size.width, size.height * k / 4), 1f)
        val zero = size.height * (1f - (0 - caps.minLevel) / range)
        drawLine(line, Offset(0f, zero), Offset(size.width, zero), 1.5f)
        if (levels.isEmpty()) return@Canvas
        val pts = levels.mapIndexed { i, l ->
            Offset(size.width * (i + 0.5f) / n, size.height * (1f - (l - caps.minLevel) / range))
        }
        val path = Path().apply {
            moveTo(0f, pts.first().y)
            lineTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                val mx = (pts[i - 1].x + pts[i].x) / 2
                cubicTo(mx, pts[i - 1].y, mx, pts[i].y, pts[i].x, pts[i].y)
            }
            lineTo(size.width, pts.last().y)
        }
        val fill = Path().apply { addPath(path); lineTo(size.width, zero); lineTo(0f, zero); close() }
        drawPath(fill, accent.copy(alpha = 0.18f))
        drawPath(path, accent, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        pts.forEach { drawCircle(accent, 4.dp.toPx(), it) }
    }
}

/** One band: a fader with a level meter beside it. The meter moves with the music while it plays. */
@Composable
private fun Channel(level: Int, caps: FxCaps, index: Int, playing: Boolean, label: String, modifier: Modifier, onChange: (Int) -> Unit) {
    val min = caps.minLevel
    val max = caps.maxLevel
    val accent = Hud.Accent
    val line = Hud.Line
    val hot = Hud.Hot

    // A lively meter: each band has its own rhythm; the band's boost lifts it. Falls to rest when paused.
    var meter by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing, level) {
        var t = index * 1.7f
        val gain = (level - min) / (max - min).toFloat() - 0.5f
        while (true) {
            val target = if (playing) (0.35f + 0.35f * abs(sin(t * (1.3f + index * 0.37f))) + 0.3f * gain).coerceIn(0.05f, 1f) else 0.03f
            meter += (target - meter) * 0.35f
            t += 0.09f
            delay(60)
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(String.format("%+.0f", level / 100f), fontSize = 10.sp, color = Hud.Muted)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Canvas(
                Modifier.width(30.dp).fillMaxHeight()
                    .pointerInput(min, max) { detectTapGestures { onChange(levelAt(it.y, size.height.toFloat(), min, max)) } }
                    .pointerInput(min, max) {
                        detectDragGestures(onDragStart = { onChange(levelAt(it.y, size.height.toFloat(), min, max)) }) { change, _ ->
                            onChange(levelAt(change.position.y, size.height.toFloat(), min, max))
                        }
                    },
            ) {
                val cx = size.width / 2
                val y = size.height * (1f - (level - min) / (max - min).toFloat())
                val mid = size.height * (1f - (0 - min) / (max - min).toFloat())
                drawLine(line, Offset(cx, 0f), Offset(cx, size.height), 6.dp.toPx())
                drawLine(accent.copy(alpha = 0.75f), Offset(cx, mid), Offset(cx, y), 6.dp.toPx())
                drawLine(line, Offset(cx - 14.dp.toPx(), mid), Offset(cx + 14.dp.toPx(), mid), 1.dp.toPx())
                drawRect(accent, Offset(cx - 14.dp.toPx(), y - 8.dp.toPx()), Size(28.dp.toPx(), 16.dp.toPx()))
                drawLine(Color.Black.copy(alpha = 0.5f), Offset(cx - 9.dp.toPx(), y), Offset(cx + 9.dp.toPx(), y), 2.dp.toPx())
            }
            // Segmented meter: green, then amber, then red at the top.
            Canvas(Modifier.width(9.dp).fillMaxHeight()) {
                val segs = 16
                val gap = 2.dp.toPx()
                val h = (size.height - gap * (segs - 1)) / segs
                for (s in 0 until segs) {
                    val lit = (s + 1) / segs.toFloat() <= meter
                    val c = when {
                        s >= segs - 3 -> hot
                        s >= segs - 7 -> Color(0xFFE0A458)
                        else -> Color(0xFF6BE3A0)
                    }
                    drawRect(
                        if (lit) c else line.copy(alpha = 0.18f),
                        Offset(0f, size.height - (s + 1) * h - s * gap), Size(size.width, h),
                    )
                }
            }
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, textAlign = TextAlign.Center)
    }
}

private fun levelAt(y: Float, height: Float, min: Int, max: Int): Int {
    val f = 1f - (y / height).coerceIn(0f, 1f)
    return (min + f * (max - min)).toInt().coerceIn(min, max)
}

/** A rotary knob: drag up or down to turn it. Dimmed and fixed when the device does not have the effect. */
@Composable
private fun Knob(label: String, value: Float, enabled: Boolean, color: Color, onChange: (Float) -> Unit) {
    val line = Hud.Line
    val face = Hud.Bg
    var current by remember { mutableFloatStateOf(value) }
    current = value
    Column(Modifier.width(110.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Canvas(
            Modifier.size(54.dp).let { m ->
                if (!enabled) m else m.pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        current = (current - drag.y / 220f).coerceIn(0f, 1f)
                        onChange(current)
                    }
                }
            },
        ) {
            val r = size.minDimension / 2 - 5.dp.toPx()
            val c = center
            val sweep = 270f
            drawCircle(face, r, c)
            drawArc(line, 135f, sweep, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            drawArc(if (enabled) color else line, 135f, sweep * value, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            val a = (135f + sweep * value) * PI.toFloat() / 180f
            drawLine(Color.White.copy(alpha = if (enabled) 1f else 0.3f), c, Offset(c.x + cos(a) * (r - 6.dp.toPx()), c.y + sin(a) * (r - 6.dp.toPx())), 3.dp.toPx(), StrokeCap.Round)
        }
        Text(if (enabled) "${(value * 100).toInt()} %" else "—", fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(label, fontSize = 9.sp, color = Hud.Muted, letterSpacing = 1.sp, maxLines = 1)
    }
}

/** The system music volume as a knob, so the loudness can be set next to the effects. */
@Composable
private fun VolumeKnob(context: Context) {
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val max = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var value by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()) }
    Knob("VOLUMEN", value, true, Hud.Accent) {
        value = it
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (it * max).toInt().coerceIn(0, max), 0)
    }
}
