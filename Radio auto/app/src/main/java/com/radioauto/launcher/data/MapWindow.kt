package com.radioauto.launcher.data

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect

/**
 * Opens a navigation app in a window beside the launcher instead of covering it. The window gets the given bounds
 * (the right-hand panel), which Android honours when freeform windows are enabled; where they are not, the app simply
 * opens full screen as it always did.
 */
object MapWindow {

    /** Apps the driver can pick as the navigator, in the order they are offered. */
    val NAV_APPS = listOf("com.waze" to "Waze", "com.google.android.apps.maps" to "Google Maps")

    private const val WINDOWING_MODE_FREEFORM = 5

    /** False when the system refused to start the app. */
    fun start(context: Context, intent: Intent, bounds: Rect?): Boolean {
        val options = bounds?.takeIf { !it.isEmpty }?.let { b ->
            val o = ActivityOptions.makeBasic().setLaunchBounds(b)
            // Bounds alone keep the window full screen; asking for the freeform mode (5) makes them count. The call is
            // not public, so if the system refuses it the bounds are still set and the app opens as it did before.
            runCatching {
                ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType).invoke(o, WINDOWING_MODE_FREEFORM)
            }
            o.toBundle()
        }
        return runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), options) }.isSuccess
    }

    /** Opens the app's page in the store, for a navigator that is not installed yet. */
    fun install(context: Context, packageName: String) {
        val store = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$packageName"))
        val web = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        runCatching { context.startActivity(store.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { runCatching { context.startActivity(web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }

    /** Launches the app's own start screen. */
    fun open(context: Context, packageName: String, bounds: Rect?) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        start(context, intent, bounds)
    }
}
