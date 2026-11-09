package com.radioauto.launcher.data

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Which Bluetooth profiles the phone holds with the radio. Calls use HFP; music needs A2DP. */
object BtProfiles {

    // Profile ids of the head-unit roles; they are hidden from the SDK constants, so the numbers are used.
    const val HEADSET = BluetoothProfile.HEADSET
    const val A2DP = BluetoothProfile.A2DP
    const val A2DP_SINK = 11
    const val AVRCP_CONTROLLER = 12
    const val HEADSET_CLIENT = 16

    private val ALL = intArrayOf(HEADSET, A2DP, A2DP_SINK, AVRCP_CONTROLLER, HEADSET_CLIENT)

    // Touched only on the main thread: proxy callbacks and the broadcast receiver both arrive there.
    private val connected = HashMap<Int, Boolean>()
    private val proxies = HashMap<Int, BluetoothProfile>()
    private var appContext: Context? = null
    private var changed: (() -> Unit)? = null

    val hfp: Boolean get() = connected[HEADSET] == true || connected[HEADSET_CLIENT] == true
    val a2dp: Boolean get() = connected[A2DP] == true || connected[A2DP_SINK] == true

    fun isConnected(profile: Int): Boolean = connected[profile] == true

    fun init(context: Context, onChange: () -> Unit) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app
        changed = onChange
        refresh()
    }

    /** Opens a proxy for each role and reads who is connected; a role this radio does not have just never answers. */
    fun refresh() {
        val app = appContext ?: return
        if (!hasPermission(app)) return
        val adapter = ContextCompat.getSystemService(app, BluetoothManager::class.java)?.adapter ?: return
        for (profile in ALL) {
            val existing = proxies[profile]
            if (existing != null) {
                update(profile, existing)
                continue
            }
            try {
                adapter.getProfileProxy(app, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(p: Int, proxy: BluetoothProfile) {
                        proxies[profile] = proxy
                        update(profile, proxy)
                    }

                    override fun onServiceDisconnected(p: Int) {
                        proxies.remove(profile)
                        set(profile, false)
                    }
                }, profile)
            } catch (e: Exception) {
                // Unknown profile numbers or a missing permission: that role is simply not available here.
            }
        }
    }

    /** From a CONNECTION_STATE_CHANGED broadcast of [profile]. */
    fun onBroadcast(profile: Int, state: Int) {
        when (state) {
            BluetoothProfile.STATE_CONNECTED -> set(profile, true)
            BluetoothProfile.STATE_DISCONNECTED -> set(profile, false)
        }
    }

    /** The phone is gone: no role can still be connected. */
    fun clear() {
        ALL.forEach { set(it, false) }
    }

    private fun update(profile: Int, proxy: BluetoothProfile) {
        val any = try {
            proxy.connectedDevices.isNotEmpty()
        } catch (e: Exception) {
            return
        }
        set(profile, any)
    }

    private fun set(profile: Int, value: Boolean) {
        if (connected.put(profile, value) == value) return
        changed?.invoke()
    }

    private fun hasPermission(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
}
