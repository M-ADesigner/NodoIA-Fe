package com.radioauto.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.radioauto.launcher.data.AppInfo
import com.radioauto.launcher.data.DayNightMode
import com.radioauto.launcher.data.FontChoice
import com.radioauto.launcher.data.FxSettings
import com.radioauto.launcher.data.SoundFx
import com.radioauto.launcher.data.LauncherSettings
import com.radioauto.launcher.data.ObdConnection
import com.radioauto.launcher.data.Palette

class SettingsActions(
    val onMode: (DayNightMode) -> Unit,
    val onPalette: (Palette) -> Unit,
    val onFont: (FontChoice) -> Unit,
    val onBtMusicApp: (String?) -> Unit,
    val onKeepLauncher: (Boolean) -> Unit,
    val onSpeedAlert: (Boolean) -> Unit,
    val onSpeedLimit: (Int) -> Unit,
    val onObdConnection: (ObdConnection) -> Unit,
    val onObdAddress: (String, Int) -> Unit,
    val onNextOilKm: (Int) -> Unit,
    val onObdBtAddress: (String) -> Unit,
    val onObdInterval: (Int) -> Unit,
    val onBrightness: (Int) -> Unit,
    val onVolume: (Int) -> Unit,
    val onFx: (FxSettings) -> Unit,
    val onWifi: () -> Unit,
    val onBluetooth: () -> Unit,
)

/** The sections of the settings, each shown alone so nothing needs a long scroll. */
private enum class Section(val label: String, val icon: ImageVector) {
    LOOK("APARIENCIA", Icons.Default.Palette),
    SOUND("SONIDO Y PANTALLA", Icons.Default.VolumeUp),
    LINKS("CONEXIONES", Icons.Default.Wifi),
    ALERTS("ALERTAS", Icons.Default.Warning),
    CAR("AUTO Y OBD", Icons.Default.DirectionsCar),
}

