package com.radioauto.launcher.link

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket

/** A phone sent the right code (or paired over Bluetooth); the driver must press "Permitir". */
data class JoinRequest(val deviceName: String, val address: String?)

/** The song on the phone that has the music; empty fields when its app shares nothing. */
data class LinkTrack(val title: String, val artist: String, val playing: Boolean, val art: Bitmap?)

enum class LinkBluetooth {
    /** This radio has no Bluetooth that apps can use (some head units handle it in a separate module). */
    Unavailable,
    Off,
    NeedsPermission,
    Ready,
}

data class LinkState(
    val running: Boolean = false,
    val code: String = "",
    val host: String? = null,
    val port: Int = 0,
    val request: JoinRequest? = null,
    val devices: List<DeviceInfo> = emptyList(),
    val playingId: String? = null,
    val bluetooth: LinkBluetooth = LinkBluetooth.Unavailable,
    /** The name phones see in their list of Bluetooth devices. */
    val bluetoothName: String? = null,
    val track: LinkTrack? = null,
) {
    val joinLink: JoinLink? get() = host?.takeIf { code.isNotEmpty() }?.let { JoinLink(it, port, code) }
    val connected: Boolean get() = devices.any { it.online }
}

/**
 * Radio side of the phone link: accepts phones over Wi-Fi (QR + code) and over Bluetooth (RFCOMM), asks the
 * driver to approve new ones and plays the audio of the first phone that shares it. Same protocol as AutoPlay.
 */
object LinkServer {

    private class Session(val id: String, val name: String, val token: String, val trust: String) {
        var connection: Connection? = null
        var sharing = false
        var bluetooth = false
        var track: LinkTrack? = null
        var artKey = ""
        var resumeTimeout: Job? = null
    }

    private const val CAR_NAME = "Radio Launcher"
    private const val HANDSHAKE_TIMEOUT_MILLIS = 10_000
    private const val SESSION_READ_TIMEOUT_MILLIS = 10_000
    private const val APPROVAL_TIMEOUT_MILLIS = 60_000L
    private const val RESUME_WINDOW_MILLIS = 90_000L
    private const val MAX_DEVICES = 4
    private const val MAX_FAILED_ATTEMPTS = 5
    private const val BLUETOOTH_RETRY_SECONDS = 5
    private const val BLOCK_GRACE_MILLIS = 1_500L
    private const val DEFAULT_VOLUME = 80

    private val _state = MutableStateFlow(LinkState())
    val state: StateFlow<LinkState> = _state.asStateFlow()

    private val lock = Any()
    private var appContext: Context? = null
    private lateinit var player: LinkPlayer
    private lateinit var trusted: TrustedPhones
    private var scope: CoroutineScope? = null
    private var serverSocket: ServerSocket? = null
    private var bluetoothServer: BluetoothServerSocket? = null

    @Volatile private var code = SessionCode.generate(0)
    private var failedAttempts = 0
    private var pendingDecision: CompletableDeferred<Boolean>? = null
    private val sessions = mutableListOf<Session>()
    private var rosterChanged = Channel<Unit>(Channel.CONFLATED)
    private var volume = DEFAULT_VOLUME

    /** While another app has the audio, the phone's music is paused and what it still sends is dropped. */
    @Volatile private var audioBlocked = false
    private var resumeAfterBlock = false
    private var blockedAt = 0L

