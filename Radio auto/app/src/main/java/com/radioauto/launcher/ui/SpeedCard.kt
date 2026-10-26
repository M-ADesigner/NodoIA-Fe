package com.radioauto.launcher.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radioauto.launcher.data.Prefs
import com.radioauto.launcher.data.TrackingService
import com.radioauto.launcher.data.TrackingState
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private const val MAX_KMH = 200f

/**
 * Small speed readout for the left edge: speed, a thin bar, and the odometer and trip lines.
 * It has no panel around it so the screen keeps the look of the reference. Turns red over the limit.
 */
@Composable
fun SpeedCard(limit: Int?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(TrackingService.hasLocationPermission(context)) }
    var editing by remember { mutableStateOf(false) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (it) TrackingService.ensureRunning(context)
    }

    val tracking by TrackingState.state.collectAsState()
    val kmh = tracking.kmh
    val over = limit != null && (kmh ?: 0) > limit
    val accent = if (over) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val km = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("es")) }

    Column(modifier, horizontalAlignment = Alignment.Start) {
        if (granted) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(kmh?.toString() ?: "--", fontSize = 40.sp, color = if (over) accent else MaterialTheme.colorScheme.onSurface)
                Text("km/h", fontSize = 13.sp, color = accent, modifier = Modifier.padding(bottom = 7.dp))
            }
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                drawRoundRect(track, size = size, cornerRadius = CornerRadius(size.height))
                drawRoundRect(
                    accent,
                    size = Size(size.width * ((kmh ?: 0) / MAX_KMH).coerceIn(0f, 1f), size.height),
                    cornerRadius = CornerRadius(size.height),
                )
            }
        } else {
            Button(onClick = { request.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) { Text("Activar GPS", fontSize = 13.sp) }
        }
        Text(
            "${km.format((tracking.totalMeters / 1000).toLong())} km",
            Modifier.clip(RoundedCornerShape(8.dp)).clickable { editing = true }.padding(vertical = 4.dp),
            fontSize = 15.sp,
        )
        Text(
            "Viaje %.1f km".format(Locale.forLanguageTag("es"), tracking.tripMeters / 1000),
            Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                TrackingState.resetTrip()
                val s = TrackingState.state.value
                scope.launch { Prefs(context.applicationContext).saveTracking(s.totalMeters, s.tripMeters) }
            },
            fontSize = 12.sp,
            color = accent,
        )
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
