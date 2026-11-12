package com.radioauto.launcher.ui

import androidx.compose.foundation.border
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.radioauto.launcher.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.radioauto.launcher.data.TrackingState
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.File

private val SANTIAGO = GeoPoint(-33.4489, -70.6693)

/**
 * Own OpenStreetMap view on the right (Waze and Google Maps cannot be embedded). Dark by inverting the tiles,
 * centred on the car, with an arrow that turns with the heading and big buttons for the usual destinations.
 */
@Composable
fun MapPanel(
    onSearch: () -> Unit,
    onHome: () -> Unit,
    onWork: () -> Unit,
    onChooseApp: () -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    // Only what the map needs, so GPS speed or odometer changes do not redraw it.
    val position by remember { TrackingState.state.map { it.lat to it.lon }.distinctUntilChanged() }
        .collectAsState(initial = null to null)
    val heading by remember { TrackingState.state.map { ((it.bearing ?: 0f) / 5).toInt() * 5f }.distinctUntilChanged() }
        .collectAsState(initial = 0f)
    val owner = LocalLifecycleOwner.current

    val map = remember {
        // Tiles are cached in the app's own cache dir: no storage permission, and the user agent identifies the app to OSM.
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osm")
            osmdroidTileCache = File(context.cacheDir, "osm/tiles")
        }
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            // Invert, then drop the colour: a monochrome night map that matches the rest of the screen.
            val mono = android.graphics.ColorMatrix(
                floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f),
            ).apply {
                postConcat(android.graphics.ColorMatrix().apply { setSaturation(0f) })
                // Dim it so the map sits back and the readouts stand out.
                postConcat(android.graphics.ColorMatrix().apply { setScale(0.72f, 0.72f, 0.72f, 1f) })
            }
            overlayManager.tilesOverlay.setColorFilter(android.graphics.ColorMatrixColorFilter(mono))
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(17.0)
            controller.setCenter(SANTIAGO)
        }
    }

    // The map only draws while the launcher is on screen, which keeps weak head-unit CPUs free.
    DisposableEffect(owner, map) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            map.onDetach()
        }
    }

    val online by rememberOnline()

    Box(modifier.hudPanel(Hud.Radius, Hud.Panel)) {
        if (online) {
            AndroidView(
                factory = { map },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    val (lat, lon) = position
                    if (lat != null && lon != null) view.controller.animateTo(GeoPoint(lat, lon))
                },
            )

            // The car stays in the middle; only the arrow turns.
            Icon(
                Icons.Default.Navigation, "Mi posición",
                Modifier.align(Alignment.Center).size(34.dp).rotate(heading),
                Hud.Accent,
            )
        } else {
            // No internet means no tiles: show the car instead of an empty grey box.
            Image(
                painterResource(R.drawable.i20_active), "Hyundai i20 Active",
                Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0.15f) }),
            )
            Text(
                "Sin conexión · mapa no disponible",
                Modifier.align(Alignment.BottomStart).padding(12.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xAA000000)).padding(horizontal = 10.dp, vertical = 5.dp),
                fontSize = 12.sp, color = Color.White,
            )
        }

        Box(Modifier.align(Alignment.TopCenter).padding(10.dp)) { overlay() }

        Column(
            Modifier.align(Alignment.CenterEnd).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MapButton(Icons.Default.Search, "Buscar destino", onSearch)
            MapButton(Icons.Default.Home, "Ir a Casa", onHome)
            MapButton(Icons.Default.Work, "Ir a Trabajo", onWork)
            MapButton(Icons.Default.Apps, "Elegir app de navegación", onChooseApp)
        }
    }
}

/** True while the device has a network that really reaches the internet (not just a WiFi with no uplink). */
@Composable
private fun rememberOnline(): State<Boolean> {
    val context = LocalContext.current
    val cm = remember { context.getSystemService(ConnectivityManager::class.java) }
    fun now(): Boolean = cm?.getNetworkCapabilities(cm.activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    val online = remember { mutableStateOf(now()) }
    DisposableEffect(cm) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            // Trust what the callback reports: right after a loss the system may still list the old network as active.
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                online.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
            override fun onLost(network: Network) { online.value = false }
        }
        runCatching { cm?.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { cm?.unregisterNetworkCallback(callback) } }
    }
    return online
}

@Composable
private fun MapButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(Color(0xCC000000)).border(1.dp, Hud.Line, CircleShape).clickable(onClick = onClick),
        Alignment.Center,
    ) { Icon(icon, description, Modifier.size(22.dp), Hud.Accent) }
}
