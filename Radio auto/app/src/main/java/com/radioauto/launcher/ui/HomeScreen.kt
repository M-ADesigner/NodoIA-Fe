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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.radioauto.launcher.data.AppInfo
import com.radioauto.launcher.data.AppRepository
import com.radioauto.launcher.data.LauncherSettings
import com.radioauto.launcher.data.BluetoothMusic
import com.radioauto.launcher.data.MediaState
import com.radioauto.launcher.data.Navigator
import com.radioauto.launcher.data.Prefs
import com.radioauto.launcher.link.LinkServer
import com.radioauto.launcher.data.MAP_PACKAGES
import com.radioauto.launcher.data.defaultDock
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Extra height taken off the window at the bottom; none needed now that it starts below the tabs. */
private const val WINDOW_TITLE_PX = 0

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
    var showObdHelp by remember { mutableStateOf(false) }
    var showNoNet by remember { mutableStateOf(false) }
    var showObdFull by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<Boolean?>(null) } // true = Casa, false = Trabajo

    val byPackage = apps.associateBy { it.packageName }

    // Where the right-hand panel is on the screen: a navigator opened from here gets a window of exactly that size.
    val view = androidx.compose.ui.platform.LocalView.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val panelBounds = remember { arrayOfNulls<androidx.compose.ui.geometry.Rect>(1) }
    fun screenRect(): android.graphics.Rect? {
        val b = panelBounds[0] ?: return null
        val at = IntArray(2)
        view.getLocationOnScreen(at)
        // The status bar comes back while the navigator is open and pushes the launcher down, so count it even if hidden now.
        val bar = androidx.core.view.ViewCompat.getRootWindowInsets(view)
            ?.getInsetsIgnoringVisibility(androidx.core.view.WindowInsetsCompat.Type.statusBars())?.top ?: 0
        val tabs = (70 * density.density).toInt()
        return android.graphics.Rect((at[0] + b.left).toInt(), (at[1] + maxOf(b.top.toInt(), bar)) + tabs, (at[0] + b.right).toInt(), (at[1] + b.bottom).toInt() - WINDOW_TITLE_PX)
    }
    val dockPackages = settings.dock ?: defaultDock(apps)
    // Driving mode: above 10 km/h video and web shortcuts are hidden from "Todas las apps". Only the flip is observed,
    // so the screen is not rebuilt on every GPS fix.
    val moving by remember {
        com.radioauto.launcher.data.TrackingState.state.map { (it.kmh ?: 0) > 10 }.distinctUntilChanged()
    }.collectAsState(initial = false)
    val distracting = setOf("com.google.android.youtube", "com.android.chrome")

    fun toggleDock(app: AppInfo) {
        val next = if (app.packageName in dockPackages) dockPackages - app.packageName
        else (dockPackages + app.packageName).takeLast(6)
        scope.launch { prefs.setDock(next) }
    }

    fun goTo(home: Boolean) {
        val address = if (home) settings.homeAddress else settings.workAddress
        if (address.isBlank()) editingAddress = home
        else Navigator.navigateTo(context, address, byPackage.keys, settings.navApp, screenRect())
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
    // When music starts (from the phone, by Bluetooth or the link) the player takes the big panel; after that the driver is free to switch.
    val nowPlaying by MediaState.state.collectAsState()
    val linkNow by LinkServer.state.collectAsState()
    val isPlayingNow = (linkNow.track?.playing ?: nowPlaying.isPlaying)
    LaunchedEffect(isPlayingNow) {
        // The phone's music and the internet radio never play together: whoever starts last wins.
        if (isPlayingNow) com.radioauto.launcher.data.RadioPlayer.stop()
        if (isPlayingNow && settings.panel != PanelMode.MUSIC.name) prefs.setPanel(PanelMode.MUSIC.name)
    }
    // The sound effects follow the settings, and come back on their own after a restart.
    LaunchedEffect(settings.fx) { com.radioauto.launcher.data.SoundFx.apply(context, settings.fx) }

    val background = MaterialTheme.colorScheme.background
    // The OBD adapter over Bluetooth needs the nearby-devices permission on Android 12+; asked when the driver picks it.
    val btPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(settings.obdConnection, settings.obdHost, settings.obdPort, settings.obdBtAddress, settings.obdIntervalMs) {
        com.radioauto.launcher.data.Obd.configure(
            context, settings.obdConnection, settings.obdHost, settings.obdPort, settings.obdBtAddress, settings.obdIntervalMs,
        )
    }

    // The navigator the driver picked, else the first map app found; it opens in a window beside the launcher.
    fun openMapApp() {
        val pkg = settings.navApp.takeIf { it in byPackage } ?: MAP_PACKAGES.firstOrNull { it in byPackage } ?: return
        com.radioauto.launcher.embedded.EmbeddedAppManager.open(context, pkg, screenRect())
    }

    val embedded by com.radioauto.launcher.embedded.EmbeddedAppManager.state.collectAsState()

    val mapChoices = remember(apps) {
        com.radioauto.launcher.data.MapWindow.NAV_APPS.map { (pkg, name) ->
            val app = apps.firstOrNull { it.packageName == pkg }
            MapChoice(pkg, name, app?.icon, "NAVEGACIÓN COMPLETA", installed = app != null)
        } + MapChoice("OSM", "Mapa propio", null, "SIN INTERNET: OK")
    }

    Box(
        Modifier.fillMaxSize().background(background).drawWithCache {
            // Fine horizontal lines over everything, drawn from a cache so they cost nothing per frame.
            val step = 3.dp.toPx()
            val color = Color.White.copy(alpha = 0.025f)
            onDrawWithContent {
                drawContent()
                var y = 0f
                while (y < size.height) {
                    drawLine(color, Offset(0f, y), Offset(size.width, y), 1f)
                    y += step
                }
            }
        },
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp)) {
            // Left: car status. Right: the map with the next turn on top and the usual destinations on the side.
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CarStatusPanel(
                    limit = if (settings.speedAlert) settings.speedLimit else null,
                    nextOilKm = settings.nextOilKm,
                    modifier = Modifier.width(190.dp).fillMaxHeight(),
                )
                RightPanel(
                    mode = PanelMode.from(settings.panel),
                    onMode = { scope.launch { prefs.setPanel(it.name) } },
                    onSettings = { showSettings = true },
                    modifier = Modifier.weight(1f).fillMaxHeight().onGloballyPositioned { panelBounds[0] = it.boundsInWindow() },
                ) {
                    if (settings.mapView == "OSM") {
                        MapPanel(
                            onSearch = ::openMapApp,
                            onHome = { goTo(true) },
                            onWork = { goTo(false) },
                            onChooseApp = { scope.launch { prefs.setMapView("CHOOSE") } },
                            modifier = Modifier.fillMaxSize(),
                            overlay = {
                                Column(Modifier.width(340.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ObdAlertBanner(settings.nextOilKm)
                                }
                            },
                        )
                    } else if (embedded !is com.radioauto.launcher.embedded.EmbeddedAppState.Idle) {
                        val pkg = when (val e = embedded) {
                            is com.radioauto.launcher.embedded.EmbeddedAppState.Loading -> e.packageName
                            is com.radioauto.launcher.embedded.EmbeddedAppState.NotInstalled -> e.packageName
                            is com.radioauto.launcher.embedded.EmbeddedAppState.Opened -> e.packageName
                            is com.radioauto.launcher.embedded.EmbeddedAppState.Failed -> e.packageName
                            else -> ""
                        }
                        val manager = com.radioauto.launcher.embedded.EmbeddedAppManager
                        EmbeddedAppContainer(
                            state = embedded,
                            app = byPackage[pkg],
                            actions = EmbeddedAppActions(
                                onOpen = { manager.open(context, pkg, screenRect()) },
                                onExpand = { manager.expand(context) },
                                onBack = { manager.backToLauncher(context) },
                                onInstall = { com.radioauto.launcher.data.MapWindow.install(context, pkg) },
                                onChange = { manager.dismiss() },
                            ),
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        MapChooser(
                            mapChoices, settings.navApp,
                            onPick = { choice ->
                                if (!choice.installed) {
                                    com.radioauto.launcher.data.MapWindow.install(context, choice.id)
                                } else if (choice.id == "OSM") {
                                    scope.launch { prefs.setNav("OSM", "OSM") }
                                } else if (!hasInternet(context)) {
                                    // Waze and Google Maps need data; the own map is the one that works offline.
                                    showNoNet = true
                                } else {
                                    scope.launch { prefs.setNav(choice.id, "CHOOSE") }
                                    com.radioauto.launcher.embedded.EmbeddedAppManager.open(context, choice.id, screenRect())
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            MediaBar(
                onOpenBluetooth = {
                    // Without a phone linked, offer the QR pairing help; with one, the system screen manages it.
                    if (MediaState.state.value.btConnected || LinkServer.state.value.connected) BluetoothMusic.openSettings(context) else showBtQr = true
                },
                onApps = { showApps = true },
                onVoice = ::voiceAssistant,
                // Connected: the full readings. Not connected: why, and what to try.
                onObd = { if (com.radioauto.launcher.data.Obd.state.value.connected) showObdFull = true else showObdHelp = true },
                demoOn = settings.obdConnection == com.radioauto.launcher.data.ObdConnection.DEMO,
                onStopDemo = { scope.launch { prefs.setObdConnection(com.radioauto.launcher.data.ObdConnection.AUTO) } },
            )
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
                    apps = if (moving) apps.filter { it.packageName !in distracting } else apps,
                    onLaunch = {
                        showApps = false
                        // Any app opens beside the launcher when the device allows it.
                        com.radioauto.launcher.embedded.EmbeddedAppManager.open(context, it.packageName, screenRect())
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
    if (showObdFull) ObdDialog { showObdFull = false }
    if (showObdHelp) {
        ObdHelpDialog(
            onRetry = { com.radioauto.launcher.data.Obd.retryNow() },
            onBluetooth = { launchIntent(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) },
            onWifi = { launchIntent(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)) },
            onDemo = {
                scope.launch { prefs.setObdConnection(com.radioauto.launcher.data.ObdConnection.DEMO) }
                showObdHelp = false
            },
            onDismiss = { showObdHelp = false },
        )
    }
    LinkApprovalDialog()
    if (showNoNet) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNoNet = false },
            title = { androidx.compose.material3.Text("Sin internet") },
            text = { androidx.compose.material3.Text("Este mapa necesita internet. Conéctate a una red WiFi o activa los datos del celular, o usa «Mapa propio», que funciona sin conexión.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showNoNet = false
                    val action = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY else android.provider.Settings.ACTION_WIFI_SETTINGS
                    launchIntent(Intent(action))
                }) { androidx.compose.material3.Text("Conectar") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { showNoNet = false }) { androidx.compose.material3.Text("Cerrar") } },
        )
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            apps = apps,
            actions = SettingsActions(
                onMode = { scope.launch { prefs.setMode(it) } },
                onPalette = { scope.launch { prefs.setPalette(it) } },
                onFont = { scope.launch { prefs.setFont(it) } },
                onFx = { scope.launch { prefs.setFx(it) } },
                onBtMusicApp = { scope.launch { prefs.setBtMusicApp(it) } },
                onKeepLauncher = { scope.launch { prefs.setKeepLauncherOnBluetooth(it) } },
                onSpeedAlert = { scope.launch { prefs.setSpeedAlert(it) } },
                onSpeedLimit = { scope.launch { prefs.setSpeedLimit(it) } },
                onObdConnection = {
                    scope.launch { prefs.setObdConnection(it) }
                    if ((it == com.radioauto.launcher.data.ObdConnection.BLUETOOTH || it == com.radioauto.launcher.data.ObdConnection.AUTO) &&
                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                    ) btPermission.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
                },
                onObdAddress = { host, port -> scope.launch { prefs.setObdAddress(host, port) } },
                onNextOilKm = { scope.launch { prefs.setNextOilKm(it) } },
                onObdBtAddress = { scope.launch { prefs.setObdBtAddress(it) } },
                onObdInterval = { scope.launch { prefs.setObdInterval(it) } },
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
                Navigator.navigateTo(context, address, byPackage.keys, settings.navApp, screenRect())
            },
            onDismiss = { editingAddress = null },
        )
    }
}

/** True when the radio has a network that reaches the internet. */
private fun hasInternet(context: android.content.Context): Boolean {
    val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
