package com.radioauto.launcher.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radioauto.launcher.data.BluetoothMusic
import com.radioauto.launcher.data.MediaState
import com.radioauto.launcher.data.Obd
import com.radioauto.launcher.link.LinkServer
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Volume and brightness helpers shared by the bar and the settings dialog. */
fun changeVolume(context: Context, delta: Int) {
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val next = (audio.getStreamVolume(AudioManager.STREAM_MUSIC) + delta).coerceIn(0, max)
    // FLAG_SHOW_UI makes the system draw its own volume bar as feedback.
    audio.setStreamVolume(AudioManager.STREAM_MUSIC, next, AudioManager.FLAG_SHOW_UI)
}

fun changeBrightness(context: Context, delta: Int) {
    // Writing brightness needs a one-time grant of WRITE_SETTINGS.
    if (!Settings.System.canWrite(context)) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        return
    }
    val current = runCatching { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) }
        .getOrDefault(128)
    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (current + delta).coerceIn(10, 255))
}

private val OnArt = TextStyle(fontWeight = FontWeight.Medium, shadow = Shadow(Color.Black, Offset(0f, 2f), 10f))

/** Bottom bar: transport controls, cover, then a colourful spectrum with the track name over it, and volume. */
@Composable
fun MediaBar(onOpenBluetooth: () -> Unit, onApps: () -> Unit, onVoice: () -> Unit, onObd: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val music by MediaState.state.collectAsState()
    val link by LinkServer.state.collectAsState()
    val linkTrack = link.track
    val c = MaterialTheme.colorScheme
    val hasAccess by MediaState.accessGranted.collectAsState()

    Row(
        // A darker strip across the whole width, as in the reference, so the controls and title always read.
        modifier.fillMaxWidth().height(74.dp).hudPanel(18.dp).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Transport on the left, big and bare, like the reference.
        IconButton({ BluetoothMusic.previous(context) }, Modifier.size(48.dp)) { Icon(Icons.Default.SkipPrevious, "Anterior", Modifier.size(32.dp)) }
        IconButton({ BluetoothMusic.playPause(context) }, Modifier.size(56.dp).border(1.5.dp, Hud.Accent, CircleShape)) {
            Icon(
                if (music.isPlaying || linkTrack?.playing == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                "Reproducir o pausar", Modifier.size(42.dp),
            )
        }
        IconButton({ BluetoothMusic.next(context) }, Modifier.size(48.dp)) { Icon(Icons.Default.SkipNext, "Siguiente", Modifier.size(32.dp)) }

        // Dark pill: small cover, title, artist and a thin progress line.
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight(0.86f)
                .hudPanel(12.dp, Hud.Bg)
                .clickable { onOpenBluetooth() }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2A2F34), Color(0xFF0E1012)))),
                Alignment.Center,
            ) {
                val art = music.albumArt
                when {
                    art != null -> Image(remember(art) { art.asImageBitmap() }, null, Modifier.size(44.dp), contentScale = ContentScale.Crop)
                    linkTrack != null || music.hasMusic -> Icon(Icons.Default.MusicNote, null, Modifier.size(22.dp), Hud.Accent)
                    else -> Icon(Icons.Default.Bluetooth, "Bluetooth", Modifier.size(22.dp), Hud.Accent)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                val muted = Hud.Muted
                when {
                    linkTrack != null -> {
                        Text(linkTrack.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(linkTrack.artist, fontSize = 12.sp, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    music.hasMusic -> {
                        Text(music.title.ifEmpty { "Sin título" }, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(music.artist, fontSize = 12.sp, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (music.durationMs > 0) {
                            Progress(music.positionMs / music.durationMs.toFloat(), c.primary, Modifier.fillMaxWidth().padding(top = 4.dp).height(3.dp))
                        }
                    }

                    music.btConnected -> {
                        Text("Celular conectado", fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Toca ▶ para reproducir", fontSize = 12.sp, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    else -> {
                        Text("Sin Bluetooth", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFFB74D), maxLines = 1)
                        Text("Toca para conectar", fontSize = 12.sp, color = muted, maxLines = 1)
                    }
                }
            }
        }

        // The big home button in the middle opens every app.
        IconButton(onApps, Modifier.size(56.dp).clip(CircleShape).border(1.5.dp, Hud.Accent, CircleShape)) {
            Icon(Icons.Default.Home, "Todas las apps", Modifier.size(30.dp))
        }

        // OBD status chip: green when the car answers.
        val obd by Obd.state.collectAsState()
        Row(
            Modifier.hudPanel(50.dp, Hud.Bg).clickable { onObd() }.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (obd.connected) Color(0xFF4CAF50) else Hud.Muted))
            Text(if (obd.connected) "OBD · Conectado" else "OBD · Sin conexión", fontSize = 12.sp, maxLines = 1)
        }

        IconButton({ changeVolume(context, -1) }, Modifier.size(38.dp)) { Icon(Icons.AutoMirrored.Filled.VolumeDown, "Bajar volumen", Modifier.size(24.dp)) }
        IconButton({ changeVolume(context, 1) }, Modifier.size(38.dp)) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Subir volumen", Modifier.size(24.dp)) }
        IconButton(onVoice, Modifier.size(44.dp).clip(CircleShape).border(1.dp, Hud.Line, CircleShape)) { Icon(Icons.Default.Mic, "Asistente de voz", Modifier.size(24.dp)) }

        // Titles and the player state come from the media sessions, which the system only shows to enabled listeners.
        if (!hasAccess) {
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                modifier = Modifier.widthIn(max = 170.dp),
            ) { Text("Dar acceso a notificaciones", fontSize = 12.sp, maxLines = 2, textAlign = TextAlign.Center) }
        }
    }
}

private fun clock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/** Thin track with the played part in the accent colour. */
@Composable
private fun Progress(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRoundRect(Color.White.copy(alpha = 0.25f), size = size, cornerRadius = CornerRadius(size.height))
        drawRoundRect(
            color,
            size = Size(size.width * fraction.coerceIn(0f, 1f), size.height),
            cornerRadius = CornerRadius(size.height),
        )
    }
}

/**
 * Decorative rainbow spectrum. It cannot hear the audio (that needs the microphone permission); the bars follow
 * a loop. It steps 20 times a second instead of 60, which looks the same and is easy on weak head-unit GPUs.
 */
@Composable
private fun Spectrum(modifier: Modifier = Modifier) {
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            phase.floatValue = (phase.floatValue + 0.3f) % (2 * PI).toFloat()
        }
    }
    val bars = 56
    val colors = remember { List(bars) { Color.hsv(it * 300f / bars, 0.85f, 1f) } }
    Canvas(modifier) {
        val gap = 2.dp.toPx()
        val w = (size.width - gap * (bars - 1)) / bars
        val p = phase.floatValue // read here, so only this draw is redone each step, not the screen
        for (i in 0 until bars) {
            val level = 0.12f + 0.88f * abs(sin(p * (1 + i % 3) + i * 0.45f))
            val h = size.height * 0.5f * level
            drawRoundRect(
                colors[i],
                topLeft = Offset(i * (w + gap), size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(w / 2),
                alpha = 0.6f,
            )
        }
    }
}
