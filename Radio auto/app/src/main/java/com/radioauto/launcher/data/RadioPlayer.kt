package com.radioauto.launcher.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class RadioStation(val name: String, val url: String, val codec: String, val bitrate: Int, val tags: String)

enum class RadioStatus { IDLE, LOADING, PLAYING, ERROR }

data class RadioState(
    val stations: List<RadioStation> = emptyList(),
    val index: Int = -1,
    val status: RadioStatus = RadioStatus.IDLE,
    /** Why the list or the stream failed, for the screen. */
    val message: String = "",
    val listLoading: Boolean = false,
    /** Urls of the stations the driver starred; they come first in [stations]. */
    val favorites: Set<String> = emptySet(),
) {
    val current: RadioStation? get() = stations.getOrNull(index)
    val currentIsFavorite: Boolean get() = current?.url in favorites
}

/**
 * An internet radio that lives inside the launcher. The head unit's own FM tuner is not open to apps, so the stations
 * come from the public radio-browser.info directory and play with the platform [MediaPlayer]. Main thread only.
 */
object RadioPlayer {
    private const val COUNTRY = "CL"
    private const val PREFS = "radio"
    private const val KEY_LAST = "last"

    private val _state = MutableStateFlow(RadioState())
    val state: StateFlow<RadioState> = _state.asStateFlow()

    val isPlaying: Boolean get() = _state.value.status == RadioStatus.PLAYING

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private var appContext: Context? = null

    private const val KEY_FAVORITES = "favorites"
    private var favList: List<RadioStation> = emptyList()
    private var favoritesRead = false
    private var remote: List<RadioStation> = emptyList()
    private var remoteLoaded = false

    /** Every working MP3 stream of each station name: the main one and the other signals to change to. */
    private var streams: Map<String, List<RadioStation>> = emptyMap()

    /** Loads the favourites at once and the directory once; safe to call whenever the Radio tab opens. */
    fun ensureLoaded(context: Context) {
        appContext = context.applicationContext
        if (!favoritesRead) {
            favoritesRead = true
            favList = readFavorites()
            added = readList(KEY_ADDED)
            hidden = prefs()?.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet()
            rebuild()
        }
        if (remoteLoaded || _state.value.listLoading) return
        reload()
    }

    fun reload() {
        _state.value = _state.value.copy(listLoading = true, message = "")
        scope.launch {
            val all = runCatching { withContext(Dispatchers.IO) { fetchStations() } }.getOrNull().orEmpty()
            val groups = all.groupBy { it.name.lowercase() }
            streams = groups
            remote = groups.values.map { it.first() }.take(24)
            remoteLoaded = remote.isNotEmpty()
            rebuild()
            _state.value = _state.value.copy(
                listLoading = false,
                message = if (_state.value.stations.isEmpty()) "No se pudieron cargar las emisoras. Revisa la conexión a internet." else "",
            )
        }
    }

    /** Stars or unstars the station that is selected; favourites always come first, so they are in view without scrolling. */
    fun toggleFavorite() {
        val cur = _state.value.current ?: return
        favList = if (favList.any { it.url == cur.url }) favList.filter { it.url != cur.url } else favList + cur
        saveFavorites()
        rebuild()
    }

    /** Favourites first, then the directory; the selected station keeps being the selected one. */
    private fun rebuild(keep: String? = null) {
        val favUrls = favList.map { it.url }.toSet()
        val extra = added.filter { it.url !in favUrls }
        val taken = favUrls + extra.map { it.url }
        val merged = favList + extra + remote.filter { it.url !in taken && it.name.lowercase() !in hidden }
        val target = keep ?: _state.value.current?.url
            ?: appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.getString(KEY_LAST, null)
        val index = merged.indexOfFirst { it.url == target }.takeIf { it >= 0 } ?: if (merged.isEmpty()) -1 else 0
        _state.value = _state.value.copy(stations = merged, index = index, favorites = favUrls)
    }

    private const val KEY_ADDED = "added"
    private const val KEY_HIDDEN = "hidden"
    private var added: List<RadioStation> = emptyList()
    private var hidden: Set<String> = emptySet()

