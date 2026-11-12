package com.radioauto.launcher.embedded

/** How an external app can share the screen with the launcher on this device, best first. */
enum class EmbedMode(val label: String) {
    /** A floating window with the bounds of the launcher's panel (the launcher stays visible around it). */
    FREEFORM("Ventana junto al launcher"),

    /** The launcher is already in split screen: the app opens in the other half. */
    SPLIT("Pantalla dividida"),

    /** The system offers no shared window: the app covers the launcher and Home brings it back. */
    FULLSCREEN("Pantalla completa"),
}

/** What the container shows about the external app. */
sealed interface EmbeddedAppState {
    data object Idle : EmbeddedAppState
    data class Loading(val packageName: String) : EmbeddedAppState
    data class NotInstalled(val packageName: String) : EmbeddedAppState
    data class Opened(val packageName: String, val mode: EmbedMode) : EmbeddedAppState
    data class Failed(val packageName: String, val message: String) : EmbeddedAppState
}
