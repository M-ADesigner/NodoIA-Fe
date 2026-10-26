package com.radioauto.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import com.radioauto.launcher.data.AppInfo
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.radioauto.launcher.data.DayNightMode
import com.radioauto.launcher.data.LauncherSettings
import com.radioauto.launcher.data.Palette

class SettingsActions(
    val onMode: (DayNightMode) -> Unit,
    val onPalette: (Palette) -> Unit,
    val onBtMusicApp: (String?) -> Unit,
    val onKeepLauncher: (Boolean) -> Unit,
    val onSpeedAlert: (Boolean) -> Unit,
    val onSpeedLimit: (Int) -> Unit,
    val onBrightness: (Int) -> Unit,
    val onVolume: (Int) -> Unit,
    val onWifi: () -> Unit,
    val onBluetooth: () -> Unit,
)

@Composable
fun SettingsDialog(settings: LauncherSettings, apps: List<AppInfo>, actions: SettingsActions, onDismiss: () -> Unit) {
    var pickingBtApp by remember { mutableStateOf(false) }

    if (pickingBtApp) {
        BtAppPicker(apps, onPick = { actions.onBtMusicApp(it); pickingBtApp = false }, onDismiss = { pickingBtApp = false })
    }

    AlertDialog(
        // The default dialog width is too narrow for head-unit screens and wraps the colour chips.
        modifier = Modifier.fillMaxWidth(0.7f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text("Ajustes del launcher") },
        text = {
            // Scrolls so it still fits on short 480 px screens.
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Modo")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayNightMode.entries.forEach {
                        FilterChip(settings.mode == it, { actions.onMode(it) }, { Text(it.label) })
                    }
                }
                Text("Color")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Palette.entries.forEach {
                        FilterChip(settings.palette == it, { actions.onPalette(it) }, { Text(it.label) })
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(actions.onWifi) { Text("WiFi") }
                    FilledTonalButton(actions.onBluetooth) { Text("Bluetooth") }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Volumen")
                    FilledTonalButton({ actions.onVolume(-1) }) { Text("−") }
                    FilledTonalButton({ actions.onVolume(+1) }) { Text("+") }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Brillo de pantalla")
                    FilledTonalButton({ actions.onBrightness(-25) }) { Text("−") }
                    FilledTonalButton({ actions.onBrightness(+25) }) { Text("+") }
                }

                SwitchRow("Avisar si paso el límite de velocidad", settings.speedAlert, actions.onSpeedAlert)
                if (settings.speedAlert) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Límite")
                        FilledTonalButton({ actions.onSpeedLimit(settings.speedLimit - 10) }) { Text("−10") }
                        Text("${settings.speedLimit} km/h")
                        FilledTonalButton({ actions.onSpeedLimit(settings.speedLimit + 10) }) { Text("+10") }
                    }
                }

                SwitchRow("Mantener launcher al conectar Bluetooth", settings.keepLauncherOnBluetooth, actions.onKeepLauncher)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("App de música Bluetooth", Modifier.weight(1f))
                    FilledTonalButton({ pickingBtApp = true }) {
                        Text(apps.firstOrNull { it.packageName == settings.btMusicApp }?.label ?: "Automático")
                    }
                }
            }
        },
    )
}

/** Lists the installed apps so the driver can pick the head unit's Bluetooth music app by hand. */
@Composable
private fun BtAppPicker(apps: List<AppInfo>, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("App de música Bluetooth") },
        text = {
            LazyColumn {
                item { PickerRow("Automático", "Prueba las apps Bluetooth conocidas", null) { onPick(null) } }
                items(apps, key = { it.packageName }) { app ->
                    PickerRow(app.label, app.packageName, app.icon) { onPick(app.packageName) }
                }
            }
        },
    )
}

@Composable
private fun PickerRow(label: String, detail: String, icon: ImageBitmap?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Image(icon, null, Modifier.size(32.dp)) else Spacer(Modifier.size(32.dp))
        Column {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // The package name tells apart apps that share a label, which matters on head units.
            Text(detail, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChange)
    }
}
