package com.radioauto.launcher.data

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri

/** Starts turn-by-turn navigation to an address in whichever map app is installed. */
object Navigator {
    private const val GOOGLE_MAPS = "com.google.android.apps.maps"
    private const val WAZE = "com.waze"

    /**
     * [preferred] is the navigator the driver picked (tried first); [bounds] is where to open its window, beside the
     * launcher, or null for full screen.
     */
    fun navigateTo(context: Context, address: String, installed: Set<String>, preferred: String? = null, bounds: Rect? = null) {
        val q = Uri.encode(address)
        fun google() = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$q")).setPackage(GOOGLE_MAPS)
        fun waze() = Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=$q&navigate=yes")).setPackage(WAZE)
        val intent = when {
            preferred == WAZE && WAZE in installed -> waze()
            preferred == GOOGLE_MAPS && GOOGLE_MAPS in installed -> google()
            GOOGLE_MAPS in installed -> google()
            WAZE in installed -> waze()
            // Any other map app that understands geo: links.
            else -> Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q"))
        }
        MapWindow.start(context, intent, bounds)
    }
}
