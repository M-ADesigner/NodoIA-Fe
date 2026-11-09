package com.radioauto.launcher.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** AUTO tries USB, then WiFi, then Bluetooth: on head units the Bluetooth is often reserved for the phone. */
enum class ObdConnection(val label: String) { AUTO("Auto"), USB("USB"), WIFI("WiFi"), BLUETOOTH("Bluetooth"), DEMO("Demo") }

/** What the car's computer reports; null means "not available" (many cars skip PIDs, e.g. fuel level). */
data class ObdData(
    val connected: Boolean = false,
    val via: String = "",
    val rpm: Int? = null,
    val speedKmh: Int? = null,
    val coolantC: Int? = null,
    val intakeC: Int? = null,
    val loadPct: Int? = null,
    val throttlePct: Int? = null,
    val fuelPct: Int? = null,
    val voltageMv: Int? = null,
    val runtimeS: Int? = null,
    val milKm: Int? = null,
    val dtcs: List<String> = emptyList(),
    val idleUnstable: Boolean = false,
    // Current trip, counted while connected.
    val tripSeconds: Int = 0,
    val tripKm: Double = 0.0,
    val avgKmh: Int = 0,
    val maxKmh: Int = 0,
    val maxCoolantC: Int? = null,
    val minVoltageMv: Int? = null,
    val maxVoltageMv: Int? = null,
)

/** Talks to an ELM327 (or fakes one in demo mode) and keeps [state] current. One command at a time, never in parallel. */
object Obd {
    private val _state = MutableStateFlow(ObdData())
    val state: StateFlow<ObdData> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var key: String? = null

    private val _status = MutableStateFlow("Sin iniciar")

    /** One plain sentence about what the connection is doing or why it failed, for the help dialog. */
    val status: StateFlow<String> = _status.asStateFlow()

    private val retry = Channel<Unit>(Channel.CONFLATED)

    private val _history = MutableStateFlow(ObdHistory())

    /** The last 30 drives and the fault codes seen so far, kept on the device. */
    val history: StateFlow<ObdHistory> = _history.asStateFlow()
    private var store: TripStore? = null
    private var intervalMs = 500L

    /** Skips the wait before the next attempt. */
    fun retryNow() {
        retry.trySend(Unit)
    }

