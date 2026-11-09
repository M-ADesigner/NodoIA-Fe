package com.radioauto.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.radioauto.launcher.link.LinkServer
import com.radioauto.launcher.link.MediaAction

/** Drives the phone's player through the Bluetooth media session and opens the Bluetooth settings. */
object BluetoothMusic {

    /** Bluetooth player apps whose media session counts as the phone's player. */
    val CANDIDATES = listOf(
        "com.ts.bt",
        "com.ts.btmusic",
        "com.ts.bluetooth",
        "com.syu.bt",
        "com.syu.btmusic",
        "com.microntek.bluetooth",
        "com.fyt.bt",
        "com.sprd.bt",
        "com.hct.bt",
        "com.nwd.bt.music",
        "com.android.bluetooth.music",
        "com.android.bluetooth",
    )

    /** The system Bluetooth screen, where the phone is paired and the audio profile can be enabled. */
    fun openSettings(context: Context) {
        val errors = ArrayList<String>()
        val settings = listOf(
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.bluetooth.BluetoothSettings")),
        )
        for (intent in settings) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: Exception) {
                errors += "${intent.action ?: intent.component?.flattenToShortString()}: ${e.message ?: e.javaClass.simpleName}"
            }
        }
        toast(context, "No se pudo abrir Bluetooth. ${errors.joinToString("; ")}")
    }

    /** Read by tools/bridge.py to drive the real phone when the launcher runs on the emulator. */
    private fun announce(command: String) {
        Log.i("RadioCmd", command)
    }

    fun previous(context: Context) {
        announce("prev")
        if (!LinkServer.mediaCommand(MediaAction.Previous) && !MediaState.skipPrevious()) sendKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    /** With no phone connected there is nothing to play, so the button opens the Bluetooth settings instead. */
    fun playPause(context: Context) {
        announce("playpause")
        // A phone linked through the QR / AutoPlay holds the music: its player gets the command.
        if (LinkServer.mediaCommand(MediaAction.PlayPause)) return
        val now = MediaState.state.value
        if (!now.btConnected && !now.hasMusic) {
            openSettings(context)
            return
        }
        // Play goes to the Bluetooth session: that is what makes the sink take the audio focus. Never sent on its own.
        if (!MediaState.togglePlay()) sendKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun next(context: Context) {
        announce("next")
        if (!LinkServer.mediaCommand(MediaAction.Next) && !MediaState.skipNext()) sendKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    private fun sendKey(context: Context, keyCode: Int) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun toast(context: Context, message: String) =
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
}
