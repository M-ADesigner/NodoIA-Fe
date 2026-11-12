package com.radioauto.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the driver chose for the sound. Levels are in millibels, strengths in 0..1000, as Android takes them. */
data class FxSettings(
    val on: Boolean = false,
    /** One level per fader of the graphic equalizer ([SoundFx.GRAPHIC_HZ]); empty means flat. */
    val bands: List<Int> = emptyList(),
    val preset: String = "Normal",
    val bass: Int = 0,
    val virtualizer: Int = 0,
    /** Extra loudness in millibels (0..1500). */
    val loudness: Int = 0,
    /** Index into [SoundFx.REVERBS]; 0 is none. */
    val reverb: Int = 0,
)

/** What this device's audio system can do; read once from the effects themselves. */
data class FxCaps(
    val probed: Boolean = false,
    val bands: Int = 0,
    val minLevel: Int = 0,
    val maxLevel: Int = 0,
    val freqsHz: List<Int> = emptyList(),
    val bass: Boolean = false,
    val virtualizer: Boolean = false,
    val loudness: Boolean = false,
    val reverb: Boolean = false,
) {
    val equalizer: Boolean get() = bands > 0
    val any: Boolean get() = equalizer || bass || virtualizer || loudness || reverb
}

/**
 * Sound effects, attached to the audio session of each player that announces itself (Spotify, YouTube Music and most
 * players do, with the standard "open audio effect control session" broadcast), and also to the whole output (session
 * 0) where the system still allows it, which older head-unit Android versions do and Android 10+ does not.
 */
object SoundFx {
    val REVERBS = listOf("Ninguna", "Cuarto chico", "Cuarto mediano", "Cuarto grande", "Sala mediana", "Sala grande", "Placa")
    private val REVERB_IDS = listOf(
        PresetReverb.PRESET_NONE, PresetReverb.PRESET_SMALLROOM, PresetReverb.PRESET_MEDIUMROOM,
        PresetReverb.PRESET_LARGEROOM, PresetReverb.PRESET_MEDIUMHALL, PresetReverb.PRESET_LARGEHALL, PresetReverb.PRESET_PLATE,
    )

    /** The faders of the graphic equalizer, bass to treble. The device only has a few real bands; they are blended in. */
    val GRAPHIC_HZ = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

    /** Shapes from bass to treble, -1..1 of the band range; stretched over the faders. */
    val PRESETS: Map<String, List<Float>> = linkedMapOf(
        "Normal" to listOf(0f, 0f, 0f, 0f, 0f),
        "Rock" to listOf(0.6f, 0.3f, -0.2f, 0.3f, 0.6f),
        "Pop" to listOf(-0.2f, 0.3f, 0.5f, 0.3f, -0.2f),
        "Electrónica" to listOf(0.7f, 0.3f, 0f, 0.4f, 0.6f),
        "Jazz" to listOf(0.4f, 0.2f, -0.2f, 0.2f, 0.5f),
        "Clásica" to listOf(0.4f, 0.3f, -0.1f, 0.3f, 0.4f),
        "Bajos" to listOf(0.9f, 0.6f, 0f, 0f, 0f),
        "Agudos" to listOf(0f, 0f, 0f, 0.5f, 0.9f),
        "Voces" to listOf(-0.4f, -0.1f, 0.6f, 0.5f, 0.1f),
        "Hip hop" to listOf(0.8f, 0.5f, -0.1f, 0.2f, 0.3f),
    )

    private val _caps = MutableStateFlow(FxCaps())
    val caps: StateFlow<FxCaps> = _caps.asStateFlow()

    private var current = FxSettings()
    private val chains = HashMap<Int, Chain>()

    /** The effects of one audio session. */
    private class Chain {
        var eq: Equalizer? = null
        var bass: BassBoost? = null
        var virt: Virtualizer? = null
        var loud: LoudnessEnhancer? = null
        var rev: PresetReverb? = null

        fun release() {
            runCatching { eq?.release() }; eq = null
            runCatching { bass?.release() }; bass = null
            runCatching { virt?.release() }; virt = null
            runCatching { loud?.release() }; loud = null
            runCatching { rev?.release() }; rev = null
        }
    }

    /** Fader levels for a preset, in millibels. */
    fun levelsFor(preset: String, caps: FxCaps): List<Int> {
        val n = GRAPHIC_HZ.size
        val shape = PRESETS[preset] ?: return List(n) { 0 }
        return List(n) { i ->
            val pos = i * (shape.size - 1) / (n - 1f)
            val lo = pos.toInt().coerceIn(0, shape.size - 1)
            val hi = (lo + 1).coerceAtMost(shape.size - 1)
            val v = shape[lo] + (shape[hi] - shape[lo]) * (pos - lo)
            (if (v >= 0) v * caps.maxLevel else -v * caps.minLevel).toInt().coerceIn(caps.minLevel, caps.maxLevel)
        }
    }