    /** Safe to call on every settings change: it only restarts when the connection actually changed. */
    fun configure(
        context: Context, connection: ObdConnection, host: String, port: Int,
        btAddress: String = "", pollMs: Int = 500,
    ) {
        val newKey = "$connection/$host/$port/$btAddress/$pollMs"
        if (newKey == key && job?.isActive == true) return
        stop()
        key = newKey
        val app = context.applicationContext
        intervalMs = pollMs.toLong()
        if (store == null) {
            store = TripStore(app)
            _history.value = store!!.load()
        }
        job = scope.launch {
            when (connection) {
                ObdConnection.DEMO -> {
                    _status.value = "Modo demo (datos simulados)"
                    demo()
                }
                else -> {
                    var wait = 2_000L
                    var attempt = 0
                    while (isActive) {
                        // AUTO walks USB -> WiFi -> Bluetooth; the others always use their own way.
                        val way = when (connection) {
                            ObdConnection.USB -> ObdConnection.USB
                            ObdConnection.WIFI -> ObdConnection.WIFI
                            ObdConnection.BLUETOOTH -> ObdConnection.BLUETOOTH
                            else -> listOf(ObdConnection.USB, ObdConnection.WIFI, ObdConnection.BLUETOOTH)[attempt % 3]
                        }
                        val transport = when (way) {
                            ObdConnection.USB -> UsbSerialTransport(app)
                            ObdConnection.WIFI -> WifiTcpTransport(host, port)
                            else -> BluetoothSppTransport(app, btAddress)
                        }
                        _status.value = "Conectando por ${transport.label}…"
                        var linked = false
                        val failure = runCatching { session(transport) { linked = true; _status.value = "Conectado por ${transport.label}" } }
                            .exceptionOrNull()
                        transport.close()
                        _state.value = ObdData()
                        if (!isActive) break
                        // Backoff: a missing adapter is checked less and less often; a dropped link retries soon.
                        wait = if (linked) 2_000L else (wait * 2).coerceAtMost(30_000L)
                        // In AUTO the other way is tried right after, and the long wait only comes after both failed.
                        val pause = if (connection == ObdConnection.AUTO && attempt % 3 != 2) 1_000L else wait
                        _status.value = "${friendly(way, failure, host, port)} · reintenta en ${pause / 1000} s"
                        attempt++
                        withTimeoutOrNull(pause) { retry.receive() }
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        key = null
        _state.value = ObdData()
    }

    private fun friendly(way: ObdConnection, e: Throwable?, host: String, port: Int): String = when {
        e is IllegalStateException && e.message != null -> e.message!!
        way == ObdConnection.WIFI -> "No se alcanza el adaptador WiFi ($host:$port)"
        way == ObdConnection.USB -> "El adaptador USB no respondió"
        else -> "El adaptador Bluetooth no respondió"
    }

    private suspend fun session(t: ObdTransport, onLinked: () -> Unit) {
        t.connect()
        onLinked()
        for (cmd in listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")) t.send(cmd)

        // Ask the car which PIDs it has so we never poll (or show) what it cannot answer.
        val supported = HashSet<Int>()
        var base = 0x00
        while (base <= 0x40) {
            val block = parseSupported(base, t.send("01%02X".format(base)))
            supported += block
            if ((base + 0x20) !in block) break
            base += 0x20
        }
        val pids = if (supported.isEmpty()) ObdPid.entries else ObdPid.entries.filter { it.number in supported }

        val trip = TripStats()
        var cur = ObdData(connected = true, via = t.label)
        _state.value = cur
        var lastDtc = 0L
        var lastTick = System.currentTimeMillis()
        val idleRpm = ArrayDeque<Int>()
        val startedAt = System.currentTimeMillis()

        try {
        while (scope.isActive) {
            for (pid in pids) {
                val v = parseObd(pid, t.send("01${pid.code}"))
                cur = when (pid) {
                    ObdPid.RPM -> cur.copy(rpm = v)
                    ObdPid.SPEED -> cur.copy(speedKmh = v)
                    ObdPid.COOLANT -> cur.copy(coolantC = v)
                    ObdPid.INTAKE -> cur.copy(intakeC = v)
                    ObdPid.LOAD -> cur.copy(loadPct = v)
                    ObdPid.THROTTLE -> cur.copy(throttlePct = v)
                    ObdPid.FUEL -> cur.copy(fuelPct = v)
                    ObdPid.VOLTAGE_MV -> cur.copy(voltageMv = v)
                    ObdPid.RUNTIME_S -> cur.copy(runtimeS = v)
                    ObdPid.MIL_KM -> cur.copy(milKm = v)
                }
            }
            val now = System.currentTimeMillis()
            if (now - lastDtc > 60_000) {
                lastDtc = now
                val codes = parseDtcs(t.send("03"), "43") + parseDtcs(t.send("07"), "47")
                cur = cur.copy(dtcs = codes.distinct())
                logNewDtcs(cur.dtcs, now)
            }
            cur = trip.update(cur, now - lastTick)
            lastTick = now
            cur = cur.copy(idleUnstable = idleUnstable(idleRpm, cur))
            _state.value = cur
            delay(intervalMs)
        }
        } finally {
            // The link ended (unplugged, key off, settings changed): keep the drive if it was a real one.
            saveTrip(startedAt, cur)
        }
    }

    private fun saveTrip(startedAt: Long, d: ObdData) {
        val s = store ?: return
        if (d.tripSeconds < 60 || d.tripKm < 0.2) return
        val record = TripRecord(startedAt, d.tripSeconds, d.tripKm, d.avgKmh, d.maxKmh, d.maxCoolantC, d.minVoltageMv, d.maxVoltageMv)
        _history.value = _history.value.copy(trips = pushTrip(_history.value.trips, record)).also { s.save(it) }
    }

    /** A code is logged the first time it is seen; it is not logged again every minute while it stays on. */
    private fun logNewDtcs(codes: List<String>, now: Long) {
        val s = store ?: return
        var h = _history.value
        codes.filter { c -> h.dtcs.none { it.code == c } }.forEach { h = h.copy(dtcs = pushDtc(h.dtcs, DtcRecord(it, now))) }
        if (h !== _history.value) {
            _history.value = h
            s.save(h)
        }
    }

    /** Idle (stopped, warm engine) swinging more than ±150 rpm over the last ~20 samples. */
    private fun idleUnstable(window: ArrayDeque<Int>, d: ObdData): Boolean {
        val rpm = d.rpm
        if (rpm == null || (d.speedKmh ?: 0) != 0 || (d.coolantC ?: 0) < 75 || rpm < 400) {
            window.clear()
            return false
        }
        window.addLast(rpm)
        if (window.size > 20) window.removeFirst()
        return window.size >= 15 && window.max() - window.min() > 300
    }

    /** Fake car for trying the screens without driving: warms up, revs and drives in a loop. */
    private suspend fun demo() {
        val trip = TripStats()
        var last = System.currentTimeMillis()
        var t = 0.0
        while (scope.isActive) {
            t += 0.3
            val speed = (40 + 60 * sin(t / 4)).toInt().coerceAtLeast(0)
            val d = ObdData(
                connected = true, via = "Demo",
                rpm = 900 + speed * 28 + (150 * sin(t * 2)).toInt(),
                speedKmh = speed,
                coolantC = (60 + t.coerceAtMost(28.0)).toInt(),
                intakeC = 31, loadPct = 35 + speed / 3,
                throttlePct = (18 + 14 * sin(t * 1.3)).toInt(),
                fuelPct = 62, voltageMv = 14_050 + (100 * sin(t)).toInt(),
                runtimeS = t.toInt(), milKm = 0,
            )
            val now = System.currentTimeMillis()
            _state.value = trip.update(d, now - last)
            last = now
            delay(300)
        }
    }
}

/** Distance, averages and extremes of the current trip, fed by every reading. */
private class TripStats {
    private var seconds = 0.0
    private var meters = 0.0
    private var speedSum = 0.0
    private var samples = 0
    private var maxSpeed = 0
    private var maxCoolant: Int? = null
    private var minV: Int? = null
    private var maxV: Int? = null

    fun update(d: ObdData, dtMs: Long): ObdData {
        val dt = dtMs.coerceIn(0, 5_000) / 1000.0
        val speed = d.speedKmh ?: 0
        seconds += dt
        meters += speed / 3.6 * dt
        speedSum += speed
        samples++
        maxSpeed = maxOf(maxSpeed, speed)
        d.coolantC?.let { maxCoolant = maxOf(maxCoolant ?: it, it) }
        d.voltageMv?.let {
            minV = minOf(minV ?: it, it)
            maxV = maxOf(maxV ?: it, it)
        }
        return d.copy(
            tripSeconds = seconds.toInt(), tripKm = meters / 1000,
            avgKmh = if (samples > 0) (speedSum / samples).toInt() else 0,
            maxKmh = maxSpeed, maxCoolantC = maxCoolant, minVoltageMv = minV, maxVoltageMv = maxV,
        )
    }
}
