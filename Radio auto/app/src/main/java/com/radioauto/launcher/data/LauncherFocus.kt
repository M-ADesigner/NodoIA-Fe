package com.radioauto.launcher.data

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * When the phone connects, the head unit may open a link app (Android Auto) over the launcher.
 * Bring the launcher back a few times while that app settles. It never opens any other app.
 */
object LauncherFocus {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val running = AtomicBoolean(false)

    /** Seconds after the connection at which the launcher is brought forward. */
    private val DELAYS_MS = longArrayOf(2_000, 4_000, 7_000)

    /** Ignored when a sequence is already running: the ACL and A2DP broadcasts arrive almost together. */
    fun schedule(context: Context, onDone: () -> Unit = {}) {
        val app = context.applicationContext
        if (!running.compareAndSet(false, true)) {
            onDone()
            return
        }
        scope.launch {
            try {
                if (!Prefs(app).current().keepLauncherOnBluetooth) return@launch
                var elapsed = 0L
                for (at in DELAYS_MS) {
                    delay(at - elapsed)
                    elapsed = at
                    bringToFront(app)
                }
            } finally {
                running.set(false)
                onDone()
            }
        }
    }

    private fun bringToFront(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        runCatching { context.startActivity(intent) }
    }
}
