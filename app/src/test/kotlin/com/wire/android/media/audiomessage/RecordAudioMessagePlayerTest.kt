/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.media.audiomessage

import com.wire.android.mediaplayer.AndroidMediaPlayerPlaybackEngineFactory
import com.wire.media.player.AudioMediaPlayingState
import com.wire.media.player.AudioState
import com.wire.media.player.MediaPlaybackEngine
import com.wire.media.player.PlaybackCommand
import com.wire.media.player.PlaybackCommandResult
import com.wire.media.player.PlaybackEvent
import com.wire.media.player.PlaybackSnapshot
import com.wire.media.player.PlaybackSource
import com.wire.media.recording.RecordingCapabilityResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecordAudioMessagePlayerTest {
    @Test
    fun `preview coordinates focus playback seek completion and release`() = runTest {
        val engine = FakeEngine()
        val factory = mockk<AndroidMediaPlayerPlaybackEngineFactory> {
            every { create() } returns engine
        }
        val pauseCallback = slot<() -> Unit>()
        val resumeCallback = slot<() -> Unit>()
        val focus = mockk<AudioFocusHelper>(relaxUnitFun = true) {
            every { request() } returns true
            every { setListener(capture(pauseCallback), capture(resumeCallback)) } returns Unit
        }
        val player = RecordAudioMessagePlayer(factory, focus, backgroundScope)
        val path = "/tmp/preview.wav".toPath()

        assertEquals(RecordingCapabilityResult.Success(Unit), player.toggle(path))
        assertEquals(
            PlaybackCommand.Prepare(PlaybackSource.Local(path), playWhenReady = true),
            engine.commands.last(),
        )
        engine.emit(PlaybackEvent.Ready(DURATION_MS))
        runCurrent()
        assertEquals(AudioMediaPlayingState.Playing, player.state.value.audioMediaPlayingState)
        assertEquals(AudioState.TotalTimeInMs.Known(DURATION_MS), player.state.value.totalTimeInMs)

        pauseCallback.captured()
        runCurrent()
        assertEquals(AudioMediaPlayingState.Paused, player.state.value.audioMediaPlayingState)
        resumeCallback.captured()
        runCurrent()
        assertEquals(AudioMediaPlayingState.Playing, player.state.value.audioMediaPlayingState)

        player.seekTo(500)
        runCurrent()
        assertEquals(500, player.state.value.currentPositionInMs)
        engine.emit(PlaybackEvent.Completed)
        runCurrent()
        assertEquals(AudioMediaPlayingState.Completed, player.state.value.audioMediaPlayingState)
        assertEquals(0, player.state.value.currentPositionInMs)
        verify(atLeast = 1) { focus.abandon() }

        player.release()
        assertTrue(engine.commands.last() is PlaybackCommand.Release)
        verify(atLeast = 1) { focus.request() }
        verify(atLeast = 1) { focus.abandon() }
    }

    private class FakeEngine : MediaPlaybackEngine {
        val commands = mutableListOf<PlaybackCommand>()
        private var listener: ((PlaybackEvent) -> Unit)? = null

        override fun setEventListener(listener: ((PlaybackEvent) -> Unit)?) {
            this.listener = listener
        }

        override fun execute(command: PlaybackCommand): PlaybackCommandResult {
            commands += command
            when (command) {
                PlaybackCommand.Play -> emit(PlaybackEvent.Playing)
                PlaybackCommand.Pause -> emit(PlaybackEvent.Paused)
                PlaybackCommand.Stop -> emit(PlaybackEvent.Stopped)
                is PlaybackCommand.SeekTo -> emit(PlaybackEvent.PositionChanged(command.positionMs, DURATION_MS))
                PlaybackCommand.Release -> emit(PlaybackEvent.Released)
                else -> Unit
            }
            return PlaybackCommandResult.Executed
        }

        override fun snapshot(): PlaybackSnapshot = PlaybackSnapshot(0, DURATION_MS)

        fun emit(event: PlaybackEvent) {
            listener?.invoke(event)
        }
    }

    private companion object {
        const val DURATION_MS = 1_000
    }
}
