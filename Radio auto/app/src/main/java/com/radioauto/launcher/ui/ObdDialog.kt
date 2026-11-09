package com.radioauto.launcher.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import com.radioauto.launcher.data.ObdData
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.radioauto.launcher.data.OIL_INTERVAL_KM
import com.radioauto.launcher.data.Obd
import com.radioauto.launcher.data.ObdAlert
import com.radioauto.launcher.data.Tracking
import com.radioauto.launcher.data.TrackingState
import com.radioauto.launcher.data.maintenanceAlert
import com.radioauto.launcher.data.maintenanceDue
import com.radioauto.launcher.data.ObdTone
import com.radioauto.launcher.data.dtcDescription
import com.radioauto.launcher.data.obdAlerts
import java.util.Locale

/** Warnings from the car (hot engine, charging, fault codes) shown over the map; nothing when all is well. */
@Composable
fun ObdAlertBanner(nextOilKm: Int, modifier: Modifier = Modifier) {
    val data by Obd.state.collectAsState()
    val tracking by TrackingState.state.collectAsState()
    // The oil change warning needs no OBD: it counts the odometer.
    val oil = maintenanceAlert(maintenanceDue(tracking.totalMeters / 1000, nextOilKm))?.let { ObdAlert(ObdTone.WARNING, it) }
    val alerts = obdAlerts(data) + listOfNotNull(oil)
    if (alerts.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        alerts.take(2).forEach {
            val color = if (it.tone == ObdTone.ERROR) Color(0xFFD32F2F) else Color(0xFFF9A825)
            Text(
                "⚠ ${it.text}",
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.92f)).padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Every reading, the trip numbers and the fault codes. Opened only while stopped: it is for reading, not for driving. */
@Composable
fun ObdDialog(onDismiss: () -> Unit) {
    val d by Obd.state.collectAsState()
    val hist by Obd.history.collectAsState()
    val dateFmt = remember { java.text.SimpleDateFormat("d MMM HH:mm", Locale.forLanguageTag("es")) }
    val es = Locale.forLanguageTag("es")
    fun v(x: Int?, unit: String) = if (x == null) "--" else "$x$unit"

    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.7f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text(if (d.connected) "Mi auto · ${d.via}" else "Mi auto · sin conexión OBD") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!d.connected) Text("Conecta el adaptador ELM327 y elige la conexión en Ajustes (o prueba el modo Demo).")
                Section("En vivo")
                Line("RPM", v(d.rpm, ""))
                Line("Temperatura del motor", v(d.coolantC, " °C"))
                Line("Temperatura de admisión", v(d.intakeC, " °C"))
                Line("Carga del motor", v(d.loadPct, " %"))
                Line("Acelerador", v(d.throttlePct, " %"))
                Line("Combustible", v(d.fuelPct, " %"))
                Line("Voltaje", d.voltageMv?.let { String.format(es, "%.1f V", it / 1000.0) } ?: "--")
                Line("Encendido hace", d.runtimeS?.let { "${it / 60} min" } ?: "--")
                Section("Viaje actual")
                Line("Duración", "${d.tripSeconds / 60} min")
                Line("Distancia", String.format(es, "%.1f km", d.tripKm))
                Line("Velocidad promedio / máxima", "${d.avgKmh} / ${d.maxKmh} km/h")
                Line("Temp. máxima del motor", v(d.maxCoolantC, " °C"))
                val minV = d.minVoltageMv
                val maxV = d.maxVoltageMv
                Line("Voltaje mín. / máx.", if (minV == null || maxV == null) "--" else String.format(es, "%.1f / %.1f V", minV / 1000.0, maxV / 1000.0))
                Section("Fallas")
                if (d.dtcs.isEmpty()) Text(if (d.connected) "Sin códigos de falla" else "--")
                d.dtcs.forEach { Line(it, dtcDescription(it)) }
                Section("Últimos viajes")
                if (hist.trips.isEmpty()) Text("Aún no hay viajes guardados", fontSize = 14.sp, color = Muted)
                hist.trips.take(10).forEach {
                    Line(
                        dateFmt.format(java.util.Date(it.startMs)),
                        String.format(es, "%.1f km · %d min · prom. %d · máx. %d", it.km, it.seconds / 60, it.avgKmh, it.maxKmh),
                    )
                }
                Section("Registro de fallas")
                if (hist.dtcs.isEmpty()) Text("Sin fallas registradas", fontSize = 14.sp, color = Muted)
                hist.dtcs.take(10).forEach { Line("${it.code} · ${dateFmt.format(java.util.Date(it.atMs))}", dtcDescription(it.code)) }
            }
        },
    )
}

