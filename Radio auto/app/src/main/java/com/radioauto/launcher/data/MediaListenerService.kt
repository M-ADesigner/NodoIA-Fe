package com.radioauto.launcher.data

import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Everything the bar, the clock and the background need to know about what plays on the phone. */
data class NowPlaying(
    val btConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val albumArt: Bitmap? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val sourcePackage: String? = null,
) {
    /** True when there is something to show: a track name or audio that is playing. */
    val hasMusic: Boolean get() = title.isNotEmpty() || isPlaying
}

/** Position of the track now, from the last report of the player, which keeps moving while it plays. */
internal fun extrapolatePosition(
    position: Long,
    updatedAt: Long,
    now: Long,
    speed: Float,
    playing: Boolean,
    durationMs: Long,
): Long {
    if (position < 0) return 0
    val moved = if (playing && updatedAt > 0) ((now - updatedAt) * speed).toLong().coerceAtLeast(0) else 0
    val p = position + moved
    return if (durationMs > 0) p.coerceIn(0, durationMs) else p
}

/**
 * Only exists so the system grants access to active media sessions; the launcher
 * reads them through [MediaState] while this listener is enabled in Settings.
 */
class MediaListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        MediaState.attach(this)
    }

    override fun onListenerDisconnected() {
        MediaState.detach()
    }
}

object MediaState {
    private val _state = MutableStateFlow(NowPlaying())
    val state: StateFlow<NowPlaying> = _state.asStateFlow()

    private val _access = MutableStateFlow(false)

    /** Whether the notification listener is enabled; the bar asks for it when it is not. */
    val accessGranted: StateFlow<Boolean> = _access.asStateFlow()

    // Everything below is touched only on the main thread: callbacks, receivers and the ticker all run there.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var appContext: Context? = null
    private var component: ComponentName? = null
    private var manager: MediaSessionManager? = null
    private var controller: MediaController? = null
    private var ticker: Job? = null

    /** Package of the Bluetooth music app chosen in Settings; its session wins over any other. */
    var preferredPackage: String? = null
        private set

    private var profileLink = false
    private val connectedPhones = HashSet<String>()

    // Used only when no app publishes a session: track names heard from music broadcasts.
    private var fallbackTitle = ""
    private var fallbackArtist = ""
    private var fallbackPlaying: Boolean? = null

