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

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MediaPlaybackCoordinatorTest {
    @Test
    fun `prepare and native events produce common preparation and buffering state`() = runTest {
        val engine = FakePlaybackEngine()
        val coordinator = MediaPlaybackCoordinator(engine, backgroundScope)

        coordinator.prepare(PlaybackSource.Local("/video.mp4".toPath()), restoredPositionMs = 250)
        assertTrue(coordinator.state.value.isBuffering)
        assertEquals(250, coordinator.state.value.currentPositionMs)

        engine.emit(PlaybackEvent.Buffering(true))
        engine.emit(PlaybackEvent.Ready(durationMs = 1_000))

        assertTrue(coordinator.state.value.isPrepared)
        assertFalse(coordinator.state.value.isBuffering)
        assertEquals(1_000, coordinator.state.value.durationMs)
    }

    @Test
    fun `play pause seek mute speed completion and replay share one state machine`() = runTest {
        val engine = FakePlaybackEngine()
        val coordinator = MediaPlaybackCoordinator(engine, backgroundScope)
        coordinator.prepare(PlaybackSource.Remote("https://wire.test/video"))
        engine.emit(PlaybackEvent.Ready(1_000))

        coordinator.play()
        assertTrue(coordinator.state.value.isPlaying)
        assertTrue(coordinator.state.value.isStarted)

        coordinator.seekTo(400)
        coordinator.toggleMute()
        coordinator.setSpeed(PlaybackSpeed.FAST)
        assertEquals(400, coordinator.state.value.currentPositionMs)
        assertTrue(coordinator.state.value.isMuted)
        assertEquals(PlaybackSpeed.FAST, coordinator.state.value.speed)

        coordinator.pause()
        assertFalse(coordinator.state.value.isPlaying)

        engine.emit(PlaybackEvent.Completed)
        assertTrue(coordinator.state.value.isCompleted)
        assertEquals(1_000, coordinator.state.value.currentPositionMs)

        coordinator.togglePlayPause()
        assertTrue(coordinator.state.value.isPlaying)
        assertFalse(coordinator.state.value.isCompleted)
        assertEquals(0, coordinator.state.value.currentPositionMs)
    }

    @Test
    fun `play when ready restores position before starting`() = runTest {
        val engine = FakePlaybackEngine()
        val coordinator = MediaPlaybackCoordinator(engine, backgroundScope)

        coordinator.prepare(
            source = PlaybackSource.Local("/audio.m4a".toPath()),
            restoredPositionMs = 700,
            playWhenReady = true,
        )
        engine.emit(PlaybackEvent.Ready(2_000))

        assertTrue(coordinator.state.value.isPlaying)
        assertEquals(700, coordinator.state.value.currentPositionMs)
        assertTrue(engine.commands.contains(PlaybackCommand.Play))
    }

    @Test
    fun `playing polls native position and stopping cancels polling`() = runTest {
        val engine = FakePlaybackEngine().apply {
            snapshot = PlaybackSnapshot(currentPositionMs = 321, durationMs = 900)
        }
        val coordinator = MediaPlaybackCoordinator(engine, backgroundScope, positionPollIntervalMs = 50)

        coordinator.play()
        runCurrent()
        assertEquals(321, coordinator.state.value.currentPositionMs)

        engine.snapshot = PlaybackSnapshot(currentPositionMs = 654, durationMs = 900)
        advanceTimeBy(50)
        runCurrent()
        assertEquals(654, coordinator.state.value.currentPositionMs)

        coordinator.pause()
        val snapshotsAfterPause = engine.snapshotCalls
        advanceTimeBy(200)
        runCurrent()
        assertEquals(snapshotsAfterPause, engine.snapshotCalls)
    }

    @Test
    fun `release tears down engine and rejects later commands`() = runTest {
        val engine = FakePlaybackEngine()
        val coordinator = MediaPlaybackCoordinator(engine, backgroundScope)

        coordinator.release()

        assertTrue(coordinator.state.value.isReleased)
        assertIs<PlaybackCommandResult.Failure>(coordinator.play())
        assertEquals(PlaybackCommand.Release, engine.commands.last())
    }

    private class FakePlaybackEngine : MediaPlaybackEngine {
        val commands = mutableListOf<PlaybackCommand>()
        var snapshot: PlaybackSnapshot? = null
        var snapshotCalls: Int = 0
        private var listener: ((PlaybackEvent) -> Unit)? = null

        override fun setEventListener(listener: ((PlaybackEvent) -> Unit)?) {
            this.listener = listener
        }

        override fun execute(command: PlaybackCommand): PlaybackCommandResult {
            commands += command
            return PlaybackCommandResult.Executed
        }

        override fun snapshot(): PlaybackSnapshot? {
            snapshotCalls++
            return snapshot
        }

        fun emit(event: PlaybackEvent) {
            listener?.invoke(event)
        }
    }
}
