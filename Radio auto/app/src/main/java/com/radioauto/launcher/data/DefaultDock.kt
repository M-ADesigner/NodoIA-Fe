package com.radioauto.launcher.data

/** What a driver reaches for first: map, radio, music, then phone and the rest. */
val MAP_PACKAGES = listOf("com.google.android.apps.maps", "com.waze", "com.here.app.maps")
private val maps = MAP_PACKAGES

// Radio apps differ per head unit vendor; these are the usual ones (SYU, Microntek, MediaTek, Qualcomm, stock).
private val radio = listOf(
    "com.syu.radio", "com.microntek.radio", "com.hct.radio", "com.ts.radio",
    "com.android.fmradio", "com.mediatek.fmradio", "com.caf.fmradio", "com.qualcomm.fmradio",
)
private val radioLabel = Regex("(?i)\\b(radio|fm)\\b")

private val music = listOf(
    "com.spotify.music", "com.google.android.apps.youtube.music", "com.syu.music",
    "com.microntek.music", "com.google.android.music", "com.android.music",
)
private val phone = listOf("com.google.android.dialer", "com.android.dialer", "com.samsung.android.dialer")
private val video = listOf("com.google.android.youtube")
private val web = listOf("com.android.chrome")

/**
 * Picks the first installed app of each group so the dock is useful before any customising.
 * The first entry is the map: the home screen shows it as the big tile.
 */
fun defaultDock(apps: List<AppInfo>): List<String> {
    val installed = apps.map { it.packageName }.toSet()
    val radioApp = radio.firstOrNull { it in installed }
        ?: apps.firstOrNull { radioLabel.containsMatchIn(it.label) }?.packageName
    return listOfNotNull(
        maps.firstOrNull { it in installed },
        radioApp,
        music.firstOrNull { it in installed },
        phone.firstOrNull { it in installed },
        video.firstOrNull { it in installed },
        web.firstOrNull { it in installed },
    ).distinct()
}
