package com.radioauto.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Connects to the media service of Android's Bluetooth stack (A2DP Sink / AVRCP Controller). The sink only plays
 * once its media session gets Play and takes the audio focus, so the bar drives that session directly.
 * Everything here runs on the main thread, where [MediaBrowserCompat] needs to live.
 */
object BtMediaConnector {

    private const val BROWSE_ACTION = "android.media.browse.MediaBrowserService"
    private const val BLUETOOTH_PACKAGE = "com.android.bluetooth"
    private const val RETRY_MS = 10_000L

    /** Used when the system does not list the service: Android 8.x first, then 9+. */
    private val FALLBACKS = listOf(
        ComponentName(BLUETOOTH_PACKAGE, "com.android.bluetooth.a2dpsink.mbs.A2dpMediaBrowserService"),
        ComponentName(BLUETOOTH_PACKAGE, "com.android.bluetooth.avrcpcontroller.BluetoothMediaBrowserService"),
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var appContext: Context? = null
    private var changed: (() -> Unit)? = null
    private var browser: MediaBrowserCompat? = null
    private var retry: Job? = null
    private var nextFallback = 0

    /** The Bluetooth session while the browser is connected; null otherwise. */
    var controller: MediaControllerCompat? = null
        private set

    /** The service that was tried last, for the diagnostic screen. */
    var service: ComponentName? = null
        private set

    var status: String = "sin iniciar"
        private set

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) = notifyChanged()
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) = notifyChanged()
        override fun onSessionDestroyed() {
            drop()
            connect()
        }
    }

    fun init(context: Context, onChange: () -> Unit) {
        if (appContext != null) return
        appContext = context.applicationContext
        changed = onChange
        connect()
    }

    /** Safe to call at any time: it does nothing while connected or while a connection is in progress. */
    fun connect() {
        val app = appContext ?: return
        if (browser != null) return
        retry?.cancel()
        val component = findService(app)
        service = component
        status = "conectando"
        val created = MediaBrowserCompat(app, component, object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                val b = browser ?: return
                val c = try {
                    MediaControllerCompat(app, b.sessionToken)
                } catch (e: Exception) {
                    return fail("sesión no disponible")
                }
                c.registerCallback(controllerCallback)
                controller = c
                status = "conectado"
                notifyChanged()
            }

            override fun onConnectionSuspended() = fail("suspendido")
            override fun onConnectionFailed() = fail("falló")
        }, null)
        browser = created
        try {
            created.connect()
        } catch (e: Exception) {
            fail("error: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /** The system's own answer first; the known class names only when it lists nothing. */
    private fun findService(context: Context): ComponentName {
        val found = context.packageManager
            .queryIntentServices(Intent(BROWSE_ACTION).setPackage(BLUETOOTH_PACKAGE), 0)
            .firstOrNull()?.serviceInfo
        if (found != null) return ComponentName(found.packageName, found.name)
        // Each failed attempt moves on to the next known class.
        val component = FALLBACKS[nextFallback % FALLBACKS.size]
        nextFallback++
        return component
    }

    private fun fail(reason: String) {
        status = reason
        drop()
        retry?.cancel()
        // Reconnects on its own; ACL_CONNECTED and onResume also call connect() sooner.
        retry = scope.launch {
            delay(RETRY_MS)
            connect()
        }
    }

    private fun drop() {
        controller?.unregisterCallback(controllerCallback)
        controller = null
        runCatching { browser?.disconnect() }
        browser = null
        notifyChanged()
    }

    private fun notifyChanged() {
        changed?.invoke()
    }

    // Transport controls of the Bluetooth session. They return false when there is no session to ask.

    fun play(): Boolean = controller?.transportControls?.play() != null
    fun pause(): Boolean = controller?.transportControls?.pause() != null
    fun skipToPrevious(): Boolean = controller?.transportControls?.skipToPrevious() != null
    fun skipToNext(): Boolean = controller?.transportControls?.skipToNext() != null

    val isPlaying: Boolean get() = controller?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING
}
