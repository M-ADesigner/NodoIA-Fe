package com.radioauto.launcher.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Speaks alerts in Spanish, ducking the music while it talks. The speech engine is only created on the
 * first alert, so it costs nothing until needed. Some head units ship without a voice, so it falls back to a beep.
 */
class Speaker(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var failed = false
    private var pending: String? = null
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private var focusRequest: AudioFocusRequest? = null

    fun say(text: String) {
        if (failed) return beep()
        val engine = tts
        if (engine == null) {
            pending = text
            tts = TextToSpeech(context.applicationContext) { status -> onInit(status) }
        } else if (ready) {
            speak(engine, text)
        } else {
            pending = text
        }
    }

    fun shutdown() {
        abandonFocus()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun onInit(status: Int) {
        val engine = tts ?: return
        val es = Locale.forLanguageTag("es")
        if (status != TextToSpeech.SUCCESS || engine.setLanguage(es) < TextToSpeech.LANG_AVAILABLE) {
            failed = true
            pending?.let { beep() }
            pending = null
            shutdown()
            return
        }
        engine.setAudioAttributes(attributes)
        // Runs on the engine's thread; abandoning focus is thread-safe and lets the music come back at full volume.
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = abandonFocus()

            @Deprecated("Replaced by onError(String, Int)")
            override fun onError(utteranceId: String?) = abandonFocus()
            override fun onError(utteranceId: String?, errorCode: Int) = abandonFocus()
            override fun onStop(utteranceId: String?, interrupted: Boolean) = abandonFocus()
        })
        ready = true
        pending?.let { speak(engine, it) }
        pending = null
    }

    private fun speak(engine: TextToSpeech, text: String) {
        requestFocus()
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alert") != TextToSpeech.SUCCESS) abandonFocus()
    }

    /** Transient may-duck only: a permanent gain would silence the Bluetooth music for good. */
    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .build()
                .also { focusRequest = it }
            audio.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audio.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audio.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audio.abandonAudioFocus(null)
        }
    }

    private fun beep() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
        }
    }
}
