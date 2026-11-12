package com.radioauto.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radioauto.launcher.R
import com.radioauto.launcher.data.BluetoothMusic
import com.radioauto.launcher.data.MediaState
import com.radioauto.launcher.data.RadioPlayer
import com.radioauto.launcher.data.RadioStatus
import com.radioauto.launcher.data.TrackingState
import com.radioauto.launcher.link.LinkServer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.cos
import kotlin.math.sin

/** The three things the big right-hand panel can show; saved by name in the settings. */
enum class PanelMode(val label: String, val icon: ImageVector) {
    CAR("Auto", Icons.Default.DirectionsCar),
    MAP("Mapa", Icons.Default.Map),
    MUSIC("Música", Icons.Default.MusicNote);

    companion object {
        fun from(name: String) = entries.firstOrNull { it.name == name } ?: MAP
    }
}

/** A pointed (gothic) arch: straight sides and an ogive top. */
private object ArchShape : Shape {
    private val top = listOf(
        0f to 0.38f, 0.06f to 0.24f, 0.16f to 0.12f, 0.30f to 0.04f, 0.5f to 0f,
        0.70f to 0.04f, 0.84f to 0.12f, 0.94f to 0.24f, 1f to 0.38f,
    )

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        // The ogive is stretched to the width, so it stays pointed on tall and on squat shapes alike.
        val rise = minOf(size.height, size.width * 1.25f)
        val path = Path().apply {
            moveTo(0f, size.height)
            top.forEach { (x, y) -> lineTo(size.width * x, rise * y + (size.height - rise) * 0f) }
            lineTo(size.width, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

/** A rectangle with its four corners cut off. */
internal val CutShape = GenericShape { size, _ ->
    val c = 8f
    moveTo(c, 0f); lineTo(size.width - c, 0f); lineTo(size.width, c)
    lineTo(size.width, size.height - c); lineTo(size.width - c, size.height)
    lineTo(c, size.height); lineTo(0f, size.height - c); lineTo(0f, c); close()
}

/** A thin rose window drawn in the accent colour; used behind the music and on the empty cover. */
@Composable
private fun RoseWindow(modifier: Modifier, alpha: Float) {
    val color = Hud.Accent.copy(alpha = alpha)
    val hot = Hud.Hot.copy(alpha = alpha * 1.6f)
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = minOf(size.width, size.height) / 2
        val s = Stroke(1.5.dp.toPx())
        listOf(0.96f, 0.77f, 0.46f, 0.15f).forEach { drawCircle(color, r * it, c, style = s) }
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            drawLine(color, Offset(c.x + dx * r * 0.15f, c.y + dy * r * 0.15f), Offset(c.x + dx * r * 0.96f, c.y + dy * r * 0.96f), s.width)
            drawCircle(color, r * 0.11f, Offset(c.x + dx * r * 0.62f, c.y + dy * r * 0.62f), style = s)
        }
        drawCircle(hot, r * 0.07f, c)
    }
}

/** Holds the chosen panel with a rail of three arches on the left. Only the visible panel is composed. */
@Composable
fun RightPanel(
    mode: PanelMode,
    onMode: (PanelMode) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    map: @Composable () -> Unit,
) {
    Box(modifier.hudPanel(Hud.Radius, Hud.Panel)) {
        when (mode) {
            PanelMode.CAR -> CarPhotoPanel(Modifier.fillMaxSize())
            PanelMode.MAP -> map()
            PanelMode.MUSIC -> MusicPanel(Modifier.fillMaxSize())
        }
        // Floating over the map, the photo or the music, always in the same spot.
        ModeSelector(mode, onMode, Modifier.align(Alignment.TopStart).padding(10.dp))
        // Settings live here, in every mode.
        Box(
            Modifier.align(Alignment.TopEnd).padding(10.dp).size(44.dp).clip(CircleShape)
                .background(Hud.Bg.copy(alpha = 0.88f)).border(1.dp, Hud.Line, CircleShape)
                .clickable(onClick = onSettings),
            Alignment.Center,
        ) { Icon(Icons.Default.Settings, "Ajustes", Modifier.size(22.dp), Hud.Accent) }
    }
}

/** Three tabs with cut corners in a dark pill; the chosen one is lit in the accent colour. */
@Composable
private fun ModeSelector(mode: PanelMode, onMode: (PanelMode) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CutShape).background(Hud.Bg.copy(alpha = 0.88f)).border(1.dp, Hud.Line, CutShape).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PanelMode.entries.forEach { m ->
            val selected = m == mode
            val tint = if (selected) Color.White else Hud.Accent
            Row(
                Modifier
                    .height(42.dp)
                    .clip(CutShape)
                    .background(if (selected) Hud.Accent.copy(alpha = 0.32f) else Color.Transparent)
                    .border(1.dp, if (selected) Hud.Accent else Color.Transparent, CutShape)
                    .clickable { onMode(m) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(m.icon, m.label, Modifier.size(20.dp), tint)
                Text(m.label.uppercase(), fontSize = 12.sp, color = tint, maxLines = 1, letterSpacing = 1.sp)
            }
        }
    }
}

