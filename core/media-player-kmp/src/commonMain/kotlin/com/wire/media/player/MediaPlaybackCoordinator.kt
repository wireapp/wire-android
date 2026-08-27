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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Common audio/video state machine. Native engines only execute commands and report events. */
@Suppress("TooManyFunctions")
class MediaPlaybackCoordinator(
    private val engine: MediaPlaybackEngine,
    private val scope: CoroutineScope,
    private val positionPollIntervalMs: Long = DEFAULT_POSITION_POLL_INTERVAL_MS,
) {
    private val mutableState = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = mutableState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = EVENT_BUFFER_CAPACITY)
    val events: SharedFlow<PlaybackEvent> = mutableEvents.asSharedFlow()

    private var positionPollJob: Job? = null
    private var playWhenReady = false

    init {
        engine.setEventListener(::onEngineEvent)
    }

    fun prepare(
        source: PlaybackSource,
        restoredPositionMs: Int = 0,
        playWhenReady: Boolean = false,
    ): PlaybackCommandResult = dispatch(
        PlaybackCommand.Prepare(source, restoredPositionMs.coerceAtLeast(0), playWhenReady)
    )

    fun play(): PlaybackCommandResult = dispatch(PlaybackCommand.Play)

    fun pause(): PlaybackCommandResult = dispatch(PlaybackCommand.Pause)

    fun stop(): PlaybackCommandResult = dispatch(PlaybackCommand.Stop)

    fun seekTo(positionMs: Int): PlaybackCommandResult = dispatch(
        PlaybackCommand.SeekTo(positionMs.coerceIn(0, state.value.durationMs.coerceAtLeast(positionMs)))
    )

    fun setMuted(muted: Boolean): PlaybackCommandResult = dispatch(PlaybackCommand.SetMuted(muted))

    fun toggleMute(): PlaybackCommandResult = setMuted(!state.value.isMuted)

    fun setSpeed(speed: PlaybackSpeed): PlaybackCommandResult = dispatch(PlaybackCommand.SetSpeed(speed))

    fun replay(): PlaybackCommandResult {
        val seekResult = seekTo(0)
        return if (seekResult is PlaybackCommandResult.Failure) seekResult else play()
    }

    fun togglePlayPause(): PlaybackCommandResult = when {
        state.value.isCompleted -> replay()
        state.value.isPlaying -> pause()
        else -> play()
    }

    fun release(): PlaybackCommandResult = dispatch(PlaybackCommand.Release)

    fun dispatch(command: PlaybackCommand): PlaybackCommandResult {
        if (state.value.isReleased && command != PlaybackCommand.Release) return PlaybackCommandResult.Failure("released")
        if (command is PlaybackCommand.Prepare) {
            playWhenReady = command.playWhenReady
            onEngineEvent(PlaybackEvent.Preparing(command.restoredPositionMs))
        }

        val result = engine.execute(command)
        when (result) {
            PlaybackCommandResult.Executed -> applyOptimisticEvent(command)
            PlaybackCommandResult.Unsupported -> Unit
            is PlaybackCommandResult.Failure -> onEngineEvent(PlaybackEvent.Failed(result.reason))
        }
        return result
    }

    private fun applyOptimisticEvent(command: PlaybackCommand) {
        when (command) {
            PlaybackCommand.Play -> onEngineEvent(PlaybackEvent.Playing)
            PlaybackCommand.Pause -> onEngineEvent(PlaybackEvent.Paused)
            PlaybackCommand.Stop -> onEngineEvent(PlaybackEvent.Stopped)
            is PlaybackCommand.SeekTo -> onEngineEvent(
                PlaybackEvent.PositionChanged(command.positionMs, state.value.durationMs)
            )
            is PlaybackCommand.SetMuted -> onEngineEvent(PlaybackEvent.MutedChanged(command.muted))
            is PlaybackCommand.SetSpeed -> onEngineEvent(PlaybackEvent.SpeedChanged(command.speed))
            PlaybackCommand.Release -> onEngineEvent(PlaybackEvent.Released)
            is PlaybackCommand.Prepare -> Unit
        }
    }

    private fun onEngineEvent(event: PlaybackEvent) {
        val updatedState = mutableState.value.reduce(event)
        if (updatedState == mutableState.value) return
        mutableState.value = updatedState
        mutableEvents.tryEmit(event)
        when (event) {
            PlaybackEvent.Playing -> startPositionPolling()
            PlaybackEvent.Paused,
            PlaybackEvent.Stopped,
            PlaybackEvent.Completed,
            PlaybackEvent.Released,
            is PlaybackEvent.Failed -> stopPositionPolling()
            is PlaybackEvent.Ready -> {
                if (state.value.speed != PlaybackSpeed.NORMAL) setSpeed(state.value.speed)
                if (state.value.isMuted) setMuted(true)
                if (playWhenReady) {
                    playWhenReady = false
                    play()
                }
            }
            else -> Unit
        }
    }

    private fun startPositionPolling() {
        if (positionPollJob?.isActive == true) return
        positionPollJob = scope.launch {
            while (isActive) {
                engine.snapshot()?.let { snapshot ->
                    onEngineEvent(
                        PlaybackEvent.PositionChanged(snapshot.currentPositionMs, snapshot.durationMs)
                    )
                }
                delay(positionPollIntervalMs)
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollJob?.cancel()
        positionPollJob = null
    }

    private fun PlaybackState.reduce(event: PlaybackEvent): PlaybackState = when (event) {
        is PlaybackEvent.Preparing -> copy(
            isPrepared = false,
            isPlaying = false,
            isStarted = event.restoredPositionMs > 0,
            isCompleted = false,
            isBuffering = true,
            currentPositionMs = event.restoredPositionMs,
            failureReason = null,
        )
        is PlaybackEvent.Ready -> copy(
            isPrepared = true,
            isBuffering = false,
            durationMs = event.durationMs.coerceAtLeast(0),
        )
        is PlaybackEvent.Buffering -> copy(isBuffering = event.buffering)
        PlaybackEvent.Playing -> copy(isPlaying = true, isStarted = true, isCompleted = false)
        PlaybackEvent.Paused -> copy(isPlaying = false)
        PlaybackEvent.Stopped -> copy(isPlaying = false, isStarted = false, isCompleted = false, currentPositionMs = 0)
        is PlaybackEvent.PositionChanged -> copy(
            currentPositionMs = event.positionMs.coerceAtLeast(0),
            durationMs = event.durationMs.coerceAtLeast(0),
        )
        PlaybackEvent.Completed -> copy(
            isPlaying = false,
            isStarted = true,
            isCompleted = true,
            currentPositionMs = durationMs,
        )
        is PlaybackEvent.MutedChanged -> copy(isMuted = event.muted)
        is PlaybackEvent.SpeedChanged -> copy(speed = event.speed)
        is PlaybackEvent.Failed -> copy(isPlaying = false, isBuffering = false, failureReason = event.reason)
        PlaybackEvent.Released -> copy(isPlaying = false, isBuffering = false, isReleased = true)
    }

    private companion object {
        const val DEFAULT_POSITION_POLL_INTERVAL_MS = 200L
        const val EVENT_BUFFER_CAPACITY = 32
    }
}
