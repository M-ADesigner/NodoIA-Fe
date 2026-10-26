package com.radioauto.launcher.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationListener
import android.location.LocationManager
import android.location.GnssStatus
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.radioauto.launcher.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Keeps reading GPS while the driver is in another app (Maps, music), so speed and the
 * kilometre counter keep working when the launcher is not on screen.
 */
class TrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tracker = TripTracker()
    private val prefs by lazy { Prefs(applicationContext) }
    private val alerts = DriveAlerts()
    private val speaker by lazy { Speaker(applicationContext) }

    // Latest settings, kept in memory so each GPS fix does not read from disk.
    @Volatile private var config = LauncherSettings()
    private var pendingMeters = 0.0
    private var started = false

    private val listener = LocationListener { loc ->
        val added = tracker.onFix(
            Fix(loc.latitude, loc.longitude, loc.accuracy, if (loc.hasSpeed()) loc.speed else null, loc.time)
        )
        val kmh = (tracker.speedMps * 3.6f).roundToInt()
        // Bearing is only meaningful while moving; at a standstill it jumps around.
        val bearing = if (loc.hasBearing() && kmh >= 5) loc.bearing else null
        TrackingState.onFix(kmh, added, loc.latitude, loc.longitude, bearing, null)
        alerts.onSpeed(kmh, SystemClock.elapsedRealtime(), config.speedAlert, config.speedLimit)
            ?.let { speaker.say(it.phrase) }
        pendingMeters += added
        // Save every ~500 m, or as soon as the car stops, instead of on every fix.
        if (pendingMeters >= 500 || (kmh == 0 && pendingMeters > 0)) save()
    }

    /** Satellites used for the current position, shown next to the compass. */
    private val gnss = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var used = 0
            for (i in 0 until status.satelliteCount) if (status.usedInFix(i)) used++
            TrackingState.setSatellites(used)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Keeps the Bluetooth and music listeners alive while the launcher is behind another app.
        MediaState.init(applicationContext)
        scope.launch { prefs.settings.collect { config = it } }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        if (!started && hasLocationPermission(this)) {
            started = true
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            runCatching { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener) }
            runCatching { lm.registerGnssStatusCallback(gnss, Handler(Looper.getMainLooper())) }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        (getSystemService(Context.LOCATION_SERVICE) as LocationManager).apply {
            removeUpdates(listener)
            unregisterGnssStatusCallback(gnss)
        }
        save()
        speaker.shutdown()
        scope.cancel()
        super.onDestroy()
    }

    private fun save() {
        pendingMeters = 0.0
        val s = TrackingState.state.value
        scope.launch {
            prefs.saveTracking(s.totalMeters, s.tripMeters)
            if (s.lat != null && s.lon != null) prefs.saveLastPosition(s.lat, s.lon)
        }
    }

    private fun startInForeground() {
        // Channels only exist from Android 8; older versions use the builder's priority instead.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Velocidad y kilometraje", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Midiendo velocidad y kilometraje")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        // The constant is inlined at compile time, so read it only where it exists (API 29+); older ignores the type.
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        private const val CHANNEL = "tracking"
        private const val NOTIFICATION_ID = 1

        fun hasLocationPermission(context: Context) =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        /** Starts the service when GPS is allowed; call while the launcher is visible (Android 14 rule). */
        fun ensureRunning(context: Context) {
            if (hasLocationPermission(context)) {
                ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
            }
        }
    }
}
