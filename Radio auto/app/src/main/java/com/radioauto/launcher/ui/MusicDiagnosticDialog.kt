package com.radioauto.launcher.ui

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.support.v4.media.session.PlaybackStateCompat
import com.radioauto.launcher.data.BtMediaConnector
import com.radioauto.launcher.data.BtProfiles
import kotlinx.coroutines.delay

/** Profile and audio state of the Bluetooth link, refreshed every 2 s, to see why the music does not play. */
@Composable
fun MusicDiagnosticDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var lines by remember { mutableStateOf(diagnostic(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2_000)
            BtProfiles.refresh()
            lines = diagnostic(context)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text("Diagnóstico de música") },
        text = { Column { lines.forEach { Text(it) } } },
    )
}

private fun diagnostic(context: Context): List<String> {
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    fun yn(b: Boolean) = if (b) "conectado" else "no"
    @Suppress("DEPRECATION")
    val bt = BtMediaConnector.controller
    val meta = bt?.metadata
    return listOf(
        "Servicio Bluetooth: ${BtMediaConnector.service?.flattenToShortString() ?: "no encontrado"}",
        "MediaBrowser: ${BtMediaConnector.status}",
        "PlaybackState: ${bt?.playbackState?.let { stateName(it.state) } ?: "sin sesión"}",
        "Metadatos: ${meta?.getString("android.media.metadata.TITLE") ?: "—"} / ${meta?.getString("android.media.metadata.ARTIST") ?: "—"}",
        "HFP (manos libres): ${yn(BtProfiles.hfp)}",
        "A2DP sink (audio): ${yn(BtProfiles.a2dp)}",
        "AVRCP (controles): ${yn(BtProfiles.isConnected(BtProfiles.AVRCP_CONTROLLER))}",
        "AudioManager.getMode(): ${modeName(audio.mode)}",
        "isBluetoothScoOn: ${audio.isBluetoothScoOn}",
        "isBluetoothA2dpOn: ${audio.isBluetoothA2dpOn}",
        "isMusicActive: ${audio.isMusicActive}",
    )
}

private fun modeName(mode: Int) = when (mode) {
    AudioManager.MODE_NORMAL -> "NORMAL"
    AudioManager.MODE_RINGTONE -> "RINGTONE"
    AudioManager.MODE_IN_CALL -> "IN_CALL"
    AudioManager.MODE_IN_COMMUNICATION -> "IN_COMMUNICATION"
    else -> mode.toString()
}

private fun stateName(state: Int) = when (state) {
    PlaybackStateCompat.STATE_NONE -> "NONE"
    PlaybackStateCompat.STATE_STOPPED -> "STOPPED"
    PlaybackStateCompat.STATE_PAUSED -> "PAUSED"
    PlaybackStateCompat.STATE_PLAYING -> "PLAYING"
    PlaybackStateCompat.STATE_BUFFERING -> "BUFFERING"
    PlaybackStateCompat.STATE_ERROR -> "ERROR"
    else -> state.toString()
}
