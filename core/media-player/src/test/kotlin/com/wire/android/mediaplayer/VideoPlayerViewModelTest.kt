/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.mediaplayer

import androidx.lifecycle.ViewModel
import com.wire.android.config.CoroutineTestExtension
import com.wire.media.player.MediaPlaybackEngine
import com.wire.media.player.PlaybackCommand
import com.wire.media.player.PlaybackCommandResult
import com.wire.media.player.PlaybackEvent
import com.wire.media.player.PlaybackSnapshot
import com.wire.media.player.PlaybackSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class VideoPlayerViewModelTest {
    @Test
    fun givenLocalPath_whenCreated_thenCommonEnginePreparesLocalSource() = runTest {
        val (arrangement, viewModel) = Arrangement(localPath = "/tmp/video.mp4").arrange()

        assertEquals(
            PlaybackSource.Local("/tmp/video.mp4".toPath()),
            (arrangement.engine.commands.single() as PlaybackCommand.Prepare).source,
        )
        assertSame(arrangement.surfaceController, viewModel.surfaceController)
    }

    @Test
    fun givenRemoteUrl_whenCreated_thenCommonEnginePreparesRemoteSource() = runTest {
        val (arrangement, _) = Arrangement(contentUrl = "https://wire.test/video.mp4").arrange()

        assertEquals(
            PlaybackSource.Remote("https://wire.test/video.mp4"),
            (arrangement.engine.commands.single() as PlaybackCommand.Prepare).source,
        )
    }

    @Test
    fun givenNativeEvents_whenControllingPlayback_thenCommonStateTracksPlayback() = runTest {
        val (arrangement, viewModel) = Arrangement(localPath = "/tmp/video.mp4").arrange()
        arrangement.engine.emit(PlaybackEvent.Buffering(true))
        assertTrue(viewModel.state.value.isBuffering)

        arrangement.engine.emit(PlaybackEvent.Ready(durationMs = 5_000))
        viewModel.play()
        viewModel.seekTo(1_250)
        viewModel.toggleMute()
        viewModel.pause()

        assertTrue(viewModel.state.value.isPrepared)
        assertFalse(viewModel.state.value.isPlaying)
        assertEquals(1_250, viewModel.state.value.currentPositionMs)
        assertEquals(5_000, viewModel.state.value.durationMs)
        assertTrue(viewModel.state.value.isMuted)
        assertEquals(
            listOf(
                PlaybackCommand.Play,
                PlaybackCommand.SeekTo(1_250),
                PlaybackCommand.SetMuted(true),
                PlaybackCommand.Pause,
            ),
            arrangement.engine.commands.takeLast(4),
        )
    }

    @Test
    fun givenCompletedPlayback_whenToggled_thenReplayStartsAtBeginningAndReleaseTearsDownEngine() = runTest {
        val (arrangement, viewModel) = Arrangement(localPath = "/tmp/video.mp4").arrange()
        arrangement.engine.emit(PlaybackEvent.Ready(durationMs = 5_000))
        arrangement.engine.emit(PlaybackEvent.Completed)

        viewModel.togglePlayPause()

        assertEquals(PlaybackCommand.SeekTo(0), arrangement.engine.commands.takeLast(2).first())
        assertEquals(PlaybackCommand.Play, arrangement.engine.commands.last())
        assertTrue(viewModel.state.value.isPlaying)

        arrangement.clear(viewModel)
        assertEquals(PlaybackCommand.Release, arrangement.engine.commands.last())
        assertTrue(viewModel.state.value.isReleased)
    }

    private class Arrangement(
        private val localPath: String? = null,
        private val contentUrl: String? = null,
    ) {
        val engine = FakePlaybackEngine()
        val surfaceController = mockk<VideoPlaybackSurfaceController>(relaxed = true)
        private val sessionFactory = mockk<AndroidVideoPlaybackSessionFactory>()

        init {
            every { sessionFactory.create() } returns AndroidVideoPlaybackSession(engine, surfaceController)
        }

        fun arrange() = this to VideoPlayerViewModel(
            sessionFactory = sessionFactory,
            localPath = localPath,
            contentUrl = contentUrl,
            fileName = "video.mp4",
        )

        fun clear(viewModel: ViewModel) {
            val method = ViewModel::class.java.getDeclaredMethod("onCleared")
            method.isAccessible = true
            method.invoke(viewModel)
        }
    }

    private class FakePlaybackEngine : MediaPlaybackEngine {
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
                PlaybackCommand.Release -> emit(PlaybackEvent.Released)
                else -> Unit
            }
            return PlaybackCommandResult.Executed
        }

        override fun snapshot(): PlaybackSnapshot? = null

        fun emit(event: PlaybackEvent) {
            listener?.invoke(event)
        }
    }
}
