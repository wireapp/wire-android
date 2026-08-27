/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.media.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AudioState(
    val audioMediaPlayingState: AudioMediaPlayingState,
    val currentPositionInMs: Int,
    val totalTimeInMs: TotalTimeInMs,
) {
    fun sanitizeTotalTime(otherClientTotalTime: Int): TotalTimeInMs =
        if (otherClientTotalTime != 0) TotalTimeInMs.Known(otherClientTotalTime) else totalTimeInMs

    fun isPlaying(): Boolean = audioMediaPlayingState is AudioMediaPlayingState.Playing

    fun isPlayingOrPaused(): Boolean = audioMediaPlayingState is AudioMediaPlayingState.Playing ||
        audioMediaPlayingState is AudioMediaPlayingState.Paused

    fun isPlayingOrPausedOrFetching(): Boolean = isPlayingOrPaused() ||
        audioMediaPlayingState is AudioMediaPlayingState.Fetching ||
        audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching

    sealed interface TotalTimeInMs {
        data object NotKnown : TotalTimeInMs
        data class Known(val value: Int) : TotalTimeInMs
    }

    companion object {
        val DEFAULT = AudioState(AudioMediaPlayingState.Stopped, 0, TotalTimeInMs.NotKnown)
    }
}

sealed interface AudioMediaPlayingState {
    data object Playing : AudioMediaPlayingState
    data object Stopped : AudioMediaPlayingState
    data object Completed : AudioMediaPlayingState
    data object Paused : AudioMediaPlayingState
    data object Fetching : AudioMediaPlayingState
    data object SuccessfulFetching : AudioMediaPlayingState
    data object Failed : AudioMediaPlayingState
}

sealed interface AudioStateUpdate {
    data class PlayingStateChanged(val state: AudioMediaPlayingState) : AudioStateUpdate
    data class PositionChanged(val positionMs: Int) : AudioStateUpdate
    data class TotalTimeChanged(val totalTimeMs: Int) : AudioStateUpdate
}

/** Platform-independent state history for independently rendered conversation audio messages. */
class AudioPlaybackStateStore<Key> {
    private val mutableStates = MutableStateFlow<Map<Key, AudioState>>(emptyMap())
    val states: StateFlow<Map<Key, AudioState>> = mutableStates.asStateFlow()

    fun update(key: Key, update: AudioStateUpdate) {
        mutableStates.update { history ->
            val current = history[key] ?: AudioState.DEFAULT
            history + (key to current.reduce(update))
        }
    }

    fun state(key: Key): AudioState? = states.value[key]

    fun restoredPosition(key: Key): Int? = state(key)?.let { state ->
        if (state.audioMediaPlayingState == AudioMediaPlayingState.Completed) 0 else state.currentPositionInMs
    }

    fun clear() {
        mutableStates.value = emptyMap()
    }

    private fun AudioState.reduce(update: AudioStateUpdate): AudioState = when (update) {
        is AudioStateUpdate.PlayingStateChanged -> copy(audioMediaPlayingState = update.state)
        is AudioStateUpdate.PositionChanged -> copy(currentPositionInMs = update.positionMs)
        is AudioStateUpdate.TotalTimeChanged -> copy(totalTimeInMs = AudioState.TotalTimeInMs.Known(update.totalTimeMs))
    }
}