/** The car photo with the model name over it; the speed already shows in the dial on the left. */
@Composable
fun CarPhotoPanel(modifier: Modifier = Modifier) {
    Box(modifier.background(Hud.Panel)) {
        Image(
            painterResource(R.drawable.i20_active), "Hyundai i20 Active",
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
        )
        // Sharp, full-colour photo; the dark fade only starts at a third of the height so the car stays clean.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Hud.Bg.copy(alpha = 0.92f))))
        Column(Modifier.align(Alignment.BottomEnd).padding(20.dp), horizontalAlignment = Alignment.End) {
            Text("HYUNDAI", fontSize = 12.sp, color = Hud.Accent, letterSpacing = 3.sp)
            Text("i20 Active", fontFamily = Hud.Display, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

private fun mmss(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/**
 * Now playing as a record player: a spinning vinyl with the cover as its label and a tonearm, the title and the
 * controls on the right and the next tracks below them. The record only turns while the music plays.
 */
@Composable
private fun BluetoothPlayer(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val music by MediaState.state.collectAsState()
    val link by LinkServer.state.collectAsState()
    val linkTrack = link.track

    val title = linkTrack?.title ?: music.title
    val artist = linkTrack?.artist ?: music.artist
    val playing = linkTrack?.playing ?: music.isPlaying
    val art = linkTrack?.art ?: music.albumArt
    val artBitmap = remember(art) { art?.asImageBitmap() }
    val hasTrack = linkTrack != null || music.hasMusic

    // Keeps its angle when the music stops, and goes on from there.
    val angle = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        if (playing) while (true) angle.animateTo(angle.value + 360f, tween(6_000, easing = LinearEasing))
    }

    // What comes after the current track; without a marked current one, the head of the queue.
    val upcoming = remember(music.queue) {
        val at = music.queue.indexOfFirst { it.current }
        music.queue.drop(at + 1).take(3)
    }

    Box(modifier.background(Hud.Panel)) {
        // On a very wide screen the player stays together in the middle instead of hugging the left edge.
        Box(Modifier.align(Alignment.Center).widthIn(max = 840.dp).fillMaxSize()) {
        // ----- the record player -----
        Box(Modifier.align(Alignment.TopStart).padding(start = 22.dp, top = 66.dp).size(380.dp, 380.dp)) {
            // A warm halo that breathes while the music plays and fades out when it stops.
            val glowOn by animateFloatAsState(if (playing) 1f else 0f, tween(600), label = "glowOn")
            val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "glow").animateFloat(
                0.55f, 1f,
                androidx.compose.animation.core.infiniteRepeatable(tween(1200, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse),
                label = "pulse",
            )
            Canvas(Modifier.align(Alignment.BottomStart).offset((-25).dp, 25.dp).size(380.dp)) {
                drawCircle(
                    Brush.radialGradient(listOf(Hud.Hot.copy(alpha = 0.35f * glowOn * pulse), Color.Transparent)),
                    radius = size.minDimension / 2,
                )
            }
            Box(Modifier.align(Alignment.BottomStart).size(330.dp).graphicsLayer { rotationZ = angle.value }) {
                Record(artBitmap)
            }
            Tonearm(Modifier.fillMaxSize(), playing)
        }

        // ----- title, controls and queue -----
        Column(Modifier.align(Alignment.TopStart).padding(start = 424.dp, top = 76.dp, end = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (playing) LiveBars()
                Text(if (playing) "AHORA SUENA" else "EN PAUSA", fontSize = 11.sp, color = if (playing) Hud.Accent else Hud.Muted, letterSpacing = 3.sp)
            }
            Text(
                if (hasTrack) title.ifEmpty { "Sin título" } else if (music.btConnected) "Celular conectado" else "Sin Bluetooth",
                fontFamily = Hud.Display, fontSize = 40.sp, fontWeight = FontWeight.Bold, lineHeight = 42.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                if (hasTrack) artist else if (music.btConnected) "Toca ▶ para reproducir" else "Conecta tu celular",
                fontSize = 13.sp, color = Hud.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
            )
            if (linkTrack == null && music.durationMs > 0) {
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(mmss(music.positionMs), fontSize = 10.sp, color = Hud.Muted)
                    Box(Modifier.weight(1f).height(3.dp).background(Hud.Line)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth((music.positionMs / music.durationMs.toFloat()).coerceIn(0f, 1f)).background(Hud.Accent))
                    }
                    Text(mmss(music.durationMs), fontSize = 10.sp, color = Hud.Muted)
                }
            } else {
                Spacer(Modifier.height(17.dp))
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton({ BluetoothMusic.previous(context) }, Modifier.size(48.dp)) { Icon(Icons.Default.SkipPrevious, "Anterior", Modifier.size(32.dp)) }
                IconButton({ BluetoothMusic.playPause(context) }, Modifier.size(62.dp).border(1.5.dp, Hud.Accent, CircleShape)) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Reproducir o pausar", Modifier.size(38.dp))
                }
                IconButton({ BluetoothMusic.next(context) }, Modifier.size(48.dp)) { Icon(Icons.Default.SkipNext, "Siguiente", Modifier.size(32.dp)) }
            }

            Text("A CONTINUACIÓN", fontSize = 11.sp, color = Hud.Accent, letterSpacing = 3.sp, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
            if (upcoming.isEmpty()) {
                Text("Esta app no comparte su lista de reproducción.", fontSize = 12.sp, color = Hud.Muted)
            } else {
                upcoming.forEachIndexed { i, item ->
                    Row(
                        Modifier.fillMaxWidth().height(38.dp)
                            .background(if (i == 0) Hud.Hot.copy(alpha = 0.14f) else Color.Transparent)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("${i + 1}", fontSize = 11.sp, color = Hud.Muted, modifier = Modifier.width(14.dp))
                        Text(item.title.ifEmpty { "Sin título" }, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (item.subtitle.isNotEmpty()) {
                            Text(item.subtitle, fontSize = 10.sp, color = Hud.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(90.dp))
                        }
                    }
                }
            }
        }
        }
    }
}

/** The disc: grooves, a sheen, and the cover (or a rose window) as the label. Drawn once; the parent only rotates it. */
@Composable
private fun Record(cover: androidx.compose.ui.graphics.ImageBitmap?) {
    val accent = Hud.Accent
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension / 2
            drawCircle(Color(0xFF0A0810), r, c)
            var g = r * 0.30f
            var i = 0
            while (g < r - 2.dp.toPx()) {
                drawCircle(if (i % 2 == 0) Color(0xFF17121F) else Color(0xFF0E0B15), g, c, style = Stroke(1.dp.toPx()))
                g += 3.dp.toPx()
                i++
            }
            drawCircle(
                Brush.sweepGradient(
                    0f to Color.White.copy(alpha = 0.08f), 0.22f to Color.Transparent,
                    0.5f to Color.White.copy(alpha = 0.05f), 0.72f to Color.Transparent, 1f to Color.White.copy(alpha = 0.08f), center = c,
                ),
                r, c,
            )
            drawCircle(accent.copy(alpha = 0.45f), r - 1.dp.toPx(), c, style = Stroke(2.dp.toPx()))
        }
        // The label.
        Box(
            Modifier.size(112.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Hud.Hot.copy(alpha = 0.45f), Hud.Bg)))
                .border(1.5.dp, accent, CircleShape),
            Alignment.Center,
        ) {
            if (cover != null) Image(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else RoseWindow(Modifier.size(84.dp), alpha = 0.7f)
        }
        Box(Modifier.size(10.dp).clip(CircleShape).background(Hud.Bg).border(1.dp, accent, CircleShape))
    }
}

