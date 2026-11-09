package com.radioauto.launcher.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the map app says in its ongoing notification: the next manoeuvre and the trip summary. */
data class NavInfo(
    val packageName: String,
    val title: String,
    val text: String,
    val subText: String,
)

/**
 * Waze and Google Maps cannot be embedded, but while navigating they publish an ongoing notification with the next
 * turn, the distance and the arrival time. The notification listener already granted for music feeds this.
 */
object NavState {
    private val _state = MutableStateFlow<NavInfo?>(null)
    val state: StateFlow<NavInfo?> = _state.asStateFlow()

    private val MAP_APPS = setOf("com.waze", "com.google.android.apps.maps")

    // `adb shell cmd notification post` comes from this package; accepted only in debug builds, to test without driving.
    private const val SHELL = "com.android.shell"

    fun update(context: Context, active: Array<StatusBarNotification>?) {
        val debug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val sbn = active?.filter { it.packageName in MAP_APPS || (debug && it.packageName == SHELL) }
            ?.firstOrNull { it.isOngoing || it.packageName == SHELL }
        _state.value = sbn?.let {
            val e = it.notification.extras
            val title = e.getCharSequence("android.title")?.toString().orEmpty()
            val text = (e.getCharSequence("android.bigText") ?: e.getCharSequence("android.text"))?.toString().orEmpty()
            val sub = e.getCharSequence("android.subText")?.toString().orEmpty()
            if (title.isEmpty() && text.isEmpty()) null else NavInfo(it.packageName, title, text, sub)
        }
    }
}
