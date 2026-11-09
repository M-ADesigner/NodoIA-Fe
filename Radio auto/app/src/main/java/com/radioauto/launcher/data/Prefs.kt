package com.radioauto.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("launcher_prefs")

enum class DayNightMode(val label: String) { AUTO("Auto"), DAY("Día"), NIGHT("Noche") }

enum class Palette(val label: String) { BLUE("Azul"), VIOLET("Violeta"), GREEN("Verde"), ORANGE("Naranja"), RED("Rojo") }

data class LauncherSettings(
    /** null until the driver customises the dock; then the default apps are shown. */
    val dock: List<String>? = null,
    val mode: DayNightMode = DayNightMode.NIGHT,
    val palette: Palette = Palette.VIOLET,
    val homeAddress: String = "",
    val workAddress: String = "",
    /** Bluetooth music app chosen by hand; null means try the known head-unit apps. */
    val btMusicApp: String? = null,
    /** Bring the launcher back to the front when the phone connects, so a link app cannot cover it. */
    val keepLauncherOnBluetooth: Boolean = true,
    val speedAlert: Boolean = true,
    val speedLimit: Int = 120,
    val obdConnection: ObdConnection = ObdConnection.AUTO,
    /** Odometer reading of the next oil change; the maintenance card counts down to it. */
    val nextOilKm: Int = DEFAULT_NEXT_OIL_KM,
    /** WiFi adapters: the clones answer on this address once the radio joins their network. */
    val obdHost: String = "192.168.0.10",
    /** MAC of the Bluetooth adapter picked by hand; empty means "find the one that looks like an OBD dongle". */
    val obdBtAddress: String = "",
    /** Pause between two reading rounds; a slow clone needs more air. */
    val obdIntervalMs: Int = 500,
    val obdPort: Int = 35000,
)

class Prefs(private val context: Context) {

    private val dockKey = stringPreferencesKey("dock")
    private val modeKey = stringPreferencesKey("mode")
    private val paletteKey = stringPreferencesKey("palette")
    private val homeKey = stringPreferencesKey("home_address")
    private val workKey = stringPreferencesKey("work_address")
    private val btMusicAppKey = stringPreferencesKey("bt_music_app")
    private val keepLauncherKey = booleanPreferencesKey("keep_launcher_on_bluetooth")
    private val speedAlertKey = booleanPreferencesKey("speed_alert")
    private val speedLimitKey = intPreferencesKey("speed_limit")
    private val obdConnKey = stringPreferencesKey("obd_connection")
    private val nextOilKey = intPreferencesKey("next_oil_km")
    private val obdBtKey = stringPreferencesKey("obd_bt_address")
    private val obdIntervalKey = intPreferencesKey("obd_interval_ms")
    private val obdHostKey = stringPreferencesKey("obd_host")
    private val obdPortKey = intPreferencesKey("obd_port")
    private val latKey = doublePreferencesKey("last_lat")
    private val lonKey = doublePreferencesKey("last_lon")
    private val totalKey = doublePreferencesKey("total_meters")
    private val tripKey = doublePreferencesKey("trip_meters")

    val settings: Flow<LauncherSettings> = context.store.data.map { it.toSettings() }

    suspend fun current(): LauncherSettings = settings.first()

    suspend fun setDock(packages: List<String>) {
        context.store.edit { it[dockKey] = packages.joinToString(",") }
    }

    suspend fun setMode(mode: DayNightMode) {
        context.store.edit { it[modeKey] = mode.name }
    }

    suspend fun setPalette(palette: Palette) {
        context.store.edit { it[paletteKey] = palette.name }
    }

    suspend fun setAddress(home: Boolean, address: String) {
        context.store.edit { it[if (home) homeKey else workKey] = address.trim() }
    }

    suspend fun setBtMusicApp(packageName: String?) {
        context.store.edit {
            if (packageName == null) it.remove(btMusicAppKey) else it[btMusicAppKey] = packageName
        }
    }

    suspend fun setKeepLauncherOnBluetooth(enabled: Boolean) {
        context.store.edit { it[keepLauncherKey] = enabled }
    }

    suspend fun setSpeedAlert(enabled: Boolean) {
        context.store.edit { it[speedAlertKey] = enabled }
    }

    suspend fun setSpeedLimit(kmh: Int) {
        context.store.edit { it[speedLimitKey] = kmh.coerceIn(30, 200) }
    }

    suspend fun setObdConnection(c: ObdConnection) {
        context.store.edit { it[obdConnKey] = c.name }
    }

    suspend fun setNextOilKm(km: Int) {
        context.store.edit { it[nextOilKey] = km.coerceIn(0, 999_999) }
    }

    suspend fun setObdBtAddress(address: String) {
        context.store.edit { it[obdBtKey] = address }
    }

    suspend fun setObdInterval(ms: Int) {
        context.store.edit { it[obdIntervalKey] = ms.coerceIn(100, 2000) }
    }

    suspend fun setObdAddress(host: String, port: Int) {
        context.store.edit {
            it[obdHostKey] = host.trim()
            it[obdPortKey] = port.coerceIn(1, 65535)
        }
    }

    /** Remembered so the map and the weather have somewhere to start before the first GPS fix. */
    suspend fun saveLastPosition(lat: Double, lon: Double) {
        context.store.edit {
            it[latKey] = lat
            it[lonKey] = lon
        }
    }

    suspend fun loadLastPosition(): Pair<Double, Double>? {
        val p = context.store.data.first()
        val lat = p[latKey] ?: return null
        val lon = p[lonKey] ?: return null
        return lat to lon
    }

    suspend fun loadTracking(): Pair<Double, Double> {
        val p = context.store.data.first()
        return (p[totalKey] ?: 0.0) to (p[tripKey] ?: 0.0)
    }

    suspend fun saveTracking(totalMeters: Double, tripMeters: Double) {
        context.store.edit {
            it[totalKey] = totalMeters
            it[tripKey] = tripMeters
        }
    }

    private fun Preferences.toSettings() = LauncherSettings(
        dock = this[dockKey]?.split(",")?.filter { it.isNotBlank() },
        mode = this[modeKey]?.let { runCatching { DayNightMode.valueOf(it) }.getOrNull() } ?: DayNightMode.NIGHT,
        palette = this[paletteKey]?.let { runCatching { Palette.valueOf(it) }.getOrNull() } ?: Palette.VIOLET,
        homeAddress = this[homeKey].orEmpty(),
        workAddress = this[workKey].orEmpty(),
        btMusicApp = this[btMusicAppKey],
        keepLauncherOnBluetooth = this[keepLauncherKey] ?: true,
        speedAlert = this[speedAlertKey] ?: true,
        speedLimit = this[speedLimitKey] ?: 120,
        obdConnection = this[obdConnKey]?.let { runCatching { ObdConnection.valueOf(it) }.getOrNull() } ?: ObdConnection.AUTO,
        nextOilKm = this[nextOilKey] ?: DEFAULT_NEXT_OIL_KM,
        obdHost = this[obdHostKey] ?: "192.168.0.10",
        obdBtAddress = this[obdBtKey].orEmpty(),
        obdIntervalMs = this[obdIntervalKey] ?: 500,
        obdPort = this[obdPortKey] ?: 35000,
    )
}
