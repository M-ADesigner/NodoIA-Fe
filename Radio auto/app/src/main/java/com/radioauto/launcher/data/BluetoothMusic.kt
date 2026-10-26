package com.radioauto.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast

/** Opens the head unit's Bluetooth music app and drives the phone's player. */
object BluetoothMusic {

    /** Bluetooth apps seen on Chinese head units, tried in order when none was chosen in the settings. */
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

    /**
     * The chosen app first, then the known ones, and finally the system Bluetooth settings so the phone can be paired.
     * Every failure is reported in a toast with the package that was tried, so a tap is never left without an answer.
     */
    fun open(context: Context, preferred: String?) {
        val pm = context.packageManager
        val errors = ArrayList<String>()
        for (pkg in listOfNotNull(preferred) + CANDIDATES) {
            val launch = pm.getLaunchIntentForPackage(pkg) ?: continue
            try {
                context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: Exception) {
                errors += "$pkg: ${e.message ?: e.javaClass.simpleName}"
            }
        }

        val settings = listOf(
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.bluetooth.BluetoothSettings")),
        )
        for (intent in settings) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                if (errors.isNotEmpty()) toast(context, "No abrió la app Bluetooth. ${errors.first()}")
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
        if (!MediaState.skipPrevious()) sendKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    /** With no phone connected there is nothing to play, so the button opens the Bluetooth screen instead. */
    fun playPause(context: Context) {
        announce("playpause")
        val now = MediaState.state.value
        if (!now.btConnected && !now.hasMusic) {
            open(context, MediaState.preferredPackage)
            return
        }
        if (!MediaState.togglePlay()) sendKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun next(context: Context) {
        announce("next")
        if (!MediaState.skipNext()) sendKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    private fun sendKey(context: Context, keyCode: Int) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun toast(context: Context, message: String) =
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
}
