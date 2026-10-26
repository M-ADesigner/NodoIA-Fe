package com.radioauto.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.radioauto.launcher.data.AppRepository
import com.radioauto.launcher.data.BluetoothMusic
import com.radioauto.launcher.data.LauncherSettings
import com.radioauto.launcher.data.MediaState
import androidx.lifecycle.lifecycleScope
import com.radioauto.launcher.data.Prefs
import com.radioauto.launcher.data.TrackingService
import com.radioauto.launcher.data.TrackingState
import kotlinx.coroutines.launch
import com.radioauto.launcher.ui.HomeScreen
import com.radioauto.launcher.ui.theme.LauncherTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        MediaState.init(applicationContext)

        val repository = AppRepository(applicationContext)
        val prefs = Prefs(applicationContext)

        // Totals must be loaded before the service starts adding to them.
        lifecycleScope.launch {
            TrackingState.load(prefs)
            TrackingService.ensureRunning(applicationContext)
        }

        setContent {
            val apps by remember { repository.observe() }.collectAsState(initial = emptyList())
            val settings by remember { prefs.settings }.collectAsState(initial = LauncherSettings())
            LauncherTheme(mode = settings.mode, palette = settings.palette) {
                HomeScreen(apps, settings, repository, prefs)
            }
        }
    }

    // The one-second music refresh only runs while the launcher is on screen.
    override fun onResume() {
        super.onResume()
        MediaState.setVisible(true)
    }

    override fun onPause() {
        MediaState.setVisible(false)
        super.onPause()
    }

    // Steering wheel buttons arrive as media keys; volume is left to the system.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val action: ((Context) -> Unit)? = when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> BluetoothMusic::next
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> BluetoothMusic::previous
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> BluetoothMusic::playPause
            else -> null
        }
        if (action == null) return super.onKeyDown(keyCode, event)
        if (event.repeatCount == 0) action(this)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> true
        else -> super.onKeyUp(keyCode, event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    // Pressing Home while already on the launcher must not leave it in a stale state.
    @SuppressLint("MissingSuperCall")
    @Deprecated("Back does nothing on a launcher")
    override fun onBackPressed() = Unit

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
