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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.radioauto.launcher.link.LinkServer
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
    var showBtQr by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<Boolean?>(null) } // true = Casa, false = Trabajo

    val byPackage = apps.associateBy { it.packageName }
    val dockPackages = settings.dock ?: defaultDock(apps)
    // Driving mode: above 10 km/h video and web shortcuts are hidden (still reachable in "Todas las apps").
    val tracking by com.radioauto.launcher.data.TrackingState.state.collectAsState()
    val moving = (tracking.kmh ?: 0) > 10
    val distracting = setOf("com.google.android.youtube", "com.android.chrome")
    val dockApps = dockPackages.mapNotNull { byPackage[it] }.filter { !moving || it.packageName !in distracting }
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
            .onFailure { launchIntent(Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)) }
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
                val aurora = Brush.linearGradient(
                    listOf(Color(0xFF3A1D4E), Color(0xFF5A2A5A), Color(0xFF6B3550)), Offset(0f, 0f), Offset(w, h),
                )
                val glowA = Brush.radialGradient(listOf(Color(0xFFFF8FB1).copy(alpha = 0.40f), Color.Transparent), Offset(w * 0.12f, h * 0.15f), w * 0.5f)
                val glowB = Brush.radialGradient(listOf(Color(0xFFB39DDB).copy(alpha = 0.38f), Color.Transparent), Offset(w * 0.95f, h * 0.9f), w * 0.55f)
                val glowC = Brush.radialGradient(listOf(Color(0xFFFFAB80).copy(alpha = 0.32f), Color.Transparent), Offset(w * 0.55f, h * 0.05f), w * 0.4f)
                onDrawBehind {
                    drawRect(background)
                    drawRect(aurora)
                    drawRect(glowA)
                    drawRect(glowB)
                    drawRect(glowC)
                }
            }
    ) {
        ArtBackground(Modifier.fillMaxSize())

        // Shade so text reads over any cover: darker toward the app list, the top bar and the bottom bar.
        Box(
            Modifier.fillMaxSize().drawWithCache {
                val side = Brush.horizontalGradient(
                    listOf(Color.Transparent, background.copy(alpha = 0.25f)), size.width * 0.35f, size.width * 0.7f,
                )
                val top = Brush.verticalGradient(listOf(background.copy(alpha = 0.35f), Color.Transparent), 0f, size.height * 0.2f)
                val bottom = Brush.verticalGradient(
                    listOf(Color.Transparent, background.copy(alpha = 0.6f)), size.height * 0.75f, size.height,
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

            // Left: time and speed. Right: navigation, the three things done most (home, work, voice), then favourites.
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                val w = maxWidth
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        Modifier.width(w * 0.30f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        ClockDial(diameter = minOf(w * 0.28f, 210.dp), modifier = Modifier)
                        SpeedCard(
                            limit = if (settings.speedAlert) settings.speedLimit else null,
                            modifier = Modifier.fillMaxWidth(0.8f),
                        )
                    }

                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NavCard(onOpen = { repository.launch(it) }, modifier = Modifier.fillMaxWidth())

                        Row(Modifier.fillMaxWidth().weight(0.8f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ActionTile(Icons.Default.Home, "Casa", { goTo(true) }, Modifier.weight(1f).fillMaxHeight(), hue = 15f, onLongClick = { editingAddress = true })
                            ActionTile(Icons.Default.Work, "Trabajo", { goTo(false) }, Modifier.weight(1f).fillMaxHeight(), hue = 330f, onLongClick = { editingAddress = false })
                            ActionTile(Icons.Default.Mic, "Asistente", ::voiceAssistant, Modifier.weight(1f).fillMaxHeight(), hue = 270f)
                        }

                        // Favourites (long press in the full list to change them) plus the button to everything else.
                        val tiles: List<AppInfo?> = (dockApps.take(5) + listOf<AppInfo?>(null)).let { it.take(6) }
                        tiles.chunked(3).forEachIndexed { rowIdx, row ->
                            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEachIndexed { col, app ->
                                    if (app == null) ActionTile(Icons.Default.GridView, "Todas las apps", { showApps = true }, Modifier.weight(1f).fillMaxHeight(), hue = 200f)
                                    else AppTile(app, { repository.launch(app.packageName) }, { toggleDock(app) }, Modifier.weight(1f).fillMaxHeight(), hue = listOf(35f, 330f, 270f, 15f, 200f, 300f)[(rowIdx * 3 + col) % 6])
                                }
                                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }

            MediaBar(onOpenBluetooth = {
                // Without a phone linked, offer the QR pairing help; with one, the system screen manages it.
                if (MediaState.state.value.btConnected || LinkServer.state.value.connected) BluetoothMusic.openSettings(context) else showBtQr = true
            })
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

    if (showBtQr) BluetoothQrDialog { showBtQr = false }
    LinkApprovalDialog()

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
