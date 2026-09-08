/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.videoplayer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.wire.android.di.ApplicationContext
import com.wire.android.di.metro.WireAssistedViewModelBinding
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/**
 * Plays a single video from either a local file ([localPath]) or a remote URL ([contentUrl]).
 *
 * The screen arguments are passed in through the per-ViewModel assisted [Factory]
 * rather than read from a navigation destination, so the player can be reused from any module.
 */
@WireAssistedViewModelBinding(VideoPlayerManualViewModelFactoryGroup::class)
class VideoPlayerViewModel @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted val localPath: String?,
    @Assisted val contentUrl: String?,
    @Assisted val fileName: String?,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(localPath: String?, contentUrl: String?, fileName: String?): VideoPlayerViewModel
    }

    // Held in the ViewModel so playback survives configuration changes (e.g. rotating to full screen)
    // without re-buffering the media.
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _state = MutableStateFlow(
        VideoPlaybackState(
            isPlaying = player.isPlaying,
            isStarted = player.currentPosition > 0L || player.isPlaying,
            isCompleted = player.playbackState == Player.STATE_ENDED,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            isMuted = player.volume == 0f,
            currentPositionMs = player.currentPosition.toInt(),
            durationMs = player.duration.coerceAtLeast(0).toInt(),
        )
    )
    val state: StateFlow<VideoPlaybackState> = _state.asStateFlow()

    private var positionPollJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _state.update { it.copy(isPlaying = playing) }
            if (playing) startPositionPolling() else stopPositionPolling()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(isBuffering = playbackState == Player.STATE_BUFFERING) }
            when (playbackState) {
                Player.STATE_READY -> _state.update {
                    it.copy(durationMs = player.duration.coerceAtLeast(0).toInt())
                }

                Player.STATE_ENDED -> _state.update { it.copy(isCompleted = true) }
            }
        }
    }

    init {
        player.addListener(playerListener)
        videoUri()?.let {
            player.setMediaItem(MediaItem.fromUri(it))
            player.prepare()
        }
        if (player.isPlaying) startPositionPolling()
    }

    fun play() {
        player.play()
        _state.update { it.copy(isStarted = true, isCompleted = false) }
    }

    fun pause() {
        player.pause()
    }

    fun replay() {
        player.seekTo(0)
        play()
    }

    fun togglePlayPause() {
        val current = _state.value
        when {
            current.isCompleted -> replay()
            current.isPlaying -> pause()
            else -> play()
        }
    }

    fun toggleMute() {
        val muted = !_state.value.isMuted
        player.volume = if (muted) 0f else 1f
        _state.update { it.copy(isMuted = muted) }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _state.update { it.copy(currentPositionMs = positionMs.toInt()) }
    }

    private fun startPositionPolling() {
        if (positionPollJob?.isActive == true) return
        positionPollJob = viewModelScope.launch {
            while (isActive) {
                _state.update {
                    it.copy(
                        currentPositionMs = player.currentPosition.toInt(),
                        durationMs = player.duration.coerceAtLeast(0).toInt(),
                    )
                }
                delay(POSITION_POLL_MS.milliseconds)
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollJob?.cancel()
        positionPollJob = null
    }

    private fun videoUri(): Uri? = when {
        localPath != null -> Uri.fromFile(File(localPath))
        contentUrl != null -> Uri.parse(contentUrl)
        else -> null
    }

    override fun onCleared() {
        super.onCleared()
        stopPositionPolling()
        player.removeListener(playerListener)
        player.release()
    }

    private companion object {
        const val POSITION_POLL_MS = 200L
    }
}
