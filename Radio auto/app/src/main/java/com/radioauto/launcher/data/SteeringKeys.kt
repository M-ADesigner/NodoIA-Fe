package com.radioauto.launcher.data

import android.content.Context
import android.view.KeyEvent

/**
 * The steering wheel buttons of the car arrive as media keys. One handler for all of them, so the launcher answers the
 * same way from the home screen, from a dialog or from anywhere else that asks. Volume keys are not handled here on
 * purpose: Android raises and lowers the music volume itself, with its own on-screen bar.
 */
object SteeringKeys {

    /** True for the keys this handler takes (and which the caller must then swallow). */
    fun isMedia(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
        KeyEvent.KEYCODE_HEADSETHOOK -> true
        else -> false
    }

    /** Runs the command on the key press (not on repeats or on release). Returns whether the key was ours. */
    fun handle(context: Context, keyCode: Int, isDown: Boolean, repeatCount: Int): Boolean {
        if (!isMedia(keyCode)) return false
        if (isDown && repeatCount == 0) {
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> BluetoothMusic.next(context)
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> BluetoothMusic.previous(context)
                else -> BluetoothMusic.playPause(context)
            }
        }
        return true
    }
}
