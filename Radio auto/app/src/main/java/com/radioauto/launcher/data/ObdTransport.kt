package com.radioauto.launcher.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.delay
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

/** A byte pipe to an ELM327; the protocol on top does not care whether it is USB, WiFi or Bluetooth. */
interface ObdTransport {
    val label: String
    suspend fun connect()
    suspend fun send(cmd: String): String
    fun close()
}

/** Sends one command and collects the answer up to the ">" prompt (or a 2 s timeout). */
private suspend fun exchange(out: OutputStream, input: InputStream, cmd: String): String {
    out.write((cmd + "\r").toByteArray())
    out.flush()
    val sb = StringBuilder()
    val end = System.currentTimeMillis() + 2_000
    while (System.currentTimeMillis() < end) {
        if (input.available() > 0) {
            val c = input.read()
            if (c < 0) break
            if (c.toChar() == '>') return sb.toString()
            sb.append(c.toChar())
        } else delay(10)
    }
    return sb.toString()
}

/** A paired Bluetooth device the driver can pick as the OBD adapter. */
data class BtAdapterInfo(val name: String, val address: String)

private val OBD_NAMES = Regex("(?i)obd|elm|v-?link|vgate|icar|konnwei")

private fun hasBtPermission(context: Context) =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

/** Paired devices for the picker in Settings; empty without the permission or without Bluetooth. */
@SuppressLint("MissingPermission")
fun pairedBluetoothDevices(context: Context): List<BtAdapterInfo> {
    if (!hasBtPermission(context)) return emptyList()
    val adapter: BluetoothAdapter = ContextCompat.getSystemService(context, BluetoothManager::class.java)?.adapter ?: return emptyList()
    return runCatching {
        adapter.bondedDevices.map { BtAdapterInfo(it.name ?: it.address, it.address) }.sortedBy { it.name.lowercase() }
    }.getOrDefault(emptyList())
}

/**
 * Classic Bluetooth serial (SPP). Uses the device picked in Settings ([address]); with none picked it takes the first
 * paired device whose name looks like an OBD dongle.
 */
class BluetoothSppTransport(private val context: Context, private val address: String? = null) : ObdTransport {
    override val label = "Bluetooth"
    private var socket: BluetoothSocket? = null
    private val spp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    override suspend fun connect() {
        if (!hasBtPermission(context)) error("Sin permiso Bluetooth")
        val adapter: BluetoothAdapter = ContextCompat.getSystemService(context, BluetoothManager::class.java)?.adapter
            ?: error("Sin Bluetooth")
        val bonded = adapter.bondedDevices
        val device: BluetoothDevice = (if (!address.isNullOrBlank()) bonded.firstOrNull { it.address == address } else null)
            ?: bonded.firstOrNull { it.name?.let(OBD_NAMES::containsMatchIn) == true }
            ?: error("Adaptador OBD no emparejado")
        // Needs the scan permission on Android 12+, and we never scan: ignore a refusal.
        runCatching { adapter.cancelDiscovery() }
        socket = device.createRfcommSocketToServiceRecord(spp).also { it.connect() }
    }

    override suspend fun send(cmd: String): String {
        val s = socket ?: error("Desconectado")
        return exchange(s.outputStream, s.inputStream, cmd)
    }

    override fun close() {
        runCatching { socket?.close() }
        socket = null
    }
}

/** WiFi ELM327 clones: a TCP socket, usually 192.168.0.10:35000 once the radio joins the adapter's own network. */
class WifiTcpTransport(private val host: String, private val port: Int) : ObdTransport {
    override val label = "WiFi"
    private var socket: Socket? = null

    override suspend fun connect() {
        socket = Socket().also {
            it.connect(InetSocketAddress(host, port), 4_000)
            it.soTimeout = 2_000
        }
    }

    override suspend fun send(cmd: String): String {
        val s = socket ?: error("Desconectado")
        return exchange(s.getOutputStream(), s.getInputStream(), cmd)
    }

    override fun close() {
        runCatching { socket?.close() }
        socket = null
    }
}

/** An ELM327 plugged into the radio's USB port (the most dependable route on head units whose Bluetooth is taken). */
class UsbSerialTransport(private val context: Context, private val baud: Int = 38_400) : ObdTransport {
    override val label = "USB"
    private var port: UsbSerialPort? = null

    override suspend fun connect() {
        val manager = context.getSystemService(UsbManager::class.java) ?: error("Sin USB")
        val driver = UsbSerialProber.getDefaultProber().findAllDrivers(manager).firstOrNull()
            ?: error("No hay adaptador USB conectado")
        if (!manager.hasPermission(driver.device)) {
            // The system asks the driver; the next attempt finds the permission granted.
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val intent = PendingIntent.getBroadcast(context, 0, Intent("${context.packageName}.USB_PERMISSION").setPackage(context.packageName), flags)
            manager.requestPermission(driver.device, intent)
            error("Acepta el permiso USB en la pantalla")
        }
        val connection = manager.openDevice(driver.device) ?: error("No se pudo abrir el USB")
        port = driver.ports.first().also {
            it.open(connection)
            it.setParameters(baud, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        }
    }

    override suspend fun send(cmd: String): String {
        val p = port ?: error("Desconectado")
        p.write((cmd + "\r").toByteArray(), 2_000)
        val sb = StringBuilder()
        val buf = ByteArray(256)
        val end = System.currentTimeMillis() + 2_000
        while (System.currentTimeMillis() < end) {
            val n = p.read(buf, 200)
            if (n > 0) {
                val text = String(buf, 0, n)
                val prompt = text.indexOf('>')
                if (prompt >= 0) {
                    sb.append(text, 0, prompt)
                    return sb.toString()
                }
                sb.append(text)
            } else delay(10)
        }
        return sb.toString()
    }

    override fun close() {
        runCatching { port?.close() }
        port = null
    }
}
