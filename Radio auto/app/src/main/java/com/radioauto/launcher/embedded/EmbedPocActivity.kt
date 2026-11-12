package com.radioauto.launcher.embedded

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Bundle
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Proof of concept, public APIs only: can this device show another app inside a surface of ours? It creates a
 * VirtualDisplay on a SurfaceView and asks the system to start the app on that display. Run it with
 * `am start -n com.radioauto.launcher/.embedded.EmbedPocActivity --es pkg com.waze` and read the on-screen report.
 * If the app appears in the lower box, real embedding works on this hardware; if it opens over the whole screen or
 * the report shows a SecurityException, it does not.
 */
class EmbedPocActivity : Activity() {
    private var display: VirtualDisplay? = null
    private lateinit var report: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra("pkg") ?: "com.waze"
        report = TextView(this).apply { textSize = 12f; setPadding(16, 8, 16, 8); text = AppCompatibilityChecker.report(this@EmbedPocActivity) + "\nPrueba con: $pkg" }
        val surface = SurfaceView(this)
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.TOP
                addView(report, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                addView(surface, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            },
        )
        surface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) = Unit
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                if (display == null && width > 0 && height > 0) start(holder.surface, width, height, pkg)
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                display?.release()
                display = null
            }
        })
    }

    private fun start(surface: Surface, w: Int, h: Int, pkg: String) {
        fun say(line: String) = runOnUiThread { report.append("\n$line") }
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            say("RESULTADO: $pkg no está instalada.")
            return
        }
        try {
            val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
            val vd = getSystemService(DisplayManager::class.java)
                .createVirtualDisplay("embed-poc", w, h, resources.displayMetrics.densityDpi, surface, flags)
            display = vd
            say("Pantalla virtual creada: id ${vd.display.displayId}, ${w}x$h")
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(vd.display.displayId)
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK), options.toBundle())
            say("Inicio pedido sin error. Si la app se ve abajo, la integración real funciona; si cubre toda la pantalla, no.")
        } catch (e: Throwable) {
            say("RESULTADO: no se pudo -> ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    override fun onDestroy() {
        display?.release()
        display = null
        super.onDestroy()
    }
}
