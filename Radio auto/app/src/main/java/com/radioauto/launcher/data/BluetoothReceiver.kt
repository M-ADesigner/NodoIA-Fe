package com.radioauto.launcher.data

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat

/** When the driver's phone connects, keep the launcher on screen. It opens no app and sends no Play. */
class BluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return
        if (!isPhone(context, intent)) return

        val pending = goAsync()
        LauncherFocus.schedule(context) { pending.finish() }
    }

    /** Only phones: a key fob or a headset connecting should not move the launcher. */
    private fun isPhone(context: Context, intent: Intent): Boolean {
        // BLUETOOTH_CONNECT only exists from Android 12; before that the legacy permission is granted at install.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        return device?.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.PHONE
    }
}
