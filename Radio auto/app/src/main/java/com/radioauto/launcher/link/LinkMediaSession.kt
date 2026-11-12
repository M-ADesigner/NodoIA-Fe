package com.radioauto.launcher.link

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import com.radioauto.launcher.data.BluetoothMusic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Publishes the music of a phone joined through the link as a system media session. That is what makes Android hand
 * the steering wheel buttons (next, previous, play) to the launcher even when another app, such as the map, is in
 * front. The session is only active while the phone's track is playing, so it never competes with other players.
 */
object LinkMediaSession {
    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun start(context: Context) {
        if (session != null) return
        val app = context.applicationContext
        val s = MediaSession(app, "RadioLauncherLink").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = BluetoothMusic.playPause(app)
                override fun onPause() = BluetoothMusic.playPause(app)
                override fun onSkipToNext() = BluetoothMusic.next(app)
                override fun onSkipToPrevious() = BluetoothMusic.previous(app)
            })
        }
        session = s
        scope.launch {
            LinkServer.state.map { it.track }.distinctUntilChanged { a, b -> a?.title == b?.title && a?.artist == b?.artist && a?.playing == b?.playing }
                .collect { track ->
                    if (track == null) {
                        s.isActive = false
                        return@collect
                    }
                    s.setMetadata(
                        MediaMetadata.Builder()
                            .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
                            .build(),
                    )
                    val actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                    s.setPlaybackState(
                        PlaybackState.Builder()
                            .setActions(actions)
                            .setState(if (track.playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, 0, 1f)
                            .build(),
                    )
                    s.isActive = true
                }
        }
    }
}
