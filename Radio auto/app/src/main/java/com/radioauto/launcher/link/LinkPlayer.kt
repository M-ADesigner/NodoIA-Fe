package com.radioauto.launcher.link

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build

/** Plays the PCM stream of the phone that has the music on the radio's speakers. */
class LinkPlayer(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change -> onFocusChange?.invoke(change) }
    private val focusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
        } else null

    /** Called when another app takes or gives back the audio, so the phone's music can be paused. */
    @Volatile var onFocusChange: ((Int) -> Unit)? = null

    private var track: AudioTrack? = null
    private var sourceId: String? = null
    private var gain = 1f

    /** Music volume chosen from the phone, on top of the radio's own volume. */
    @Synchronized
    fun setGain(value: Float) {
        gain = value.coerceIn(0f, 1f)
        track?.setVolume(gain)
    }

    /** Blocks while the track buffer is full, which paces the sender through back-pressure. */
    @Synchronized
    fun write(fromId: String, pcm: ByteArray) {
        if (fromId != sourceId) {
            // A different phone took over: drop what is left of the previous one
            track?.run { pause(); flush() }
            sourceId = fromId
        }
        val current = track ?: createTrack().also {
            it.setVolume(gain)
            track = it
            requestFocus()
        }
        if (current.playState != AudioTrack.PLAYSTATE_PLAYING) current.play()
        current.write(pcm, 0, pcm.size)
    }

    /** Holds the music without letting go of the audio, so it is heard again when the other app gives it back. */
    @Synchronized
    fun pause() {
        track?.run {
            pause()
            flush()
        }
    }

    /** Silences the radio when no phone is playing anymore. */
    @Synchronized
    fun stop() {
        track?.run {
            pause()
            flush()
            release()
        }
        track = null
        sourceId = null
        abandonFocus()
    }

    private fun requestFocus() {
        if (focusRequest != null) {
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun abandonFocus() {
        if (focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }

    private fun createTrack(): AudioTrack {
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(AudioSpec.SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        val minBuffer = AudioTrack.getMinBufferSize(
            AudioSpec.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        // ~200 ms absorbs Wi-Fi jitter without a noticeable delay
        val buffer = maxOf(minBuffer, AudioSpec.SAMPLE_RATE / 5 * AudioSpec.BYTES_PER_FRAME)
        return AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(buffer)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }
}