    fun start(context: Context) {
        synchronized(lock) {
            if (scope != null) return
            val app = context.applicationContext
            appContext = app
            player = LinkPlayer(app).also { it.onFocusChange = ::onFocusChange }
            trusted = TrustedPhones(app)
            val socket = try {
                ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(Protocol.DEFAULT_PORT))
                }
            } catch (e: IOException) {
                ServerSocket(0) // default port taken: any free port, the QR carries it anyway
            }
            serverSocket = socket
            val newScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope = newScope
            rosterChanged = Channel(Channel.CONFLATED)
            _state.update { it.copy(running = true, host = localIpv4Address(), port = socket.localPort) }
            newCode()
            player.setGain(gainFor(volume))
            newScope.launch { acceptLoop(socket) }
            newScope.launch { housekeeping() }
            newScope.launch { broadcastRoster() }
            startBluetoothLocked()
        }
    }

    /** Called again after the driver grants the Bluetooth permission; also retried every few seconds. */
    fun startBluetooth() = synchronized(lock) { startBluetoothLocked() }

    @SuppressLint("MissingPermission") // checked just below
    private fun startBluetoothLocked() {
        val scope = scope ?: return
        val context = appContext ?: return
        if (bluetoothServer != null) return
        val adapter = ContextCompat.getSystemService(context, BluetoothManager::class.java)?.adapter
        val status = when {
            adapter == null -> LinkBluetooth.Unavailable
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ->
                LinkBluetooth.NeedsPermission
            !adapter.isEnabled -> LinkBluetooth.Off
            else -> LinkBluetooth.Ready
        }
        val server = if (status == LinkBluetooth.Ready) {
            try {
                adapter!!.listenUsingRfcommWithServiceRecord("AutoPlay", Protocol.BLUETOOTH_UUID)
            } catch (e: Exception) {
                null // IOException or SecurityException: this radio's Bluetooth is not usable by apps
            }
        } else null
        bluetoothServer = server
        val name = if (server != null) runCatching { adapter?.name }.getOrNull() else null
        val shown = if (status == LinkBluetooth.Ready && server == null) LinkBluetooth.Unavailable else status
        _state.update { it.copy(bluetooth = shown, bluetoothName = name) }
        if (server != null) scope.launch { bluetoothAcceptLoop(server) }
    }

    /** Answer from the driver to [LinkState.request]. */
    fun respond(allow: Boolean) {
        synchronized(lock) { pendingDecision }?.complete(allow)
    }

    /** Disconnects a phone; with [forget] it also loses its approval and has to join again. */
    fun remove(deviceId: String, forget: Boolean = false) {
        val connection = synchronized(lock) {
            val session = sessions.find { it.id == deviceId } ?: return
            sessions.remove(session)
            if (forget) trusted.forget(session.trust)
            session.resumeTimeout?.cancel()
            session.connection.also { session.connection = null }
        }
        rosterChanged.trySend(Unit)
        scope?.launch { connection?.closeWith(Message.Bye) }
    }

    fun newCode() {
        val fresh = SessionCode.generate(System.currentTimeMillis())
        synchronized(lock) {
            code = fresh
            failedAttempts = 0
        }
        _state.update { it.copy(code = fresh.value) }
    }

    /** True when a phone of the link has the music, so the bar's buttons go to it. */
    val hasPlayer: Boolean get() = synchronized(lock) { currentPlayerLocked() != null }

    /** Play, next and previous from the bar or the steering wheel go to the phone that has the music. */
    fun mediaCommand(action: MediaAction): Boolean {
        val connection = synchronized(lock) { currentPlayerLocked()?.connection } ?: return false
        audioBlocked = false
        resumeAfterBlock = false
        scope?.launch { runCatching { connection.send(Message.MediaCommand(action)) } }
        return true
    }

    private fun gainFor(level: Int) = (level / 100f).let { it * it }

    private fun setVolume(level: Int) {
        volume = level.coerceIn(0, 100)
        player.setGain(gainFor(volume))
        rosterChanged.trySend(Unit)
    }

    private fun onFocusChange(change: Int) {
        when (focusAction(change)) {
            FocusAction.None -> Unit
            FocusAction.Pause, FocusAction.Stop -> {
                val stop = focusAction(change) == FocusAction.Stop
                if (audioBlocked && !stop) return
                val wasPlaying = synchronized(lock) { currentPlayerLocked() } != null && _state.value.track?.playing != false
                audioBlocked = true
                resumeAfterBlock = !stop && wasPlaying
                blockedAt = System.currentTimeMillis()
                if (stop) player.stop() else player.pause()
                // The phone's own player stops too, or it would keep going unheard
                if (wasPlaying) sendToPlayer(MediaAction.PlayPause)
            }
            FocusAction.Resume -> {
                if (!audioBlocked) return
                audioBlocked = false
                if (resumeAfterBlock) sendToPlayer(MediaAction.PlayPause)
                resumeAfterBlock = false
            }
        }
    }

    private fun sendToPlayer(action: MediaAction) {
        val connection = synchronized(lock) { currentPlayerLocked()?.connection } ?: return
        scope?.launch { runCatching { connection.send(Message.MediaCommand(action)) } }
    }

    private fun onNowPlaying(session: Session, song: Message.NowPlaying) {
        // The cover only changes with the song, so pause and play reuse the decoded one
        val art = if (song.art == session.artKey) {
            session.track?.art
        } else {
            runCatching { Base64.decode(song.art, Base64.DEFAULT).let { BitmapFactory.decodeByteArray(it, 0, it.size) } }.getOrNull()
        }
        synchronized(lock) {
            session.artKey = song.art
            session.track = LinkTrack(song.title, song.artist, song.playing, art).takeIf { song.title.isNotEmpty() }
        }
        // Someone pressed play on the phone after the radio took the audio: let it through
        if (audioBlocked && song.playing && System.currentTimeMillis() - blockedAt > BLOCK_GRACE_MILLIS) {
            audioBlocked = false
            resumeAfterBlock = false
        }
        rosterChanged.trySend(Unit)
    }

    private suspend fun acceptLoop(socket: ServerSocket) {
        val scope = scope ?: return
        while (scope.isActive) {
            val client = try {
                socket.accept()
            } catch (e: IOException) {
                break
            }
            scope.launch { handle(Connection(client)) }
        }
    }

    private suspend fun bluetoothAcceptLoop(server: BluetoothServerSocket) {
        val scope = scope ?: return
        while (scope.isActive) {
            val client = try {
                server.accept()
            } catch (e: IOException) {
                break // closed, or Bluetooth was turned off
            }
            scope.launch { handle(Connection(client)) }
        }
        synchronized(lock) { if (bluetoothServer === server) bluetoothServer = null }
        runCatching { server.close() }
    }

    /** Every second: expired codes, a new IP after Wi-Fi comes back, Bluetooth turned on. */
    private suspend fun housekeeping() {
        var tick = 0
        while (true) {
            delay(1_000)
            if (!code.isValid(System.currentTimeMillis())) newCode()
            val host = localIpv4Address()
            if (host != _state.value.host) _state.update { it.copy(host = host) }
            if (++tick % BLUETOOTH_RETRY_SECONDS == 0) startBluetooth()
        }
    }

    /** Publishes the roster to the UI and to every phone; conflated so only the latest state is sent. */
    private suspend fun broadcastRoster() {
        var lastPlaying: String? = null
        for (signal in rosterChanged) {
            var track: LinkTrack? = null
            val (devices, playingId, connections) = synchronized(lock) {
                val playing = currentPlayerLocked()
                track = playing?.track
                Triple(
                    sessions.map { DeviceInfo(it.id, it.name, it.connection != null, it.sharing, it.bluetooth) },
                    playing?.id,
                    sessions.mapNotNull { it.connection },
                )
            }
            _state.update { it.copy(devices = devices, playingId = playingId, track = track) }
            if (playingId == null && lastPlaying != null) player.stop()
            lastPlaying = playingId

            val roster = Message.Roster(devices, playingId, volume, CAR_NAME)
            connections.forEach { connection ->
                try {
                    connection.send(roster)
                } catch (e: IOException) {
                    connection.close() // its read loop notices and marks the phone as reconnecting
                }
            }
        }
    }

    private suspend fun handle(connection: Connection) {
        connection.readTimeoutMillis = HANDSHAKE_TIMEOUT_MILLIS
        val first = try {
            connection.receive()
        } catch (e: IOException) {
            connection.close()
            return
        }
        when (first) {
            is Message.Hello -> handleHello(connection, first)
            is Message.Resume -> handleResume(connection, first)
            else -> connection.close()
        }
    }

    private suspend fun handleHello(connection: Connection, hello: Message.Hello) {
        if (hello.version != Protocol.VERSION) {
            return connection.closeWith(Message.Rejected("Versión incompatible: actualiza la app en ambos equipos"))
        }
        val isTrusted = trusted.isTrusted(hello.trust)
        // Bluetooth pairing already proved the phone is in the car; the driver still approves it
        val needsCode = !isTrusted && !connection.isBluetooth
        if (isTrusted) {
            // The same phone is back (app restarted) while its old session waits to resume: replace it
            synchronized(lock) { sessions.find { it.trust == hello.trust } }?.let { remove(it.id) }
        }
        val decision = CompletableDeferred<Boolean>()
        val rejection = synchronized(lock) {
            when {
                sessions.size >= MAX_DEVICES -> "La radio ya tiene $MAX_DEVICES celulares conectados"
                isTrusted -> null
                pendingDecision != null -> "La radio está atendiendo otra solicitud; intenta en unos segundos"
                needsCode && !code.redeem(hello.code, System.currentTimeMillis()) -> {
                    failedAttempts++
                    "Código incorrecto o vencido"
                }
                else -> {
                    pendingDecision = decision
                    null
                }
            }
        }
        if (rejection != null) {
            // Too many wrong guesses burn the code so it cannot be brute-forced
            if (synchronized(lock) { failedAttempts >= MAX_FAILED_ATTEMPTS }) newCode()
            return connection.closeWith(Message.Rejected(rejection))
        }

        if (!isTrusted) {
            _state.update { it.copy(request = JoinRequest(hello.deviceName, connection.remoteAddress)) }
            val allowed = withTimeoutOrNull(APPROVAL_TIMEOUT_MILLIS) { decision.await() } ?: false
            synchronized(lock) { pendingDecision = null }
            _state.update { it.copy(request = null) }
            if (needsCode) newCode() // the old one was used; the next phone needs a fresh one
            if (!allowed) return connection.closeWith(Message.Rejected("La radio rechazó la conexión"))
        }

        val trust = if (isTrusted) hello.trust else trusted.trust(hello.deviceName)
        val session = synchronized(lock) {
            Session(SessionCode.newResumeToken().take(8), hello.deviceName.take(30), SessionCode.newResumeToken(), trust)
                .also { sessions.add(it) }
        }
        try {
            connection.send(Message.Accepted(session.token, CAR_NAME, session.id, trusted.carId, trust))
        } catch (e: IOException) {
            connection.close()
            onConnectionLost(session)
            return
        }
        runSession(session, connection)
    }

    private fun handleResume(connection: Connection, resume: Message.Resume) {
        val session = synchronized(lock) {
            sessions.find { it.token == resume.token }?.also { session ->
                session.resumeTimeout?.cancel()
                // The phone noticed the drop before we did: the old socket is dead, replace it
                session.connection?.close()
                session.connection = null
            }
        } ?: return connection.closeWith(Message.Rejected("La sesión ya no existe; únete con un código nuevo"))
        try {
            connection.send(Message.Accepted(session.token, CAR_NAME, session.id, trusted.carId, session.trust))
        } catch (e: IOException) {
            connection.close()
            onConnectionLost(session)
            return
        }
        runSession(session, connection)
    }

    private fun runSession(session: Session, connection: Connection) {
        synchronized(lock) {
            session.connection = connection
            session.bluetooth = connection.isBluetooth
        }
        rosterChanged.trySend(Unit)
        connection.readTimeoutMillis = SESSION_READ_TIMEOUT_MILLIS

        var endedByPhone = false
        var decoder: AacDecoder? = null
        try {
            while (true) {
                when (val message = connection.receive()) {
                    is Message.Audio -> if (isPlaying(session) && !audioBlocked) player.write(session.id, message.pcm)
                    is Message.AudioAac -> if (isPlaying(session) && !audioBlocked) {
                        val aac = decoder ?: AacDecoder().also { decoder = it }
                        aac.decode(message.frame) { pcm -> player.write(session.id, pcm) }
                    }
                    Message.Ping -> connection.send(Message.Pong)
                    is Message.Volume -> if (isPlaying(session)) setVolume(message.level)
                    is Message.NowPlaying -> onNowPlaying(session, message)
                    is Message.AudioState -> {
                        synchronized(lock) { session.sharing = message.sharing }
                        rosterChanged.trySend(Unit)
                    }
                    Message.Bye -> {
                        endedByPhone = true
                        break
                    }
                    else -> Unit
                }
            }
        } catch (e: IOException) {
            // Network drop or timeout: handled below
        } finally {
            connection.close()
            decoder?.release()
        }

        // remove() or a resume may already have taken the connection
        val stillOurs = synchronized(lock) {
            (session.connection === connection).also { if (it) session.connection = null }
        }
        if (!stillOurs) return
        if (endedByPhone) remove(session.id) else onConnectionLost(session)
    }

    private fun isPlaying(session: Session) = synchronized(lock) { currentPlayerLocked() === session }

    private fun currentPlayerLocked() = sessions.firstOrNull { it.connection != null && it.sharing }

    /** Keeps the phone's place for a while so it can come back with its resume token. */
    private fun onConnectionLost(session: Session) {
        rosterChanged.trySend(Unit)
        val job = scope?.launch {
            delay(RESUME_WINDOW_MILLIS)
            if (synchronized(lock) { session.connection == null }) remove(session.id)
        }
        synchronized(lock) { session.resumeTimeout = job }
    }
}
