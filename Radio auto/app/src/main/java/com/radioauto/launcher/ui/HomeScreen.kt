package com.radioauto.launcher.ui

import android.content.Intent
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.radioauto.launcher.data.AppInfo
import com.radioauto.launcher.data.AppRepository
import com.radioauto.launcher.data.LauncherSettings
import com.radioauto.launcher.data.BluetoothMusic
import com.radioauto.launcher.data.MediaState
import com.radioauto.launcher.data.Navigator
import com.radioauto.launcher.data.Prefs
import com.radioauto.launcher.data.defaultDock
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    repository: AppRepository,
    prefs: Prefs,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var showApps by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<Boolean?>(null) } // true = Casa, false = Trabajo

    val byPackage = apps.associateBy { it.packageName }
    val dockPackages = settings.dock ?: defaultDock(apps)
    val dockApps = dockPackages.mapNotNull { byPackage[it] }
    // Favourites first (map, radio, music...), then everything else in alphabetical order.
    val listApps = dockApps + apps.filter { it.packageName !in dockPackages }

    fun toggleDock(app: AppInfo) {
        val next = if (app.packageName in dockPackages) dockPackages - app.packageName
        else (dockPackages + app.packageName).takeLast(6)
        scope.launch { prefs.setDock(next) }
    }

    fun goTo(home: Boolean) {
        val address = if (home) settings.homeAddress else settings.workAddress
        if (address.isBlank()) editingAddress = home
        else Navigator.navigateTo(context, address, byPackage.keys)
    }

    fun launchIntent(intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun voiceAssistant() {
        runCatching { context.startActivity(Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { launchIntent(Intent(Intent.ACTION_ASSIST)) }
    }

    // The session of the chosen Bluetooth app wins over any other player.
    LaunchedEffect(settings.btMusicApp) { MediaState.setPreferred(settings.btMusicApp) }

    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    Box(
        Modifier
            .fillMaxSize()
            // Drawn once per size/colour change, not per frame: two soft glows, seen when no cover is available.
            .drawWithCache {
                val w = size.width
                val h = size.height
                val glowA = Brush.radialGradient(
                    listOf(primary.copy(alpha = 0.28f), Color.Transparent), Offset(w * 0.2f, h * 0.2f), w * 0.55f,
                )
                val glowB = Brush.radialGradient(
                    listOf(Color(0xFF7C4DFF).copy(alpha = 0.26f), Color.Transparent), Offset(w * 0.5f, h * 0.95f), w * 0.6f,
                )
                onDrawBehind {
                    drawRect(background)
                    drawRect(glowA)
                    drawRect(glowB)
                }
            }
    ) {
        ArtBackground(Modifier.fillMaxSize())

        // Shade so text reads over any cover: darker toward the app list, the top bar and the bottom bar.
        Box(
            Modifier.fillMaxSize().drawWithCache {
                val side = Brush.horizontalGradient(
                    listOf(Color.Transparent, background.copy(alpha = 0.85f)), size.width * 0.35f, size.width * 0.7f,
                )
                val top = Brush.verticalGradient(listOf(background.copy(alpha = 0.7f), Color.Transparent), 0f, size.height * 0.2f)
                val bottom = Brush.verticalGradient(
                    listOf(Color.Transparent, background.copy(alpha = 0.8f)), size.height * 0.75f, size.height,
                )
                onDrawBehind {
                    drawRect(side)
                    drawRect(top)
                    drawRect(bottom)
                }
            }
        )

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopBar(
                nav = NavShortcuts(onGo = ::goTo, onEdit = { editingAddress = it }),
                onOpenSettings = { showSettings = true },
                onOpenApps = { showApps = true },
                onCamera = { launchIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) },
            )

            // Same proportions as the reference: the app panel takes the right 60 %, the dial sits over its left
            // edge, the mic is at the far left, and the speed readout is tucked in the bottom-left corner.
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val w = maxWidth
                val h = maxHeight

                AppArcList(
                    listApps,
                    onLaunch = { repository.launch(it.packageName) },
                    onLongPress = ::toggleDock,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = w * 0.025f)
                        .width(w * 0.6f)
                        .fillMaxHeight(0.96f),
                )

                ClockDial(
                    diameter = minOf(w * 0.28f, h * 0.94f),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = w * 0.14f),
                )

                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), CircleShape)
                        .clickable { voiceAssistant() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Mic, "Asistente de voz", Modifier.size(28.dp)) }

                SpeedCard(
                    limit = if (settings.speedAlert) settings.speedLimit else null,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 6.dp).width(w * 0.12f),
                )
            }

            MediaBar(onOpenBluetooth = { BluetoothMusic.open(context, settings.btMusicApp) })
        }

        // Only composed while open, so the full app grid costs nothing on the home screen.
        if (showApps) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(background.copy(alpha = 0.97f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        showApps = false
                    },
            ) {
                AppGrid(
                    apps = apps,
                    onLaunch = {
                        showApps = false
                        repository.launch(it.packageName)
                    },
                    onLongPress = ::toggleDock,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxSize()
                        .glass(RoundedCornerShape(32.dp), strength = 0.4f),
                )
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            apps = apps,
            actions = SettingsActions(
                onMode = { scope.launch { prefs.setMode(it) } },
                onPalette = { scope.launch { prefs.setPalette(it) } },
                onBtMusicApp = { scope.launch { prefs.setBtMusicApp(it) } },
                onKeepLauncher = { scope.launch { prefs.setKeepLauncherOnBluetooth(it) } },
                onSpeedAlert = { scope.launch { prefs.setSpeedAlert(it) } },
                onSpeedLimit = { scope.launch { prefs.setSpeedLimit(it) } },
                onBrightness = { changeBrightness(context, it) },
                onVolume = { changeVolume(context, it) },
                onWifi = {
                    // The internet panel is Android 10+; older versions open the Wi-Fi settings page.
                    val action = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                    } else {
                        android.provider.Settings.ACTION_WIFI_SETTINGS
                    }
                    launchIntent(Intent(action))
                },
                onBluetooth = { launchIntent(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) },
            ),
            onDismiss = { showSettings = false },
        )
    }

    editingAddress?.let { home ->
        AddressDialog(
            title = if (home) "Dirección de Casa" else "Dirección de Trabajo",
            current = if (home) settings.homeAddress else settings.workAddress,
            onSave = { address ->
                scope.launch { prefs.setAddress(home, address) }
                editingAddress = null
                Navigator.navigateTo(context, address, byPackage.keys)
            },
            onDismiss = { editingAddress = null },
        )
    }
}
