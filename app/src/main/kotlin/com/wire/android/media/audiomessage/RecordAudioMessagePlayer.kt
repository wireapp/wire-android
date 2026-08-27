/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.media.audiomessage

import com.wire.android.di.ApplicationScope
import com.wire.android.mediaplayer.AndroidMediaPlayerPlaybackEngineFactory
import com.wire.media.player.AudioMediaPlayingState
import com.wire.media.player.AudioState
import com.wire.media.player.MediaPlaybackCoordinator
import com.wire.media.player.PlaybackCommandResult
import com.wire.media.player.PlaybackEvent
import com.wire.media.player.PlaybackSource
import com.wire.media.recording.RecordingCapabilityResult
import com.wire.media.recording.RecordingPreview
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okio.Path

@Inject
@ContributesBinding(AppScope::class, binding = binding<RecordingPreview>())
class RecordAudioMessagePlayer(
    engineFactory: AndroidMediaPlayerPlaybackEngineFactory,
    private val audioFocusHelper: AudioFocusHelper,
    @ApplicationScope private val scope: CoroutineScope,
) : RecordingPreview {
    private val coordinator = MediaPlaybackCoordinator(
        engine = engineFactory.create(),
        scope = scope,
        positionPollIntervalMs = UPDATE_POSITION_INTERVAL_IN_MS,
    )
    private val mutableState = MutableStateFlow(AudioState.DEFAULT)
    override val state: StateFlow<AudioState> = mutableState.asStateFlow()
    private var currentAudioFile: Path? = null
    private val playbackEventsJob: Job

    init {
        playbackEventsJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            coordinator.events.collect(::handlePlaybackEvent)
        }
        audioFocusHelper.setListener(
            onPauseCurrentAudio = { pause() },
            onResumeCurrentAudio = { resume() },
        )
    }

    override fun toggle(path: Path): RecordingCapabilityResult<Unit> {
        val result = if (currentAudioFile == path) {
            if (coordinator.state.value.isPlaying) {
                audioFocusHelper.abandon()
                coordinator.pause()
            } else if (audioFocusHelper.request()) {
                coordinator.play()
            } else {
                PlaybackCommandResult.Failure("audio_focus_denied")
            }
        } else {
            coordinator.stop()
            currentAudioFile = path
            if (audioFocusHelper.request()) {
                coordinator.prepare(PlaybackSource.Local(path), playWhenReady = true)
            } else {
                currentAudioFile = null
                PlaybackCommandResult.Failure("audio_focus_denied")
            }
        }
        return result.asRecordingResult()
    }

    override fun seekTo(positionMs: Int): RecordingCapabilityResult<Unit> =
        if (currentAudioFile == null) {
            RecordingCapabilityResult.Failure("preview_unavailable")
        } else {
            coordinator.seekTo(positionMs).asRecordingResult()
        }

    override fun stop(): RecordingCapabilityResult<Unit> {
        currentAudioFile = null
        audioFocusHelper.abandon()
        val result = coordinator.stop()
        mutableState.value = AudioState.DEFAULT
        return result.asRecordingResult()
    }

    override fun release() {
        currentAudioFile = null
        audioFocusHelper.abandon()
        coordinator.release()
        playbackEventsJob.cancel()
        mutableState.value = AudioState.DEFAULT
    }

    private fun pause() {
        if (currentAudioFile != null) coordinator.pause()
    }

    private fun resume() {
        if (currentAudioFile != null) coordinator.play()
    }

    private fun handlePlaybackEvent(event: PlaybackEvent) {
        mutableState.value = when (event) {
            is PlaybackEvent.Ready -> mutableState.value.copy(
                totalTimeInMs = AudioState.TotalTimeInMs.Known(event.durationMs)
            )
            PlaybackEvent.Playing -> mutableState.value.copy(
                audioMediaPlayingState = AudioMediaPlayingState.Playing
            )
            PlaybackEvent.Paused -> mutableState.value.copy(
                audioMediaPlayingState = AudioMediaPlayingState.Paused
            )
            is PlaybackEvent.PositionChanged -> mutableState.value.copy(
                currentPositionInMs = event.positionMs,
                totalTimeInMs = AudioState.TotalTimeInMs.Known(event.durationMs),
            )
            PlaybackEvent.Completed -> {
                audioFocusHelper.abandon()
                mutableState.value.copy(
                    audioMediaPlayingState = AudioMediaPlayingState.Completed,
                    currentPositionInMs = 0,
                )
            }
            PlaybackEvent.Stopped -> AudioState.DEFAULT
            is PlaybackEvent.Failed -> {
                audioFocusHelper.abandon()
                mutableState.value.copy(audioMediaPlayingState = AudioMediaPlayingState.Failed)
            }
            else -> mutableState.value
        }
    }

    private fun PlaybackCommandResult.asRecordingResult(): RecordingCapabilityResult<Unit> = when (this) {
        PlaybackCommandResult.Executed -> RecordingCapabilityResult.Success(Unit)
        PlaybackCommandResult.Unsupported -> RecordingCapabilityResult.Unsupported
        is PlaybackCommandResult.Failure -> RecordingCapabilityResult.Failure(reason)
    }

    private companion object {
        const val UPDATE_POSITION_INTERVAL_IN_MS = 100L
    }
}
