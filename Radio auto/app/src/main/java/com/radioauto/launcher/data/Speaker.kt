package com.radioauto.launcher.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
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
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        ready = true
        pending?.let { speak(engine, it) }
        pending = null
    }

    private fun speak(engine: TextToSpeech, text: String) {
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alert")
    }

    private fun beep() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
        }
    }
}