@Composable
private fun Section(title: String) = Text(title, Modifier.padding(top = 8.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold)

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 15.sp)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

enum class ObdDetail(val title: String, val icon: ImageVector) {
    MOTOR("Motor", Icons.Default.Thermostat),
    MANTENCION("Aceite", Icons.Default.Build),
    VIAJE("Viaje", Icons.Default.Route),
    BATERIA("Batería", Icons.Default.BatteryChargingFull),
    COMBUSTIBLE("Combustible", Icons.Default.LocalGasStation),
    ACELERADOR("Acelerador", Icons.Default.Speed),
}

private val Good = Color(0xFF4CAF50)
private val Warn = Color(0xFFF9A825)
private val Bad = Color(0xFFE53935)
private val Muted = Hud.Muted
private val Card = Hud.Mid

/** A coloured stretch of the gauge bar: from..to in the bar's own units. */
private class Zone(val from: Float, val to: Float, val color: Color)

private class DetailModel(
    val big: String,
    val unit: String,
    val status: String,
    val statusColor: Color,
    val value: Float?,
    val min: Float,
    val max: Float,
    val zones: List<Zone>,
    val stats: List<Pair<String, String>>,
)

private fun detailModel(kind: ObdDetail, d: ObdData, t: Tracking, nextOilKm: Int): DetailModel {
    val es = Locale.forLanguageTag("es")
    fun v(x: Int?, unit: String) = if (x == null) "--" else "$x$unit"
    fun volts(mv: Int?) = mv?.let { String.format(es, "%.1f V", it / 1000.0) } ?: "--"
    val engineOn = (d.rpm ?: 0) > 400
    return when (kind) {
        ObdDetail.MOTOR -> {
            val c = d.coolantC
            val (txt, col) = when {
                c == null -> "Sin datos" to Muted
                c < 70 -> "Calentando" to Warn
                c <= 100 -> "Normal" to Good
                c <= 105 -> "Alta" to Warn
                else -> "Muy caliente: detente" to Bad
            }
            DetailModel(
                c?.toString() ?: "--", "°C", txt, col, c?.toFloat(), 40f, 120f,
                listOf(Zone(40f, 70f, Warn), Zone(70f, 100f, Good), Zone(100f, 105f, Warn), Zone(105f, 120f, Bad)),
                listOf(
                    "Rango ideal" to "80–100 °C", "Admisión" to v(d.intakeC, " °C"),
                    "Carga del motor" to v(d.loadPct, " %"), "RPM" to v(d.rpm, ""),
                    "Máx. del viaje" to v(d.maxCoolantC, " °C"), "Encendido hace" to (d.runtimeS?.let { "${it / 60} min" } ?: "--"),
                ),
            )
        }

        ObdDetail.BATERIA -> {
            val mv = d.voltageMv
            val (txt, col) = when {
                mv == null -> "Sin datos" to Muted
                engineOn && mv < 13_200 -> "Alternador cargando poco" to Warn
                engineOn && mv > 14_800 -> "Voltaje alto" to Warn
                engineOn -> "Carga normal" to Good
                mv < 12_200 -> "Batería baja" to Bad
                else -> "Batería bien" to Good
            }
            DetailModel(
                mv?.let { String.format(es, "%.1f", it / 1000.0) } ?: "--", "V", txt, col, mv?.div(1000f), 11f, 15.5f,
                listOf(Zone(11f, 12.2f, Bad), Zone(12.2f, 13.2f, Warn), Zone(13.2f, 14.8f, Good), Zone(14.8f, 15.5f, Warn)),
                listOf(
                    "Motor" to (if (engineOn) "Encendido" else "Apagado"), "Normal encendido" to "13,2–14,8 V",
                    "Normal apagado" to "> 12,2 V", "Mínimo del viaje" to volts(d.minVoltageMv),
                    "Máximo del viaje" to volts(d.maxVoltageMv),
                ),
            )
        }

        ObdDetail.COMBUSTIBLE -> {
            val f = d.fuelPct
            val (txt, col) = when {
                f == null -> "Tu auto no informa el nivel por OBD" to Muted
                f < 15 -> "Reserva: carga bencina" to Bad
                f < 30 -> "Nivel bajo" to Warn
                else -> "Nivel bien" to Good
            }
            DetailModel(
                f?.toString() ?: "--", "%", txt, col, f?.toFloat(), 0f, 100f,
                listOf(Zone(0f, 15f, Bad), Zone(15f, 30f, Warn), Zone(30f, 100f, Good)),
                listOf(
                    "Distancia del viaje" to String.format(es, "%.1f km", d.tripKm), "Duración" to "${d.tripSeconds / 60} min",
                    "Velocidad promedio" to "${d.avgKmh} km/h", "Velocidad máxima" to "${d.maxKmh} km/h",
                ),
            )
        }

        ObdDetail.ACELERADOR -> {
            val t = d.throttlePct
            val txt = when {
                t == null -> "Sin datos"
                t < 10 -> "Suelto"
                t < 60 -> "Normal"
                else -> "A fondo"
            }
            DetailModel(
                t?.toString() ?: "--", "%", txt, if (t == null) Muted else Good, t?.toFloat(), 0f, 100f,
                listOf(Zone(0f, 60f, Good), Zone(60f, 85f, Warn), Zone(85f, 100f, Bad)),
                listOf(
                    "Carga del motor" to v(d.loadPct, " %"), "RPM" to v(d.rpm, ""),
                    "Velocidad" to v(d.speedKmh, " km/h"), "Máx. del viaje" to "${d.maxKmh} km/h",
                ),
            )
        }

        ObdDetail.MANTENCION -> {
            val left = maintenanceDue(t.totalMeters / 1000, nextOilKm)
            val (txt, col) = when {
                left <= 0 -> "Cambio vencido" to Bad
                left < 500 -> "Cambio pronto" to Warn
                else -> "Al día" to Good
            }
            DetailModel(
                String.format(es, "%,d", left.coerceAtLeast(0)), "km para el cambio", txt, col,
                left.coerceIn(0, OIL_INTERVAL_KM).toFloat(), 0f, OIL_INTERVAL_KM.toFloat(),
                listOf(Zone(0f, 500f, Bad), Zone(500f, 1500f, Warn), Zone(1500f, OIL_INTERVAL_KM.toFloat(), Good)),
                listOf(
                    "Próximo cambio" to String.format(es, "%,d km", nextOilKm),
                    "Odómetro" to String.format(es, "%,d km", (t.totalMeters / 1000).toLong()),
                    "Intervalo típico" to "10.000 km", "Se cambia en" to "Ajustes",
                ),
            )
        }

        ObdDetail.VIAJE -> DetailModel(
            String.format(es, "%.1f", t.tripMeters / 1000), "km", if ((t.kmh ?: 0) > 0) "En marcha" else "Detenido",
            if ((t.kmh ?: 0) > 0) Good else Muted, null, 0f, 1f, emptyList(),
            listOf(
                "Velocidad máxima" to "${t.tripMaxKmh} km/h", "Velocidad promedio" to "${t.avgKmh} km/h",
                "Tiempo" to "${t.tripSeconds / 60} min", "Odómetro" to String.format(es, "%,d km", (t.totalMeters / 1000).toLong()),
            ),
        )
    }
}

