package com.radioauto.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
)

class AppRepository(private val context: Context) {

    private val iconCache = HashMap<String, ImageBitmap>()

    fun load(): List<AppInfo> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(query, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                val pkg = info.activityInfo.packageName
                AppInfo(
                    packageName = pkg,
                    label = info.loadLabel(pm).toString(),
                    icon = iconCache.getOrPut(pkg) { info.loadIcon(pm).toImageBitmap() },
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Emits the app list now and again whenever a package is installed, replaced or removed. */
    fun observe(): Flow<List<AppInfo>> = callbackFlow {
        trySend(load())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                i.data?.schemeSpecificPart?.let { iconCache.remove(it) }
                trySend(load())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }.flowOn(Dispatchers.Default)

    fun launch(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }
}

/** Icons are never drawn bigger than ~76 dp, so keep them at 128 px instead of the app's full-size art (up to 432 px). */
private const val ICON_PX = 128

private fun Drawable.toImageBitmap(): ImageBitmap {
    val bmp = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
    setBounds(0, 0, ICON_PX, ICON_PX)
    draw(Canvas(bmp))
    return bmp.asImageBitmap()
}