    private const val A2DP_SINK_ACTION = "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED"
    private const val HEADSET_CLIENT_ACTION = "android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED"
    private const val AVRCP_TRACK_EVENT = "android.bluetooth.avrcp-controller.profile.action.TRACK_EVENT"
    private const val MUSIC_META = "com.android.music.metachanged"
    private const val MUSIC_STATE = "com.android.music.playstatechanged"

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> onAcl(context, intent, connected = true)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> onAcl(context, intent, connected = false)
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED, A2DP_SINK_ACTION -> onProfile(context, intent, bringFront = true)
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED, HEADSET_CLIENT_ACTION -> onProfile(context, intent, bringFront = false)
                MUSIC_META, MUSIC_STATE, AVRCP_TRACK_EVENT -> onTrackBroadcast(intent)
            }
        }
    }

    /** Safe to call many times; the launcher and the tracking service both call it so this lives with the process. */
    fun init(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app
        profileLink = profileConnected(app)

        val filter = IntentFilter().apply {
            listOf(
                BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED,
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED, A2DP_SINK_ACTION,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED, HEADSET_CLIENT_ACTION,
                MUSIC_META, MUSIC_STATE, AVRCP_TRACK_EVENT,
            ).forEach(::addAction)
        }
        // Exported: the music broadcasts come from other apps.
        ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        refreshAccess()
        publish()
    }

    fun attach(context: Context) {
        init(context)
        val comp = ComponentName(context, MediaListenerService::class.java)
        val mgr = context.getSystemService(MediaSessionManager::class.java) ?: return
        component = comp
        manager = mgr
        _access.value = true
        runCatching {
            mgr.addOnActiveSessionsChangedListener(sessionsListener, comp)
            pick(mgr.getActiveSessions(comp))
        }
    }

    fun detach() {
        manager?.removeOnActiveSessionsChangedListener(sessionsListener)
        controller?.unregisterCallback(callback)
        controller = null
        manager = null
        refreshAccess()
        publish()
    }

    /** The launcher is on screen: refresh once and then every second (position, and audio detection without a session). */
    fun setVisible(visible: Boolean) {
        ticker?.cancel()
        ticker = null
        if (!visible) return
        refreshAccess()
        publish()
        ticker = scope.launch {
            while (true) {
                delay(1_000)
                publish()
            }
        }
    }

    fun setPreferred(packageName: String?) {
        if (preferredPackage == packageName) return
        preferredPackage = packageName
        val mgr = manager
        val comp = component
        if (mgr != null && comp != null) runCatching { pick(mgr.getActiveSessions(comp)) }
    }

    fun refreshAccess() {
        val ctx = appContext ?: return
        _access.value = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
            ?.contains(ctx.packageName) == true
    }

    /** True when a session answered, so the caller does not need to fall back to the media keys. */
    fun skipPrevious(): Boolean = controller?.transportControls?.skipToPrevious() != null

    fun skipNext(): Boolean = controller?.transportControls?.skipToNext() != null

    fun togglePlay(): Boolean {
        val c = controller ?: return false
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
        return true
    }

    /** Apps that count as the radio's Bluetooth music player. */
    private fun isBtSource(packageName: String?): Boolean =
        packageName != null && (packageName == preferredPackage || packageName in BluetoothMusic.CANDIDATES)

    private fun pick(list: List<MediaController>?) {
        fun playing(c: MediaController) = c.playbackState?.state == PlaybackState.STATE_PLAYING
        // The Bluetooth player first (playing, then any), then whatever plays, then the most recent session.
        val next = list?.firstOrNull { isBtSource(it.packageName) && playing(it) }
            ?: list?.firstOrNull { isBtSource(it.packageName) }
            ?: list?.firstOrNull { playing(it) }
            ?: list?.firstOrNull()
        controller?.unregisterCallback(callback)
        controller = next
        next?.registerCallback(callback)
        publish()
    }

    private fun publish() {
        val ctx = appContext
        val c = controller
        val link = profileLink || connectedPhones.isNotEmpty()
        val now = SystemClock.elapsedRealtime()
        _state.value = if (c != null) {
            val meta = c.metadata
            val ps = c.playbackState
            val playing = ps?.state == PlaybackState.STATE_PLAYING
            val duration = (meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(0)
            NowPlaying(
                btConnected = link || isBtSource(c.packageName),
                isPlaying = playing,
                title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
                artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
                albumArt = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
                positionMs = extrapolatePosition(
                    ps?.position ?: 0, ps?.lastPositionUpdateTime ?: now, now, ps?.playbackSpeed ?: 1f, playing, duration,
                ),
                durationMs = duration,
                sourcePackage = c.packageName,
            )
        } else {
            // No session: trust the music broadcasts, and otherwise ask the system whether audio is playing.
            val audioOn = ctx?.let { (it.getSystemService(Context.AUDIO_SERVICE) as AudioManager).isMusicActive } == true
            NowPlaying(
                btConnected = link,
                isPlaying = fallbackPlaying ?: (link && audioOn),
                title = fallbackTitle,
                artist = fallbackArtist,
            )
        }
    }

    private fun onAcl(context: Context, intent: Intent, connected: Boolean) {
        // Reading the device class needs BLUETOOTH_CONNECT from Android 12; without it only the profile events are used.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) return
        val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
        if (device.bluetoothClass?.majorDeviceClass != BluetoothClass.Device.Major.PHONE) return
        if (connected) connectedPhones.add(device.address) else connectedPhones.remove(device.address)
        if (!connected && connectedPhones.isEmpty()) {
            profileLink = false
            fallbackTitle = ""
            fallbackArtist = ""
            fallbackPlaying = null
        }
        publish()
    }

    private fun onProfile(context: Context, intent: Intent, bringFront: Boolean) {
        when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
            BluetoothProfile.STATE_CONNECTED -> {
                profileLink = true
                if (bringFront) LauncherFocus.schedule(context)
            }
            BluetoothProfile.STATE_DISCONNECTED -> profileLink = false
        }
        publish()
    }

    private fun onTrackBroadcast(intent: Intent) {
        if (controller != null) return
        intent.getStringExtra("track")?.let { fallbackTitle = it }
        intent.getStringExtra("artist")?.let { fallbackArtist = it }
        if (intent.hasExtra("playing")) fallbackPlaying = intent.getBooleanExtra("playing", false)
        publish()
    }

    private fun hasBluetoothPermission(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    /**
     * Whether a phone is already connected when the launcher starts, from the public profiles only. The sink and
     * headset-client profiles of a head unit are hidden, so those are followed through their broadcasts instead.
     */
    private fun profileConnected(context: Context): Boolean {
        if (!hasBluetoothPermission(context)) return false
        val adapter = ContextCompat.getSystemService(context, BluetoothManager::class.java)?.adapter ?: return false
        return try {
            adapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothAdapter.STATE_CONNECTED ||
                adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothAdapter.STATE_CONNECTED
        } catch (e: SecurityException) {
            false
        }
    }
}
