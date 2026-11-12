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
import androidx.compose.foundation.layout.Spacer
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
import com.radioauto.launcher.data.RadioPlayer
import com.radioauto.launcher.data.RadioStatus
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

/** Bottom bar: transport controls, cover, then a colourful spectrum with the track name over it, and volume. */
@Composable
fun MediaBar(onOpenBluetooth: () -> Unit, onApps: () -> Unit, onVoice: () -> Unit, onObd: () -> Unit, demoOn: Boolean = false, onStopDemo: () -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val music by MediaState.state.collectAsState()
    val link by LinkServer.state.collectAsState()
    val linkTrack = link.track
    val c = MaterialTheme.colorScheme
    val hasAccess by MediaState.accessGranted.collectAsState()
    // While the internet radio plays or connects, the bar is its player.
    val radio by RadioPlayer.state.collectAsState()
    val radioOn = radio.status == RadioStatus.PLAYING || radio.status == RadioStatus.LOADING

    Row(
        // A darker strip across the whole width, as in the reference, so the controls and title always read.
        modifier.fillMaxWidth().height(54.dp).hudPanel().padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Transport on the left, big and bare, like the reference.
        IconButton({ if (radioOn) RadioPlayer.previous() else BluetoothMusic.previous(context) }, Modifier.size(44.dp)) { Icon(Icons.Default.SkipPrevious, "Anterior", Modifier.size(28.dp)) }
        IconButton({ if (radioOn) RadioPlayer.toggle() else BluetoothMusic.playPause(context) }, Modifier.size(46.dp).border(1.5.dp, Hud.Accent, CircleShape)) {
            Icon(
                if (radioOn || music.isPlaying || linkTrack?.playing == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                "Reproducir o pausar", Modifier.size(30.dp),
            )
        }
        IconButton({ if (radioOn) RadioPlayer.next() else BluetoothMusic.next(context) }, Modifier.size(44.dp)) { Icon(Icons.Default.SkipNext, "Siguiente", Modifier.size(28.dp)) }

        // Dark pill: small cover, title, artist and a thin progress line.
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight(0.94f)
                .hudPanel(fill = Hud.Bg)
                .clickable { if (!radioOn) onOpenBluetooth() }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(4.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2A2F34), Color(0xFF0E1012)))),
                Alignment.Center,
            ) {
                val art = music.albumArt
                when {
                    art != null -> Image(remember(art) { art.asImageBitmap() }, null, Modifier.size(30.dp), contentScale = ContentScale.Crop)
                    radioOn || linkTrack != null || music.hasMusic -> Icon(Icons.Default.MusicNote, null, Modifier.size(22.dp), Hud.Accent)
                    else -> Icon(Icons.Default.Bluetooth, "Bluetooth", Modifier.size(22.dp), Hud.Accent)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                val muted = Hud.Muted
                when {
                    radioOn -> {
                        Text(radio.current?.name ?: "Radio", fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (radio.status == RadioStatus.PLAYING) "Radio en vivo" else "Conectando…", fontSize = 11.sp, color = muted, maxLines = 1)
                    }

                    linkTrack != null -> {
                        Text(linkTrack.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(linkTrack.artist, fontSize = 11.sp, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    music.hasMusic -> {
                        Text(music.title.ifEmpty { "Sin título" }, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(music.artist, fontSize = 11.sp, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (music.durationMs > 0) {
                            Progress(music.positionMs / music.durationMs.toFloat(), c.primary, Modifier.fillMaxWidth().padding(top = 4.dp).height(3.dp))
                        }
                    }

                    music.btConnected -> {
                        Text("Celular conectado", fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Toca ▶ para reproducir", fontSize = 11.sp, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    else -> {
                        Text("Sin Bluetooth", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Hud.Hot, maxLines = 1)
                        Text("Toca para conectar", fontSize = 11.sp, color = muted, maxLines = 1)
                    }
                }
            }
        }


        // The big home button in the middle opens every app.
        IconButton(onApps, Modifier.size(46.dp).clip(CircleShape).border(1.5.dp, Hud.Accent, CircleShape)) {
            Icon(Icons.Default.Home, "Todas las apps", Modifier.size(30.dp))
        }

        // OBD status chip: green when the car answers.
        val obd by Obd.state.collectAsState()
        Row(
            Modifier.hudPanel(fill = Hud.Bg).clickable { onObd() }.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (obd.connected) Color(0xFF6BE3A0) else Hud.Muted))
            Text(if (obd.connected) "OBD · ON" else "OBD · OFF", fontSize = 11.sp, maxLines = 1)
        }
        // Simulated data are on: one tap to go back to the real adapter.
        if (demoOn) {
            Row(
                Modifier.hudPanel(fill = Hud.Bg).clickable { onStopDemo() }.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Hud.Hot))
                Text("DESACTIVAR DEMO", fontSize = 11.sp, color = Hud.Hot, maxLines = 1)
            }
        }

        IconButton({ changeVolume(context, -1) }, Modifier.size(36.dp)) { Icon(Icons.AutoMirrored.Filled.VolumeDown, "Bajar volumen", Modifier.size(24.dp)) }
        IconButton({ changeVolume(context, 1) }, Modifier.size(36.dp)) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Subir volumen", Modifier.size(24.dp)) }
        IconButton(onVoice, Modifier.size(42.dp).clip(CircleShape).border(1.dp, Hud.Line, CircleShape)) { Icon(Icons.Default.Mic, "Asistente de voz", Modifier.size(24.dp)) }

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
