package com.radioauto.launcher.embedded

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

/**
 * Finds out, with public APIs only, what this device allows. Android has no public way to draw another app's Activity
 * inside our own layout (ActivityView and TaskView are system-only), so the real options are a window beside the
 * launcher (freeform), split screen, or full screen.
 */
object AppCompatibilityChecker {

    private const val FEATURE_FREEFORM = "android.software.freeform_window_management"

    fun isInstalled(context: Context, packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null

    /** True when the system lets apps open in floating windows (a feature flag, or the developer setting). */
    fun freeformSupported(context: Context): Boolean {
        val featured = context.packageManager.hasSystemFeature(FEATURE_FREEFORM)
        val enabled = runCatching { Settings.Global.getInt(context.contentResolver, "enable_freeform_support", 0) == 1 }.getOrDefault(false)
        return featured || enabled
    }

    /** True while the launcher itself shares the screen (so a new app can take the other half). */
    fun inSplitScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && context.findActivity()?.isInMultiWindowMode == true

    fun bestMode(context: Context): EmbedMode = when {
        freeformSupported(context) -> EmbedMode.FREEFORM
        inSplitScreen(context) -> EmbedMode.SPLIT
        else -> EmbedMode.FULLSCREEN
    }

    /** Short report for the diagnostics and the documentation. */
    fun report(context: Context): String = buildString {
        append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        append("Ventanas flotantes: ${freeformSupported(context)}\n")
        append("Pantalla dividida ahora: ${inSplitScreen(context)}\n")
        append("Actividades en otras pantallas: ${context.packageManager.hasSystemFeature(PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS)}\n")
        append("Modo elegido: ${bestMode(context).label}")
    }
}

internal fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