/** The arm: a pivot at the top right and a head resting on the record while it plays, lifted away when it stops. */
@Composable
private fun Tonearm(modifier: Modifier, playing: Boolean) {
    val accent = Hud.Accent
    val hot = Hud.Hot
    // 0 = lifted, 1 = on the record.
    val lower by animateFloatAsState(if (playing) 1f else 0f, tween(500), label = "arm")
    Canvas(modifier) {
        val pivot = Offset(size.width - 24.dp.toPx(), 18.dp.toPx())
        val rest = Offset(size.width - 30.dp.toPx(), 190.dp.toPx())
        val needle = Offset(size.width * 0.62f, size.height * 0.45f)
        val elbow = Offset(pivot.x - 6.dp.toPx(), pivot.y + 30.dp.toPx())
        val head = Offset(
            rest.x + (needle.x - rest.x) * lower,
            rest.y + (needle.y - rest.y) * lower,
        )
        val w = 5.dp.toPx()
        drawLine(Color(0xFFCFC6E0), pivot, elbow, w, StrokeCap.Round)
        drawLine(Color(0xFFCFC6E0), elbow, head, w, StrokeCap.Round)
        rotate(-48f, head) {
            drawRoundRect(accent, Offset(head.x - 13.dp.toPx(), head.y - 7.dp.toPx()), Size(26.dp.toPx(), 14.dp.toPx()), CornerRadius(2.dp.toPx()))
        }
        drawCircle(Color(0xFF15121D), 19.dp.toPx(), pivot)
        drawCircle(accent, 19.dp.toPx(), pivot, style = Stroke(2.dp.toPx()))
        drawCircle(hot, 7.dp.toPx(), pivot)
    }
}

