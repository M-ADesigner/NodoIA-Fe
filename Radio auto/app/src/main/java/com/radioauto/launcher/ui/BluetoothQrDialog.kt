package com.radioauto.launcher.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.radioauto.launcher.data.BluetoothMusic
import com.radioauto.launcher.link.LinkBluetooth
import com.radioauto.launcher.link.LinkServer

/**
 * Connection help for the driver, as in AutoPlay: a QR the phone scans to join over Wi-Fi, and the radio's
 * Bluetooth name for phones that pair first. The phone needs the AutoPlay app to read it.
 */
@Composable
fun BluetoothQrDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val link by LinkServer.state.collectAsState()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { LinkServer.startBluetooth() }

    // Once a phone is linked there is nothing left to do here; its music comes through the radio.
    LaunchedEffect(link.connected) { if (link.connected) onDismiss() }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) permission.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    val joinText = link.joinLink?.toString()
    val qr = remember(joinText) { joinText?.let { qrBitmap(it) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text("Conectar el celular") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                if (qr != null) {
                    Image(qr, "Código QR para conectar", Modifier.size(200.dp))
                } else {
                    Text("Sin Wi-Fi: conecta por Bluetooth", Modifier.size(200.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Abre AutoPlay en el celular y escanea el código.")
                    if (link.code.isNotEmpty()) Text("Código: ${link.code}")
                    when (link.bluetooth) {
                        LinkBluetooth.Ready -> Text("Bluetooth: busca «${link.bluetoothName ?: Build.MODEL}», empareja y elígelo en AutoPlay.")
                        LinkBluetooth.Off -> Text("Bluetooth apagado: enciéndelo para conectar sin Wi-Fi.")
                        LinkBluetooth.NeedsPermission -> Text("Falta el permiso de Bluetooth.")
                        LinkBluetooth.Unavailable -> Text("Esta radio no deja usar su Bluetooth desde apps; usa el QR por Wi-Fi.")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton({ makeDiscoverable(context) }) { Text("Hacer visible") }
                        FilledTonalButton({ BluetoothMusic.openSettings(context) }) { Text("Ajustes") }
                    }
                }
            }
        },
    )
}

/** The driver approves each new phone, wherever the launcher is showing. */
@Composable
fun LinkApprovalDialog() {
    val link by LinkServer.state.collectAsState()
    val request = link.request ?: return
    AlertDialog(
        onDismissRequest = { LinkServer.respond(false) },
        confirmButton = { TextButton({ LinkServer.respond(true) }) { Text("Permitir") } },
        dismissButton = { TextButton({ LinkServer.respond(false) }) { Text("Rechazar") } },
        title = { Text("Un celular quiere conectarse") },
        text = { Text(request.deviceName) },
    )
}

private fun makeDiscoverable(context: android.content.Context) {
    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // Some head units ship no screen for this request; the Settings button still works.
    runCatching { context.startActivity(intent) }
}