    private fun prefs() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun readList(key: String): List<RadioStation> {
        val raw = prefs()?.getString(key, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                RadioStation(o.getString("name"), o.getString("url"), o.optString("codec"), o.optInt("bitrate"), o.optString("tags"))
            }
        }.getOrDefault(emptyList())
    }

    private fun writeList(key: String, list: List<RadioStation>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(org.json.JSONObject().put("name", it.name).put("url", it.url).put("codec", it.codec).put("bitrate", it.bitrate).put("tags", it.tags))
        }
        prefs()?.edit()?.putString(key, arr.toString())?.apply()
    }

    private fun readFavorites() = readList(KEY_FAVORITES)
    private fun saveFavorites() = writeList(KEY_FAVORITES, favList)

    /** Searches the public directory by name (any country, MP3 only); [onDone] runs on the main thread, null on failure. */
    fun search(query: String, onDone: (List<RadioStation>?) -> Unit) {
        scope.launch {
            val found = runCatching {
                withContext(Dispatchers.IO) {
                    fetch("name=${java.net.URLEncoder.encode(query.trim(), "UTF-8")}&order=clickcount&reverse=true&limit=40&hidebroken=true")
                }
            }.getOrNull()
            onDone(found?.distinctBy { it.name.lowercase() })
        }
    }

    /** Puts a station from the search into the list (right after the favourites) and selects it. */
    fun addStation(station: RadioStation) {
        added = added.filter { it.url != station.url } + station
        hidden = hidden - station.name.lowercase()
        prefs()?.edit()?.putStringSet(KEY_HIDDEN, hidden)?.apply()
        writeList(KEY_ADDED, added)
        rebuild(keep = station.url)
    }

    /** Takes the selected station out of the list for good (it stops if it was playing). */
    fun removeCurrent() {
        val cur = _state.value.current ?: return
        val k = cur.name.lowercase()
        if (_state.value.status != RadioStatus.IDLE) stop()
        favList = favList.filter { it.name.lowercase() != k }
        added = added.filter { it.name.lowercase() != k }
        hidden = hidden + k
        saveFavorites()
        writeList(KEY_ADDED, added)
        prefs()?.edit()?.putStringSet(KEY_HIDDEN, hidden)?.apply()
        _state.value = _state.value.copy(index = -1)
        rebuild()
    }

    private fun key(s: RadioStation) = s.name.lowercase()

    /** How many signals the station has, and which one is on now (1-based), for the screen. */
    fun signalCount(s: RadioStation?): Int = s?.let { streams[key(it)]?.size } ?: 1
    fun signalNumber(s: RadioStation?): Int =
        s?.let { st -> (streams[key(st)]?.indexOfFirst { it.url == st.url } ?: 0).coerceAtLeast(0) + 1 } ?: 1

    /** The station sounds bad: tries its next signal (another server or quality), looking for more online if it has none. */
    fun changeSignal() {
        val cur = _state.value.current ?: return
        val known = streams[key(cur)].orEmpty()
        if (known.size >= 2) {
            switchSignal(cur, known)
            return
        }
        _state.value = _state.value.copy(message = "Buscando otra señal…")
        scope.launch {
            val found = runCatching { withContext(Dispatchers.IO) { fetchByName(cur.name) } }.getOrNull().orEmpty()
            val all = (listOf(cur) + found).distinctBy { it.url }
            if (all.size < 2) {
                _state.value = _state.value.copy(message = "Esta emisora no tiene otra señal.")
                return@launch
            }
            streams = streams + (key(cur) to all)
            switchSignal(cur, all)
        }
    }

    private fun switchSignal(cur: RadioStation, options: List<RadioStation>) {
        val at = options.indexOfFirst { it.url == cur.url }.coerceAtLeast(0)
        val next = options[(at + 1) % options.size]
        // The favourite and the list keep the signal that was chosen.
        favList = favList.map { if (it.url == cur.url) next else it }
        remote = remote.map { if (it.url == cur.url) next else it }
        saveFavorites()
        rebuild(keep = next.url)
        play(_state.value.stations.indexOfFirst { it.url == next.url })
    }

    fun play(index: Int) {
        val station = _state.value.stations.getOrNull(index) ?: return
        val ctx = appContext ?: return
        release()
        BluetoothMusic.pauseMusic()
        _state.value = _state.value.copy(index = index, status = RadioStatus.LOADING, message = "")
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LAST, station.url).apply()
        requestFocus(ctx)
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            mp.setDataSource(station.url)
            mp.setOnPreparedListener {
                if (player === it) {
                    it.start()
                    _state.value = _state.value.copy(status = RadioStatus.PLAYING)
                }
            }
            mp.setOnErrorListener { p, _, _ ->
                if (player === p) fail("Esta emisora no responde. Prueba otra.")
                true
            }
            mp.prepareAsync()
            // A stream that never answers must not stay on "Conectando" forever.
            scope.launch {
                kotlinx.coroutines.delay(15_000)
                if (player === mp && _state.value.status == RadioStatus.LOADING) fail("La emisora tarda demasiado. Prueba otra.")
            }
        } catch (e: Exception) {
            fail("No se pudo abrir la emisora.")
        }
    }

    fun toggle() {
        val s = _state.value
        when (s.status) {
            RadioStatus.PLAYING, RadioStatus.LOADING -> stop()
            else -> play(if (s.index >= 0) s.index else 0)
        }
    }

    fun next() = step(1)
    fun previous() = step(-1)

    fun stop() {
        release()
        abandonFocus()
        _state.value = _state.value.copy(status = RadioStatus.IDLE)
    }

    private fun step(delta: Int) {
        val s = _state.value
        if (s.stations.isEmpty()) return
        play(((if (s.index < 0) 0 else s.index) + delta + s.stations.size) % s.stations.size)
    }

    private fun fail(message: String) {
        release()
        abandonFocus()
        _state.value = _state.value.copy(status = RadioStatus.ERROR, message = message)
    }

    private fun release() {
        player?.let { runCatching { it.setOnPreparedListener(null); it.setOnErrorListener(null); it.release() } }
        player = null
    }

    private fun requestFocus(context: Context) {
        // Asking again while holding it would tell the first request it lost the focus, and stop the new station.
        if (focus != null) return
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener { change ->
                // Another app took the audio for good: the radio steps aside instead of fighting it.
                if (change == AudioManager.AUDIOFOCUS_LOSS) stop()
            }
            .build()
        focus = request
        audio.requestAudioFocus(request)
    }

    private fun abandonFocus() {
        val ctx = appContext ?: return
        focus?.let { (ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(it) }
        focus = null
    }

    private fun fetchStations(): List<RadioStation> =
        fetch("countrycode=$COUNTRY&order=clickcount&reverse=true&limit=80&hidebroken=true")

    private fun fetchByName(name: String): List<RadioStation> =
        fetch("name=${java.net.URLEncoder.encode(name, "UTF-8")}&name_exact=true&order=clickcount&reverse=true&limit=10&hidebroken=true")

    private fun fetch(query: String): List<RadioStation> {
        val conn = URL("https://all.api.radio-browser.info/json/stations/search?$query").openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "RadioLauncher/1.0")
        val body = try { conn.inputStream.bufferedReader().use { it.readText() } } finally { conn.disconnect() }
        val json = JSONArray(body)
        val list = ArrayList<RadioStation>()
        for (i in 0 until json.length()) {
            val o = json.getJSONObject(i)
            // Only MP3: the platform MediaPlayer stalls on live AAC+ (tried: it stayed on "connecting"), and FLAC / OGG are heavy.
            if (!o.optString("codec").equals("MP3", true)) continue
            val link = o.optString("url_resolved").ifEmpty { o.optString("url") }
            if (link.isEmpty()) continue
            list += RadioStation(o.optString("name").trim(), link, "MP3", o.optInt("bitrate"), o.optString("tags").split(',').take(2).joinToString(" · ").trim())
        }
        return list.distinctBy { it.url }
    }
}
