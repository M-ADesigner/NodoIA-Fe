package com.radioauto.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radioauto.launcher.data.TrackingState
import com.radioauto.launcher.data.Weather
import com.radioauto.launcher.data.WeatherRepository
import kotlinx.coroutines.delay

private val COMPASS = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")

fun compassPoint(bearing: Float?): String =
    if (bearing == null) "--" else COMPASS[(((bearing % 360 + 360) % 360 + 22.5f) / 45f).toInt() % 8]

/** Weather icons keep their own colour (sun yellow, rain blue...) like a phone weather widget. */
private fun Weather.Kind.color(): Color = when (this) {
    Weather.Kind.CLEAR -> Color(0xFFFFC107)
    Weather.Kind.CLOUDY -> Color(0xFFB0BEC5)
    Weather.Kind.RAIN -> Color(0xFF64B5F6)
    Weather.Kind.SNOW -> Color(0xFFE1F5FE)
    Weather.Kind.STORM -> Color(0xFFFFD54F)
}

private fun Weather.Kind.icon(): ImageVector = when (this) {
    Weather.Kind.CLEAR -> Icons.Default.WbSunny
    Weather.Kind.CLOUDY -> Icons.Default.Cloud
    Weather.Kind.RAIN -> Icons.Default.WaterDrop
    Weather.Kind.SNOW -> Icons.Default.AcUnit
    Weather.Kind.STORM -> Icons.Default.Thunderstorm
}

/** Casa / Trabajo shortcuts. [onEdit] is called on long press to change the address. */
class NavShortcuts(
    val onGo: (home: Boolean) -> Unit,
    val onEdit: (home: Boolean) -> Unit,
)

/**
 * Weather on the left; settings, Casa, Trabajo and camera in the middle;
 * compass, GPS satellites and the all-apps button on the right.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TopBar(
    nav: NavShortcuts,
    onOpenSettings: () -> Unit,
    onOpenApps: () -> Unit,
    onCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tracking by TrackingState.state.collectAsState()
    var weather by remember { mutableStateOf<Weather?>(null) }

    // One tiny request every 30 minutes (5 if it failed, e.g. no internet); waits for the first GPS position.
    LaunchedEffect(Unit) {
        while (true) {
            val t = TrackingState.state.value
            if (t.lat == null || t.lon == null) {
                delay(15_000)
                continue
            }
            val w = WeatherRepository.fetch(t.lat, t.lon)
            if (w != null) weather = w
            delay(if (w != null) 30 * 60_000L else 5 * 60_000L)
        }
    }

    val tint = MaterialTheme.colorScheme.primary
    Row(
        modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            weather?.let {
                Icon(it.kind.icon(), null, Modifier.size(32.dp), it.kind.color())
                Text("${it.tempC}°", fontSize = 26.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CircleButton(Icons.Default.Settings, "Ajustes del launcher", onOpenSettings)
            PlacesMenu(nav)
            Box(Modifier.width(1.dp).height(30.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)))
            IconButton(onCamera, Modifier.size(48.dp)) { Icon(Icons.Default.CameraAlt, "Cámara", Modifier.size(28.dp)) }
        }

        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.End),
        ) {
            Text(compassPoint(tracking.bearing), fontSize = 22.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.SatelliteAlt, null, Modifier.size(24.dp))
                Text(tracking.satellites?.toString() ?: "--", fontSize = 20.sp)
            }
            CircleButton(Icons.Default.GridView, "Todas las apps", onOpenApps, tint = tint)
        }
    }
}

@Composable
private fun CircleButton(icon: ImageVector, description: String, onClick: () -> Unit, tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, Modifier.size(26.dp), tint) }
}

/** One pin icon, like the reference; tapping it lists Casa and Trabajo, each with a pencil to set the address. */
@Composable
private fun PlacesMenu(nav: NavShortcuts) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }, Modifier.size(48.dp)) {
            Icon(Icons.Default.LocationOn, "Ir a Casa o Trabajo", Modifier.size(28.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(true to ("Casa" to Icons.Default.Home), false to ("Trabajo" to Icons.Default.Work)).forEach { (home, item) ->
                DropdownMenuItem(
                    text = { Text("Ir a ${item.first}", fontSize = 18.sp) },
                    leadingIcon = { Icon(item.second, null) },
                    trailingIcon = {
                        IconButton({ open = false; nav.onEdit(home) }) { Icon(Icons.Default.Edit, "Cambiar dirección") }
                    },
                    onClick = { open = false; nav.onGo(home) },
                )
            }
        }
    }
}
