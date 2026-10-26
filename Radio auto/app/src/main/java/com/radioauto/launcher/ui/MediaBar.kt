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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
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
fun MediaBar(onOpenBluetooth: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val music by MediaState.state.collectAsState()
    val c = MaterialTheme.colorScheme
    val hasAccess by MediaState.accessGranted.collectAsState()

    Row(
        // A darker strip across the whole width, as in the reference, so the controls and title always read.
        modifier.fillMaxWidth().height(76.dp).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton({ BluetoothMusic.previous(context) }, Modifier.size(52.dp)) { Icon(Icons.Default.SkipPrevious, "Anterior", Modifier.size(34.dp)) }
        IconButton(
            { BluetoothMusic.playPause(context) },
            Modifier.size(60.dp).border(2.dp, c.onSurface, CircleShape),
        ) {
            Icon(if (music.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Reproducir o pausar", Modifier.size(36.dp))
        }
        IconButton({ BluetoothMusic.next(context) }, Modifier.size(52.dp)) { Icon(Icons.Default.SkipNext, "Siguiente", Modifier.size(34.dp)) }

        // Rounded square with a pink-to-purple gradient when there is no cover, like the reference.
        Box(
            Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFF6FA5), Color(0xFF8E4DFF))))
                .clickable { onOpenBluetooth() },
            Alignment.Center,
        ) {
            val art = music.albumArt
            when {
                art != null -> Image(remember(art) { art.asImageBitmap() }, null, Modifier.size(50.dp), contentScale = ContentScale.Crop)
                music.hasMusic -> Icon(Icons.Default.MusicNote, null, Modifier.size(28.dp), Color.White)
                else -> Icon(Icons.Default.Bluetooth, "Bluetooth", Modifier.size(28.dp), Color.White)
            }
        }

        Box(Modifier.weight(1f).fillMaxSize().clickable { onOpenBluetooth() }, contentAlignment = Alignment.Center) {
            // The animation only exists while something plays, so it costs no CPU the rest of the time.
            if (music.isPlaying) Spectrum(Modifier.fillMaxSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    music.hasMusic -> {
                        Text(
                            music.title.ifEmpty { "Sin título" },
                            fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, style = OnArt,
                        )
                        Text(music.artist, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, style = OnArt)
                        if (music.durationMs > 0) {
                            Progress(music.positionMs / music.durationMs.toFloat(), c.primary, Modifier.fillMaxWidth(0.8f).height(3.dp))
                        }
                    }

                    music.btConnected -> {
                        Text("Mi celular conectado", fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, style = OnArt)
                        Text(
                            "Toca ▶ o reproduce desde el celular",
                            fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, style = OnArt,
                        )
                    }

                    else -> Text(
                        "Bluetooth: toca para conectar",
                        fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, style = OnArt,
                    )
                }
            }
        }

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
