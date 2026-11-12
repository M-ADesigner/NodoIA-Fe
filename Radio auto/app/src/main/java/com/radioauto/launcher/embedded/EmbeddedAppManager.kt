package com.radioauto.launcher.embedded

import android.content.Context
import android.graphics.Rect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one place that knows which external app is open and how. Not tied to Waze: any package works, so Google Maps or
 * any other app goes through the same path. Used from the main thread only.
 */
object EmbeddedAppManager {
    private val _state = MutableStateFlow<EmbeddedAppState>(EmbeddedAppState.Idle)
    val state: StateFlow<EmbeddedAppState> = _state.asStateFlow()

    private var lastPackage: String? = null
    private var lastBounds: Rect? = null

    /** Opens [packageName] in the best mode the device offers, inside [bounds] when windows are allowed. */
    fun open(context: Context, packageName: String, bounds: Rect?) {
        lastPackage = packageName
        lastBounds = bounds
        launch(context, packageName, bounds, AppCompatibilityChecker.bestMode(context))
    }

    /** The same app over the whole screen. */
    fun expand(context: Context) {
        val pkg = lastPackage ?: return
        launch(context, pkg, null, EmbedMode.FULLSCREEN)
    }

    /** The same app in the panel again, with the last known bounds. */
    fun reopen(context: Context) {
        val pkg = lastPackage ?: return
        launch(context, pkg, lastBounds, AppCompatibilityChecker.bestMode(context))
    }

    fun backToLauncher(context: Context) = ExternalAppLauncher.backToLauncher(context)

    /** Forgets the app (it is not closed): the container goes back to its start screen. */
    fun dismiss() {
        _state.value = EmbeddedAppState.Idle
    }

    private fun launch(context: Context, packageName: String, bounds: Rect?, mode: EmbedMode) {
        if (!AppCompatibilityChecker.isInstalled(context, packageName)) {
            _state.value = EmbeddedAppState.NotInstalled(packageName)
            return
        }
        _state.value = EmbeddedAppState.Loading(packageName)
        _state.value = if (ExternalAppLauncher.launch(context, packageName, bounds, mode)) {
            EmbeddedAppState.Opened(packageName, mode)
        } else {
            EmbeddedAppState.Failed(packageName, "Android no dejó abrir la aplicación")
        }
    }
}
