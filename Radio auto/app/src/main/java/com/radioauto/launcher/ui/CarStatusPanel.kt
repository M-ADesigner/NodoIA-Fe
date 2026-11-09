package com.radioauto.launcher.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.border
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radioauto.launcher.data.OIL_INTERVAL_KM
import com.radioauto.launcher.data.Obd
import com.radioauto.launcher.data.maintenanceDue
import com.radioauto.launcher.data.Prefs
import com.radioauto.launcher.data.TrackingService
import com.radioauto.launcher.data.TrackingState
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val Muted = Hud.Muted
private val Accent = Hud.Accent
private const val GAUGE_MAX = 240f
private const val SWEEP = 270f
private const val START = 135f

/**
 * Left column like the reference: a speedometer with a needle, then rows for throttle, trip, fuel and engine
 * temperature. The rows fed by the OBD adapter read "--" when no adapter (or no PID) is available.
 */
@Composable
fun CarStatusPanel(limit: Int?, nextOilKm: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tracking by TrackingState.state.collectAsState()
    val obd by Obd.state.collectAsState()
    var editing by remember { mutableStateOf(false) }
    var showObd by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<ObdDetail?>(null) }
    // Details are for reading, so they only open while stopped.
    val km = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("es")) }

    // OBD speed is exact when present; otherwise the GPS speed.
    val kmh = obd.speedKmh ?: tracking.kmh
    val over = limit != null && (kmh ?: 0) > limit
    val needle = if (over) Color(0xFFFF5252) else Accent

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Gauge(kmh, obd.rpm, needle, Modifier.fillMaxWidth().weight(1f).clickable { if ((kmh ?: 0) == 0) showObd = true })

        // Without the OBD the car cards would be dead "--", so the maintenance card takes the throttle's place.
        val remaining = maintenanceDue(tracking.totalMeters / 1000, nextOilKm)
        if (obd.connected) {
            StatRow(
                Icons.Default.Speed, "Acelerador", "En vivo",
                obd.throttlePct?.let { "$it%" } ?: "--", (obd.throttlePct ?: 0) / 100f, Modifier.height(54.dp),
                onClick = { if ((kmh ?: 0) == 0) detail = ObdDetail.ACELERADOR },
            )
        } else {
            StatRow(
                Icons.Default.Build, "Aceite", "Próximo a los ${km.format(nextOilKm.toLong())} km",
                "${km.format(remaining.coerceAtLeast(0).toLong())} km",
                1f - remaining.coerceIn(0, OIL_INTERVAL_KM) / OIL_INTERVAL_KM.toFloat(), Modifier.height(54.dp),
                onClick = { if ((kmh ?: 0) == 0) detail = ObdDetail.MANTENCION },
            )
        }
        StatRow(
            Icons.Default.Route, "Viaje", "${km.format((tracking.totalMeters / 1000).toLong())} km totales",
            "%.1f km".format(Locale.forLanguageTag("es"), tracking.tripMeters / 1000), 0f,
            Modifier.height(54.dp),
            onClick = { editing = true },
            onLongClick = {
                TrackingState.resetTrip()
                val s = TrackingState.state.value
                scope.launch { Prefs(context.applicationContext).saveTracking(s.totalMeters, s.tripMeters) }
            },
        )
        Row(Modifier.fillMaxWidth().height(58.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (obd.connected) {
                MiniStat(Icons.Default.LocalGasStation, "Comb.", obd.fuelPct?.let { "$it%" } ?: "--", Modifier.weight(1f).clickable { if ((kmh ?: 0) == 0) detail = ObdDetail.COMBUSTIBLE })
                MiniStat(Icons.Default.Thermostat, "Motor", obd.coolantC?.let { "$it°" } ?: "--", Modifier.weight(1f).clickable { if ((kmh ?: 0) == 0) detail = ObdDetail.MOTOR })
                MiniStat(
                    Icons.Default.BatteryChargingFull, "Batería",
                    obd.voltageMv?.let { String.format(Locale.forLanguageTag("es"), "%.1fV", it / 1000.0) } ?: "--",
                    Modifier.weight(1f).clickable { if ((kmh ?: 0) == 0) detail = ObdDetail.BATERIA },
                )
            } else {
                // GPS trip numbers: no adapter needed.
                MiniStat(Icons.Default.Speed, "Máx.", "${tracking.tripMaxKmh}", Modifier.weight(1f).clickable { if ((kmh ?: 0) == 0) detail = ObdDetail.VIAJE })
                MiniStat(Icons.Default.Route, "Prom.", "${tracking.avgKmh}", Modifier.weight(1f).clickable { if ((kmh ?: 0) == 0) detail = ObdDetail.VIAJE })
                MiniStat(Icons.Default.Timer, "Tiempo", "${tracking.tripSeconds / 60}m", Modifier.weight(1f).clickable { if ((kmh ?: 0) == 0) detail = ObdDetail.VIAJE })
            }
        }
    }

    if (showObd) ObdDialog { showObd = false }
    detail?.let { kind ->
        ObdDetailDialog(kind, nextOilKm, onSeeAll = { detail = null; showObd = true }, onDismiss = { detail = null })
    }

    if (editing) {
        OdometerDialog(
            currentKm = tracking.totalMeters / 1000,
            onSave = {
                TrackingState.setTotalKm(it)
                val s = TrackingState.state.value
                scope.launch { Prefs(context.applicationContext).saveTracking(s.totalMeters, s.tripMeters) }
                editing = false
            },
            onDismiss = { editing = false },
        )
    }
}