/** Three little bars that dance while the music plays. */
@Composable
private fun LiveBars() {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "bars")
    val heights = listOf(800 to 0.3f, 900 to 0.8f, 1000 to 0.5f).map { (ms, from) ->
        t.animateFloat(
            from, 1f,
            androidx.compose.animation.core.infiniteRepeatable(tween(ms, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse),
            label = "bar$ms",
        )
    }
    Row(Modifier.height(18.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        heights.forEach { h -> Box(Modifier.width(3.dp).height(18.dp * h.value).background(Hud.Hot)) }
    }
}

/** Music panel with a source switch: the phone over Bluetooth, or the radio's own FM app. */
@Composable
fun MusicPanel(modifier: Modifier = Modifier) {
    var radio by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    Box(modifier) {
        if (radio) RadioPanel(Modifier.fillMaxSize()) else BluetoothPlayer(Modifier.fillMaxSize())
        Row(
            Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 64.dp)
                .clip(CutShape).background(Hud.Bg.copy(alpha = 0.88f)).border(1.dp, Hud.Line, CutShape).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(false to "BLUETOOTH", true to "RADIO").forEach { (isRadio, label) ->
                val selected = radio == isRadio
                Box(
                    Modifier.height(42.dp).clip(CutShape)
                        .background(if (selected) Hud.Accent.copy(alpha = 0.32f) else Color.Transparent)
                        .border(1.dp, if (selected) Hud.Accent else Color.Transparent, CutShape)
                        .clickable { radio = isRadio }.padding(horizontal = 14.dp),
                    Alignment.Center,
                ) { Text(label, fontSize = 12.sp, color = if (selected) Color.White else Hud.Accent, letterSpacing = 1.sp) }
            }
        }
    }
}

/**
 * The radio side: a transmitter tower on the left that sends out arcs of signal while the station plays, the station
 * and the controls on the right, and a strip of stations below.
 */
