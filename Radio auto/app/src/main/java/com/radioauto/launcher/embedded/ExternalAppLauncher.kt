package com.radioauto.launcher.embedded

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import com.radioauto.launcher.MainActivity
import com.radioauto.launcher.data.MapWindow

/** Opens any installed app in the way the device allows, and brings the launcher back. Never touches the app itself. */
object ExternalAppLauncher {

    /** True when the system accepted the request; the app's own screen is then up to the app. */
    fun launch(context: Context, packageName: String, bounds: Rect?, mode: EmbedMode): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return when (mode) {
            // The panel's bounds make the window sit beside the launcher; reuses the window logic that already exists.
            EmbedMode.FREEFORM -> MapWindow.start(context, intent, bounds)
            // The launcher is in split screen: ask for the other half.
            EmbedMode.SPLIT -> runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT))
            }.isSuccess
            EmbedMode.FULLSCREEN -> runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
        }
    }

    /** Back to the launcher; the app keeps running behind it, so a navigation in progress is not cut. */
    fun backToLauncher(context: Context) {
        runCatching {
            context.startActivity(
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            )
        }
    }
}