/** More about one tapped item: a big value judged with a colour, a zoned bar, and the related readings as cards. */
@Composable
fun ObdDetailDialog(kind: ObdDetail, nextOilKm: Int, onSeeAll: () -> Unit, onDismiss: () -> Unit) {
    val d by Obd.state.collectAsState()
    val t by TrackingState.state.collectAsState()
    val m = detailModel(kind, d, t, nextOilKm)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Row(
            Modifier.fillMaxWidth(0.74f).hudPanel(20.dp, Hud.Panel).padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Left: what it is, the number, how it is doing.
            Column(Modifier.width(240.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(38.dp).border(1.dp, Hud.Line, CircleShape), Alignment.Center) {
                        Icon(kind.icon, null, Modifier.size(22.dp), Hud.Accent)
                    }
                    Text(kind.title.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White, letterSpacing = 2.sp)
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(m.big, fontSize = 64.sp, fontWeight = FontWeight.Thin, color = Color.White, lineHeight = 68.sp)
                    Text(m.unit, fontSize = 22.sp, color = Muted, modifier = Modifier.padding(bottom = 10.dp))
                }
                Text(
                    m.status,
                    Modifier.clip(RoundedCornerShape(50)).background(m.statusColor.copy(alpha = 0.2f)).padding(horizontal = 14.dp, vertical = 7.dp),
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = m.statusColor,
                )
                ZoneBar(m)
                if (!d.connected && kind != ObdDetail.MANTENCION && kind != ObdDetail.VIAJE) Text("Sin conexión OBD. Conecta el adaptador o usa el modo Demo.", fontSize = 12.sp, color = Muted)
            }

            // Right: related readings as cards, and the two actions.
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                m.stats.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { (label, value) ->
                            Column(Modifier.weight(1f).hudPanel(12.dp, Card).padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(label.uppercase(), fontSize = 10.sp, color = Muted, maxLines = 1, letterSpacing = 1.2.sp)
                                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Normal, color = Color.White, maxLines = 1, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                    // Maintenance and trip do not come from the car, so there is no OBD page to jump to.
                    if (kind != ObdDetail.MANTENCION && kind != ObdDetail.VIAJE) TextButton(onClick = onSeeAll) { Text("Ver todo") }
                    Button(onClick = onDismiss) { Text("Cerrar") }
                }
            }
        }
    }
}