@Composable
private fun RadioPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val radio by RadioPlayer.state.collectAsState()
    LaunchedEffect(Unit) { RadioPlayer.ensureLoaded(context) }
    val playing = radio.status == RadioStatus.PLAYING
    val live = playing || radio.status == RadioStatus.LOADING
    val station = radio.current
    var adding by remember { androidx.compose.runtime.mutableStateOf(false) }
    if (adding) AddStationDialog(onDismiss = { adding = false })

    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "tower")
    val phase by t.animateFloat(
        0f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "arcs",
    )
    val blink by t.animateFloat(
        0.3f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(1000), androidx.compose.animation.core.RepeatMode.Reverse), label = "blink",
    )
    // The arcs fade in when the station starts and out when it stops.
    val signal by animateFloatAsState(if (playing) 1f else 0f, tween(500), label = "signal")

    Box(modifier.background(Hud.Panel)) {
        Canvas(Modifier.align(Alignment.TopStart).padding(start = 110.dp, top = 94.dp).size(200.dp, 330.dp)) {
            fun d(v: Float) = v.dp.toPx()
            val top = Offset(d(100f), d(52f))
            val tower = Hud.Accent
            val w = d(2f)
            drawLine(tower, top, Offset(d(60f), d(320f)), w)
            drawLine(tower, top, Offset(d(140f), d(320f)), w)
            drawLine(tower, Offset(d(80f), d(190f)), Offset(d(120f), d(190f)), w)
            drawLine(tower, Offset(d(72f), d(250f)), Offset(d(128f), d(250f)), w)
            drawLine(tower, Offset(d(88f), d(130f)), Offset(d(112f), d(130f)), w)
            drawLine(tower, Offset(d(80f), d(190f)), Offset(d(128f), d(250f)), w)
            drawLine(tower, Offset(d(120f), d(190f)), Offset(d(72f), d(250f)), w)
            drawLine(tower, Offset(d(88f), d(130f)), Offset(d(120f), d(190f)), w)
            drawLine(tower, Offset(d(112f), d(130f)), Offset(d(80f), d(190f)), w)
            drawLine(tower, Offset(d(30f), d(320f)), Offset(d(170f), d(320f)), d(3f))
            // Signal arcs leave the top, one third of a cycle apart; each grows and fades.
            val origin = Offset(d(100f), d(44f))
            if (signal > 0.01f) {
                for (k in 0 until 3) {
                    val p = (phase + k / 3f) % 1f
                    val r = d(18f + p * 100f)
                    val color = (if (k == 2) Hud.Hot else Hud.Accent).copy(alpha = (1f - p) * signal)
                    val stroke = Stroke(d(3f), cap = StrokeCap.Round)
                    drawArc(color, -50f, 100f, false, Offset(origin.x - r, origin.y - r), Size(2 * r, 2 * r), style = stroke)
                    drawArc(color, 130f, 100f, false, Offset(origin.x - r, origin.y - r), Size(2 * r, 2 * r), style = stroke)
                }
            }
            // The warning light on top blinks while the station is on.
            val lamp = if (live) blink else 0.35f
            drawCircle(Hud.Hot.copy(alpha = lamp * 0.35f), d(14f), origin)
            drawCircle(Hud.Hot.copy(alpha = lamp), d(8f), origin)
        }

        Column(Modifier.align(Alignment.TopStart).padding(start = 380.dp, top = 110.dp, end = 30.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (playing) LiveBars()
                Text(
                    when {
                        radio.listLoading -> "BUSCANDO EMISORAS…"
                        radio.status == RadioStatus.LOADING -> "CONECTANDO…"
                        playing -> "EN VIVO"
                        radio.status == RadioStatus.ERROR -> "SIN SEÑAL"
                        else -> "RADIO POR INTERNET"
                    },
                    fontSize = 11.sp, color = if (radio.status == RadioStatus.ERROR) Hud.Hot else Hud.Accent, letterSpacing = 4.sp,
                )
            }
            Text(
                station?.name ?: "Sin emisoras",
                fontFamily = Hud.Display, fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 48.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                when {
                    radio.message.isNotEmpty() -> radio.message
                    station != null -> listOf(RadioPlayer.signalCount(station).takeIf { it > 1 }?.let { "Señal ${RadioPlayer.signalNumber(station)} de $it" }, station.codec, station.bitrate.takeIf { it > 0 }?.let { "$it kbps" }, station.tags.ifEmpty { null }).filterNotNull().joinToString("  ·  ")
                    else -> ""
                },
                fontSize = 13.sp, color = if (radio.message.isNotEmpty()) Hud.Hot else Hud.Muted, modifier = Modifier.padding(top = 8.dp),
            )
            Row(Modifier.padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton({ RadioPlayer.previous() }, Modifier.size(52.dp)) { Icon(Icons.Default.SkipPrevious, "Emisora anterior", Modifier.size(34.dp)) }
                IconButton({ if (radio.stations.isEmpty()) RadioPlayer.reload() else RadioPlayer.toggle() }, Modifier.size(66.dp).border(1.5.dp, Hud.Accent, CircleShape)) {
                    Icon(if (live) Icons.Default.Pause else Icons.Default.PlayArrow, "Reproducir o detener", Modifier.size(40.dp))
                }
                IconButton({ RadioPlayer.next() }, Modifier.size(52.dp)) { Icon(Icons.Default.SkipNext, "Emisora siguiente", Modifier.size(34.dp)) }
                // Star the one you like: favourites go first in the strip below, always in view.
                IconButton({ RadioPlayer.toggleFavorite() }, Modifier.size(52.dp)) {
                    Icon(
                        if (radio.currentIsFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        if (radio.currentIsFavorite) "Quitar de favoritas" else "Agregar a favoritas",
                        Modifier.size(32.dp), if (radio.currentIsFavorite) Hud.Hot else Hud.Accent,
                    )
                }
            }
            // Sounds bad: another signal. Plus adding a station from the search and removing the selected one.
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RadioAction("CAMBIAR SEÑAL") { RadioPlayer.changeSignal() }
                RadioAction("+ AGREGAR") { adding = true }
                RadioAction("QUITAR") { RadioPlayer.removeCurrent() }
            }
        }

        // The stations, one tap each.
        androidx.compose.foundation.lazy.LazyRow(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 18.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(radio.stations) { i, st ->
                val selected = i == radio.index
                Column(
                    Modifier.width(150.dp).height(64.dp).clip(CutShape)
                        .background(if (selected) Hud.Accent.copy(alpha = 0.28f) else Hud.Mid)
                        .border(1.dp, if (selected) Hud.Accent else Hud.Line, CutShape)
                        .clickable { RadioPlayer.play(i) }.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (st.url in radio.favorites) Icon(Icons.Default.Star, null, Modifier.size(14.dp), Hud.Hot)
                        Text(st.name, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = if (selected) Color.White else Hud.Accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioAction(label: String, onClick: () -> Unit) {
    Row(
        Modifier.height(40.dp).clip(CutShape).border(1.dp, Hud.Line, CutShape).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { Text(label, fontSize = 11.sp, color = Hud.Accent, letterSpacing = 1.sp, maxLines = 1) }
}

/** Looks a station up by name in the public directory; tapping a result adds it to the list. */
@Composable
private fun AddStationDialog(onDismiss: () -> Unit) {
    var query by remember { androidx.compose.runtime.mutableStateOf("") }
    var results by remember { androidx.compose.runtime.mutableStateOf<List<com.radioauto.launcher.data.RadioStation>>(emptyList()) }
    var status by remember { androidx.compose.runtime.mutableStateOf("Escribe el nombre de la emisora.") }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    fun search() {
        if (query.isBlank()) return
        keyboard?.hide()
        status = "Buscando…"
        RadioPlayer.search(query) { found ->
            results = found.orEmpty()
            status = when {
                found == null -> "No se pudo buscar. Revisa la conexión a internet."
                found.isEmpty() -> "No se encontró ninguna emisora con ese nombre."
                else -> ""
            }
        }
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar emisora") },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true, placeholder = { Text("Nombre de la emisora") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { search() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (status.isNotEmpty()) Text(status, fontSize = 12.sp, color = Hud.Muted, modifier = Modifier.padding(top = 8.dp))
                LazyColumn(Modifier.fillMaxWidth().height(220.dp).padding(top = 8.dp)) {
                    itemsIndexed(results) { _, st ->
                        Column(
                            Modifier.fillMaxWidth().clickable { RadioPlayer.addStation(st); onDismiss() }.padding(vertical = 8.dp),
                        ) {
                            Text(st.name, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(st.bitrate.takeIf { it > 0 }?.let { "$it kbps" }, st.tags.ifEmpty { null }).joinToString("  ·  "), fontSize = 11.sp, color = Hud.Muted, maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { search() }) { Text("Buscar") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}