    /** The faders as the [GRAPHIC_HZ] list, flat when nothing valid is saved. */
    fun faders(fx: FxSettings): List<Int> = fx.bands.takeIf { it.size == GRAPHIC_HZ.size } ?: List(GRAPHIC_HZ.size) { 0 }

    /** Level of the device band centred on [hz]: the faders around it blended on a frequency (log) scale. */
    fun bandLevel(faders: List<Int>, hz: Int): Int {
        val x = kotlin.math.log2(hz.coerceAtLeast(1).toFloat())
        val xs = GRAPHIC_HZ.map { kotlin.math.log2(it.toFloat()) }
        if (x <= xs.first()) return faders.first()
        if (x >= xs.last()) return faders.last()
        val hi = xs.indexOfFirst { it >= x }
        val lo = hi - 1
        val t = (x - xs[lo]) / (xs[hi] - xs[lo])
        return (faders[lo] + (faders[hi] - faders[lo]) * t).toInt()
    }

    /** Reads what the device offers, on a throwaway session of our own (always allowed). Safe to call many times. */
    fun probe(context: Context) {
        if (_caps.value.probed) return
        val session = (context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager).generateAudioSessionId()
        var c = FxCaps(probed = true)
        runCatching {
            val e = Equalizer(0, session)
            val range = e.bandLevelRange
            c = c.copy(
                bands = e.numberOfBands.toInt(), minLevel = range[0].toInt(), maxLevel = range[1].toInt(),
                freqsHz = List(e.numberOfBands.toInt()) { e.getCenterFreq(it.toShort()) / 1000 },
            )
            e.release()
        }
        c = c.copy(
            bass = runCatching { BassBoost(0, session).also { it.release() } }.isSuccess,
            virtualizer = runCatching { Virtualizer(0, session).also { it.release() } }.isSuccess,
            loudness = runCatching { LoudnessEnhancer(session).also { it.release() } }.isSuccess,
            reverb = runCatching { PresetReverb(0, session).also { it.release() } }.isSuccess,
        )
        _caps.value = c
    }

    /** Applies the settings to every session we hold, and tries the whole output as well. */
    fun apply(context: Context, fx: FxSettings) {
        current = fx
        probe(context)
        if (!fx.on) {
            chains.values.forEach { it.release() }
            chains.clear()
            return
        }
        open(context, 0, fx)
        chains.forEach { (_, chain) -> fill(chain, fx, _caps.value, 0) }
    }

    /** A player announced its audio session; it gets the current effects. */
    fun open(context: Context, session: Int, fx: FxSettings = current) {
        probe(context)
        if (!fx.on) return
        val chain = chains.getOrPut(session) { Chain() }
        fill(chain, fx, _caps.value, session)
        // Session 0 refused on Android 10+: do not keep an empty chain around.
        if (session == 0 && chain.eq == null && chain.bass == null && chain.virt == null && chain.loud == null && chain.rev == null) chains.remove(0)
    }

    fun close(session: Int) {
        chains.remove(session)?.release()
    }

    private fun fill(chain: Chain, fx: FxSettings, c: FxCaps, session: Int) {
        if (c.equalizer) runCatching {
            val e = chain.eq ?: Equalizer(0, session).also { chain.eq = it }
            val faders = faders(fx)
            for (i in 0 until c.bands) {
                val l = bandLevel(faders, c.freqsHz.getOrNull(i) ?: 1000)
                e.setBandLevel(i.toShort(), l.coerceIn(c.minLevel, c.maxLevel).toShort())
            }
            e.enabled = true
        }
        if (c.bass) runCatching {
            val b = chain.bass ?: BassBoost(0, session).also { chain.bass = it }
            if (b.strengthSupported) b.setStrength(fx.bass.coerceIn(0, 1000).toShort())
            b.enabled = fx.bass > 0
        }
        if (c.virtualizer) runCatching {
            val v = chain.virt ?: Virtualizer(0, session).also { chain.virt = it }
            if (v.strengthSupported) v.setStrength(fx.virtualizer.coerceIn(0, 1000).toShort())
            v.enabled = fx.virtualizer > 0
        }
        if (c.loudness) runCatching {
            val l = chain.loud ?: LoudnessEnhancer(session).also { chain.loud = it }
            l.setTargetGain(fx.loudness.coerceIn(0, 1500))
            l.enabled = fx.loudness > 0
        }
        if (c.reverb) runCatching {
            val r = chain.rev ?: PresetReverb(0, session).also { chain.rev = it }
            r.preset = REVERB_IDS[fx.reverb.coerceIn(0, REVERB_IDS.size - 1)]
            r.enabled = fx.reverb > 0
        }
    }
}

/** Receives the players' "my audio session is X" announcements, so the effects follow whatever is playing. */
class AudioSessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val session = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)
        if (session == AudioEffect.ERROR || session <= 0) return
        if (intent.action == AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION) {
            SoundFx.close(session)
            return
        }
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                SoundFx.open(app, session, Prefs(app).current().fx)
            } finally {
                pending.finish()
            }
        }
    }
}