/** The value on a bar painted with its good/warning/bad zones, with a marker where it stands now. */
@Composable
private fun ZoneBar(m: DetailModel) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(22.dp)) {
        val h = 8.dp.toPx()
        val top = (size.height - h) / 2
        val span = m.max - m.min
        m.zones.forEach {
            val x0 = size.width * ((it.from - m.min) / span)
            val x1 = size.width * ((it.to - m.min) / span)
            drawRect(it.color.copy(alpha = 0.85f), androidx.compose.ui.geometry.Offset(x0, top), androidx.compose.ui.geometry.Size(x1 - x0, h))
        }
        m.value?.let {
            val x = size.width * ((it - m.min) / span).coerceIn(0f, 1f)
            drawCircle(Color.White, 8.dp.toPx(), androidx.compose.ui.geometry.Offset(x, size.height / 2))
            drawCircle(Hud.Panel, 4.dp.toPx(), androidx.compose.ui.geometry.Offset(x, size.height / 2))
        }
    }
}

/** Why the OBD is not connected, in plain words, with the things to try and a retry that skips the wait. */
@Composable
fun ObdHelpDialog(onRetry: () -> Unit, onBluetooth: () -> Unit, onWifi: () -> Unit, onDemo: () -> Unit, onDismiss: () -> Unit) {
    val status by Obd.status.collectAsState()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.62f).hudPanel(20.dp, Hud.Panel).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("OBD · SIN CONEXIÓN", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White, letterSpacing = 2.sp)
            Text(
                status,
                Modifier.fillMaxWidth().hudPanel(12.dp, Hud.Mid).padding(12.dp),
                fontSize = 15.sp, color = Hud.Accent,
            )
            Text("Qué probar", fontSize = 13.sp, color = Muted, letterSpacing = 1.sp)
            listOf(
                "Enchufa el adaptador ELM327 al puerto OBD2 (bajo el tablero) y pon el contacto.",
                "Bluetooth: empareja el adaptador desde el Bluetooth del sistema (suele llamarse OBDII o ELM327).",
                "WiFi: conecta la radio a la red del adaptador (IP 192.168.0.10, puerto 35000).",
                "Si el Bluetooth de tu radio está reservado para el teléfono, usa WiFi.",
            ).forEach { Text("•  $it", fontSize = 14.sp, color = Color.White) }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
                TextButton(onClick = onBluetooth) { Text("Bluetooth") }
                TextButton(onClick = onWifi) { Text("WiFi") }
                TextButton(onClick = onDemo) { Text("Demo") }
                Button(onClick = { onRetry() }) { Text("Reintentar ahora") }
            }
        }
    }
}
