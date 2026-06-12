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
package com.wire.android.feature.cells.ui.audioplayer

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.generated.cells.destinations.CellAudioPlayerScreenDestination
import com.wire.android.di.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CellAudioPlayerViewModel(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val navArgs = CellAudioPlayerScreenDestination.argsFrom(savedStateHandle)
    val localPath: String? = navArgs.localPath
    val contentUrl: String? = navArgs.contentUrl
    val fileName: String? = navArgs.fileName

    // Playback state lives here so it survives configuration changes; the screen only renders it and
    // forwards user intents back through the action functions below.
    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private var positionPollJob: Job? = null

    private val mediaPlayer = MediaPlayer().apply {
        setOnPreparedListener { mp ->
            _state.update { it.copy(durationMs = mp.duration, isPrepared = true) }
        }
        setOnCompletionListener {
            stopPositionPolling()
            _state.update { it.copy(isPlaying = false, isCompleted = true) }
        }
    }

    init {
        try {
            when {
                localPath != null -> mediaPlayer.setDataSource(localPath)
                contentUrl != null -> mediaPlayer.setDataSource(context, Uri.parse(contentUrl))
//                else -> return
            }
            mediaPlayer.prepareAsync()
        } catch (_: Exception) {
            // handle silently — file may not exist yet
        }
    }

    fun play() {
        if (!_state.value.isPrepared) return
        mediaPlayer.start()
        _state.update { it.copy(isPlaying = true, isCompleted = false) }
        startPositionPolling()
    }

    fun pause() {
        if (!_state.value.isPlaying) return
        mediaPlayer.pause()
        stopPositionPolling()
        _state.update { it.copy(isPlaying = false) }
    }

    fun togglePlayPause() {
        val current = _state.value
        when {
            current.isCompleted -> {
                seekTo(0)
                play()
            }
            current.isPlaying -> pause()
            else -> play()
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer.seekTo(positionMs)
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    private fun startPositionPolling() {
        if (positionPollJob?.isActive == true) return
        positionPollJob = viewModelScope.launch {
            while (isActive) {
                _state.update { it.copy(currentPositionMs = mediaPlayer.currentPosition) }
                delay(POSITION_POLL_MS)
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollJob?.cancel()
        positionPollJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPositionPolling()
        try {
            mediaPlayer.stop()
        } catch (_: Exception) {
            // ignore — player may not be in a stoppable state
        }
        mediaPlayer.release()
    }

    private companion object {
        const val POSITION_POLL_MS = 200L
    }
}

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val isCompleted: Boolean = false,
    val isPrepared: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
)
