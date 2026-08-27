/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature.cells.ui.audioplayer

import androidx.lifecycle.ViewModel
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.mediaplayer.AndroidMediaPlayerPlaybackEngineFactory
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class AudioPlayerViewModelTest {
    @Test
    fun givenLocalPath_whenInitialized_thenCommonEnginePreparesLocalSource() = runTest {
        val (arrangement, _) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .arrange()

        assertEquals(
            PlaybackSource.Local("/tmp/audio.mp3".toPath()),
            (arrangement.engine.commands.single() as PlaybackCommand.Prepare).source,
        )
    }

    @Test
    fun givenContentUrl_whenInitialized_thenCommonEnginePreparesRemoteSource() = runTest {
        val (arrangement, _) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(contentUrl = "https://wire.com/audio.mp3"))
            .arrange()

        assertEquals(
            PlaybackSource.Remote("https://wire.com/audio.mp3"),
            (arrangement.engine.commands.single() as PlaybackCommand.Prepare).source,
        )
    }

    @Test
    fun givenNoSource_whenInitialized_thenEngineIsNotPrepared() = runTest {
        val (arrangement, _) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs())
            .arrange()

        assertTrue(arrangement.engine.commands.isEmpty())
    }

    @Test
    fun givenNavArgs_whenInitialized_thenExposedAsProperties() = runTest {
        val (_, viewModel) = Arrangement()
            .withNavArgs(
                AudioPlayerNavArgs(
                    localPath = "/tmp/audio.mp3",
                    contentUrl = "https://wire.com/audio.mp3",
                    fileName = "audio.mp3",
                )
            )
            .arrange()

        assertEquals("/tmp/audio.mp3", viewModel.localPath)
        assertEquals("https://wire.com/audio.mp3", viewModel.contentUrl)
        assertEquals("audio.mp3", viewModel.fileName)
    }

    @Test
    fun givenPlayerPrepares_whenReadyEventArrives_thenStateHasDurationAndIsPrepared() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        arrangement.engine.emit(PlaybackEvent.Ready(5_000))

        assertEquals(5_000, viewModel.state.value.durationMs)
        assertTrue(viewModel.state.value.isPrepared)
    }

    @Test
    fun givenNotPrepared_whenPlay_thenStateReportsFailureAndDoesNotPlay() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        viewModel.play()

        assertFalse(viewModel.state.value.isPlaying)
        assertEquals("not_prepared", viewModel.state.value.failureReason)
    }

    @Test
    fun givenPrepared_whenPlayAndPause_thenCommonStateAndCommandsAreUpdated() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        arrangement.engine.emit(PlaybackEvent.Ready(5_000))

        viewModel.play()
        assertTrue(viewModel.state.value.isPlaying)
        assertEquals(PlaybackCommand.Play, arrangement.engine.commands.last())

        viewModel.pause()
        assertFalse(viewModel.state.value.isPlaying)
        assertEquals(PlaybackCommand.Pause, arrangement.engine.commands.last())
    }

    @Test
    fun givenCompleted_whenTogglePlayPause_thenSeeksToStartAndPlays() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        arrangement.engine.emit(PlaybackEvent.Ready(5_000))
        arrangement.engine.emit(PlaybackEvent.Completed)

        viewModel.togglePlayPause()

        assertEquals(PlaybackCommand.SeekTo(0), arrangement.engine.commands.takeLast(2).first())
        assertEquals(PlaybackCommand.Play, arrangement.engine.commands.last())
        assertTrue(viewModel.state.value.isPlaying)
        assertFalse(viewModel.state.value.isCompleted)

        arrangement.clear(viewModel)
    }

    @Test
    fun whenSeekTo_thenEngineSeeksAndStateIsUpdated() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        arrangement.engine.emit(PlaybackEvent.Ready(5_000))

        viewModel.seekTo(1_234)

        assertEquals(PlaybackCommand.SeekTo(1_234), arrangement.engine.commands.last())
        assertEquals(1_234, viewModel.state.value.currentPositionMs)
    }

    @Test
    fun givenPlaying_whenCompletionFires_thenStateIsCompletedAndNotPlaying() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        arrangement.engine.emit(PlaybackEvent.Ready(5_000))
        viewModel.play()

        arrangement.engine.emit(PlaybackEvent.Completed)

        assertFalse(viewModel.state.value.isPlaying)
        assertTrue(viewModel.state.value.isCompleted)
    }

    @Test
    fun whenCleared_thenCommonEngineIsReleased() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        arrangement.clear(viewModel)

        assertEquals(PlaybackCommand.Release, arrangement.engine.commands.last())
        assertTrue(viewModel.state.value.isReleased)
    }

    private class Arrangement {
        val engine = FakePlaybackEngine()
        private val engineFactory = mockk<AndroidMediaPlayerPlaybackEngineFactory>()
        private var navArgs = AudioPlayerNavArgs(localPath = "/tmp/audio.mp3")

        init {
            every { engineFactory.create() } returns engine
        }

        fun withNavArgs(args: AudioPlayerNavArgs) = apply { navArgs = args }

        fun clear(viewModel: ViewModel) {
            val method = ViewModel::class.java.getDeclaredMethod("onCleared")
            method.isAccessible = true
            method.invoke(viewModel)
        }

        fun arrange(): Pair<Arrangement, AudioPlayerViewModel> = this to AudioPlayerViewModel(
            engineFactory = engineFactory,
            localPath = navArgs.localPath,
            contentUrl = navArgs.contentUrl,
            fileName = navArgs.fileName,
        )
    }

    private class FakePlaybackEngine : MediaPlaybackEngine {
        val commands = mutableListOf<PlaybackCommand>()
        private var listener: ((PlaybackEvent) -> Unit)? = null
        private var prepared = false

        override fun setEventListener(listener: ((PlaybackEvent) -> Unit)?) {
            this.listener = listener
        }

        override fun execute(command: PlaybackCommand): PlaybackCommandResult {
            commands += command
            if (command == PlaybackCommand.Play && !prepared) return PlaybackCommandResult.Failure("not_prepared")
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
            if (event is PlaybackEvent.Ready) prepared = true
            listener?.invoke(event)
        }
    }
}