@Composable
fun SettingsDialog(settings: LauncherSettings, apps: List<AppInfo>, actions: SettingsActions, onDismiss: () -> Unit) {
    var section by remember { mutableStateOf(Section.LOOK) }
    var pickingBtApp by remember { mutableStateOf(false) }
    var showDiagnostic by remember { mutableStateOf(false) }
    var pickingObdBt by remember { mutableStateOf(false) }
    var showStudio by remember { mutableStateOf(false) }
    if (showStudio) SoundStudio(settings.fx, actions.onFx) { showStudio = false }
    val context = androidx.compose.ui.platform.LocalContext.current

    if (showDiagnostic) MusicDiagnosticDialog { showDiagnostic = false }

    if (pickingBtApp) {
        BtAppPicker(apps, onPick = { actions.onBtMusicApp(it); pickingBtApp = false }, onDismiss = { pickingBtApp = false })
    }

    if (pickingObdBt) {
        val paired = remember { com.radioauto.launcher.data.pairedBluetoothDevices(context) }
        AlertDialog(
            onDismissRequest = { pickingObdBt = false },
            confirmButton = { TextButton(onClick = { pickingObdBt = false }) { Text("Cancelar") } },
            title = { Text("Adaptador Bluetooth OBD") },
            text = {
                LazyColumn {
                    item { PickerRow("Automático", "Busca uno llamado OBD, ELM, V-Link, Vgate...", null) { actions.onObdBtAddress(""); pickingObdBt = false } }
                    if (paired.isEmpty()) item { Text("No hay dispositivos emparejados (o falta el permiso de Bluetooth).", Modifier.padding(vertical = 8.dp)) }
                    items(paired, key = { it.address }) { d ->
                        PickerRow(d.name, d.address, null) { actions.onObdBtAddress(d.address); pickingObdBt = false }
                    }
                }
            },
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Row(
            Modifier
                .fillMaxWidth(0.92f).fillMaxHeight(0.92f).steeringKeys()
                .clip(CutShape).background(Hud.Panel).border(1.dp, Hud.Line, CutShape),
        ) {
            // Categories on the left.
            Column(Modifier.width(210.dp).fillMaxHeight().background(Hud.Bg)) {
                Text("Ajustes", fontFamily = Hud.Display, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Hud.Accent, modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 10.dp))
                Section.entries.forEach { s ->
                    val on = s == section
                    Row(
                        Modifier.fillMaxWidth().height(52.dp)
                            .background(if (on) Hud.Accent.copy(alpha = 0.26f) else Color.Transparent)
                            .clickable { section = s }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.width(3.dp).height(24.dp).background(if (on) Hud.Accent else Color.Transparent))
                        Icon(s.icon, null, Modifier.size(20.dp), if (on) Color.White else Hud.Accent)
                        Text(s.label, fontSize = 11.sp, letterSpacing = 1.sp, color = if (on) Color.White else Hud.Muted, maxLines = 2)
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth().height(56.dp).border(1.dp, Hud.Line).clickable(onClick = onDismiss),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(18.dp), Hud.Accent)
                    Spacer(Modifier.width(8.dp))
                    Text("CERRAR", fontSize = 12.sp, letterSpacing = 2.sp, color = Hud.Accent)
                }
            }

            // The chosen section; scrolls only if it does not fit.
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (section) {
                    Section.LOOK -> {
                        Group("MODO") { Choices(DayNightMode.entries, settings.mode, { it.label }, actions.onMode) }
                        Group("COLOR") {
                            Choices(Palette.entries, settings.palette, { it.label }, actions.onPalette) { p ->
                                Box(
                                    Modifier.size(14.dp).background(Color(p.swatch), CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                )
                            }
                        }
                        Group("TIPO DE LETRA") {
                            Choices(FontChoice.entries, settings.font, { it.label }, actions.onFont, font = { it.family() })
                        }
                        Column(Modifier.fillMaxWidth().clip(CutShape).background(Hud.Mid).border(1.dp, Hud.Line, CutShape).padding(14.dp)) {
                            Text("VISTA PREVIA", fontSize = 10.sp, color = Hud.Muted, letterSpacing = 2.sp)
                            Text("Cities in Dust", fontFamily = Hud.Display, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                            Text("Siouxsie and the Banshees · 66 °C · 62 %", fontSize = 12.sp, color = Hud.Muted)
                        }
                    }

                    Section.SOUND -> {
                        Group("VOLUMEN") { Steps(actions.onVolume, -1, +1, "−", "+") }
                        Group("BRILLO DE PANTALLA") { Steps(actions.onBrightness, -25, +25, "−", "+") }
                        Group("ESTUDIO DE SONIDO") { Btn("Abrir ecualizador y efectos", false) { showStudio = true } }
                    }

                    Section.LINKS -> {
                        Group("RED") {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Btn("WiFi", false) { actions.onWifi() }
                                Btn("Bluetooth", false) { actions.onBluetooth() }
                            }
                        }
                        Group("MÚSICA BLUETOOTH") {
                            Btn(apps.firstOrNull { it.packageName == settings.btMusicApp }?.label ?: "Automático", false) { pickingBtApp = true }
                        }
                        SwitchRow("Mantener launcher al conectar Bluetooth", settings.keepLauncherOnBluetooth, actions.onKeepLauncher)
                        Btn("Diagnóstico de música", false) { showDiagnostic = true }
                    }

                    Section.ALERTS -> {
                        SwitchRow("Avisar si paso el límite de velocidad", settings.speedAlert, actions.onSpeedAlert)
                        if (settings.speedAlert) {
                            Group("LÍMITE") {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Btn("−10", false) { actions.onSpeedLimit(settings.speedLimit - 10) }
                                    Text("${settings.speedLimit} km/h", fontSize = 22.sp, fontWeight = FontWeight.Medium)
                                    Btn("+10", false) { actions.onSpeedLimit(settings.speedLimit + 10) }
                                }
                            }
                        }
                        Group("PRÓXIMO CAMBIO DE ACEITE (KM DEL ODÓMETRO)") {
                            var oil by remember(settings.nextOilKm) { mutableStateOf(settings.nextOilKm.toString()) }
                            OutlinedTextField(
                                value = oil,
                                onValueChange = { v ->
                                    val digits = v.filter { it.isDigit() }.take(6)
                                    oil = digits
                                    digits.toIntOrNull()?.let(actions.onNextOilKm)
                                },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            )
                        }
                    }

                    Section.CAR -> {
                        Group("CONEXIÓN OBD2 (DATOS DEL AUTO)") { Choices(ObdConnection.entries, settings.obdConnection, { it.label }, actions.onObdConnection) }
                        if (settings.obdConnection == ObdConnection.WIFI || settings.obdConnection == ObdConnection.AUTO) {
                            Group("IP:PUERTO DEL ADAPTADOR") {
                                var address by remember(settings.obdHost, settings.obdPort) { mutableStateOf("${settings.obdHost}:${settings.obdPort}") }
                                OutlinedTextField(
                                    value = address,
                                    onValueChange = {
                                        address = it
                                        val host = it.substringBefore(':').trim()
                                        val port = it.substringAfter(':', "35000").trim().toIntOrNull()
                                        if (host.isNotEmpty() && port != null) actions.onObdAddress(host, port)
                                    },
                                    singleLine = true,
                                )
                            }
                        }
                        Group("ADAPTADOR BLUETOOTH") {
                            Btn(if (settings.obdBtAddress.isBlank()) "Automático" else settings.obdBtAddress, false) { pickingObdBt = true }
                        }
                        Group("VELOCIDAD DE LECTURA") {
                            Choices(listOf(300, 500, 1000), settings.obdIntervalMs, { mapOf(300 to "Rápida", 500 to "Normal", 1000 to "Suave")[it] ?: "$it" }, actions.onObdInterval)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 10.sp, color = Hud.Muted, letterSpacing = 2.sp)
        content()
    }
}

/** A row of pills, one lit. [lead] draws something before the label; [font] gives each its own typeface. */
@Composable
private fun <T> Choices(
    items: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit,
    font: (T) -> FontFamily? = { null },
    lead: (@Composable (T) -> Unit)? = null,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Btn(label(item), item == selected, font(item), { lead?.invoke(item) }) { onPick(item) }
        }
    }
}

@Composable
private fun Btn(text: String, selected: Boolean, font: FontFamily? = null, lead: @Composable () -> Unit = {}, onClick: () -> Unit) {
    Row(
        Modifier
            .height(46.dp).clip(CutShape)
            .background(if (selected) Hud.Accent.copy(alpha = 0.3f) else Hud.Mid)
            .border(1.dp, if (selected) Hud.Accent else Hud.Line, CutShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        lead()
        Text(text, fontSize = 13.sp, fontFamily = font, color = if (selected) Color.White else Color(0xFFEDE6F2), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Steps(onStep: (Int) -> Unit, down: Int, up: Int, downLabel: String, upLabel: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Btn(downLabel, false) { onStep(down) }
        Btn(upLabel, false) { onStep(up) }
    }
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
            Text(detail, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Hud.Muted)
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), fontSize = 14.sp)
        Switch(checked, onChange)
    }
}
