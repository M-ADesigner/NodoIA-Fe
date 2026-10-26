package com.radioauto.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Starts turn-by-turn navigation to an address in whichever map app is installed. */
object Navigator {
    private const val GOOGLE_MAPS = "com.google.android.apps.maps"
    private const val WAZE = "com.waze"

    fun navigateTo(context: Context, address: String, installed: Set<String>) {
        val q = Uri.encode(address)
        val intent = when {
            GOOGLE_MAPS in installed ->
                Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$q")).setPackage(GOOGLE_MAPS)
            WAZE in installed ->
                Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=$q&navigate=yes")).setPackage(WAZE)
            // Any other map app that understands geo: links.
            else -> Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q"))
        }
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