@Composable
private fun Gauge(kmh: Int?, rpm: Int?, needle: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(TrackingService.hasLocationPermission(context)) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (it) TrackingService.ensureRunning(context)
    }
    val track = Hud.Line

    Box(modifier.hudPanel(), Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val d = minOf(size.width, size.height, 190.dp.toPx())
            val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
            val stroke = Stroke(2.5.dp.toPx())
            val inset = 8.dp.toPx()
            val box = Size(d - inset * 2, d - inset * 2)
            val tl = Offset(topLeft.x + inset, topLeft.y + inset)
            drawArc(track, START, SWEEP, false, tl, box, style = stroke)
            val value = ((kmh ?: 0) / GAUGE_MAX).coerceIn(0f, 1f)
            // Soft glow under the value arc, then the arc itself.
            drawArc(needle.copy(alpha = 0.18f), START, SWEEP * value, false, tl, box, style = Stroke(9.dp.toPx()))
            drawArc(needle, START, SWEEP * value, false, tl, box, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            // Fine ticks outside the arc, longer every fifth one.
            val cx = size.width / 2
            val cy = size.height / 2
            val outer = box.width / 2 + 9.dp.toPx()
            for (i in 0..48) {
                val a = Math.toRadians((START + SWEEP * i / 48f).toDouble())
                val long = i % 6 == 0
                val inner = outer - (if (long) 7.dp.toPx() else 3.5.dp.toPx())
                drawLine(
                    if (i / 48f <= value) needle.copy(alpha = 0.9f) else Hud.Line,
                    Offset(cx + (inner * cos(a)).toFloat(), cy + (inner * sin(a)).toFloat()),
                    Offset(cx + (outer * cos(a)).toFloat(), cy + (outer * sin(a)).toFloat()),
                    if (long) 1.6.dp.toPx() else 1.dp.toPx(),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (granted) {
                Text(kmh?.toString() ?: "--", fontSize = 50.sp, fontWeight = FontWeight.Thin, color = Color.White, letterSpacing = 1.sp)
                Text("KM/H", fontSize = 11.sp, color = Muted, letterSpacing = 3.sp)
                Text(if (rpm != null) "$rpm RPM" else "", fontSize = 10.sp, color = Muted, letterSpacing = 1.5.sp)
            } else {
                Button(onClick = { request.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) { Text("Activar GPS", fontSize = 14.sp) }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun StatRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    progress: Float,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .hudPanel(14.dp)
            .then(
                if (onClick != null) Modifier.androidxCombined(onClick, onLongClick) else Modifier,
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(28.dp).border(1.dp, Hud.Line, CircleShape), Alignment.Center) {
                Icon(icon, null, Modifier.size(17.dp), Accent)
            }
            Column(Modifier.weight(1f)) {
                Text(title.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White, letterSpacing = 1.5.sp)
                Text(subtitle, fontSize = 10.sp, color = Muted)
            }
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = Color.White, fontFamily = FontFamily.Monospace)
        }
        Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(1.5.dp).background(Hud.Line)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).background(Accent))
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun Modifier.androidxCombined(onClick: () -> Unit, onLongClick: (() -> Unit)?): Modifier =
    this.then(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick))

@Composable
private fun MiniStat(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxHeight().hudPanel(14.dp).padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(14.dp), Accent)
            Text(label.uppercase(), fontSize = 9.sp, color = Muted, maxLines = 1, letterSpacing = 1.2.sp)
        }
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Normal, color = Color.White, maxLines = 1, fontFamily = FontFamily.Monospace)
    }
}
