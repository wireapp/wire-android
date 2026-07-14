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
import com.ramcosta.composedestinations.generated.cells.destinations.CellAudioPlayerScreenDestination
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class, NavigationTestExtension::class)
class AudioPlayerViewModelTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun givenLocalPath_whenInitialized_thenDataSourceIsSetWithFileUriAndPrepared() = runTest {
        val (arrangement, _) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .arrange()

        verify { Uri.fromFile(any()) }
        verify { anyConstructed<MediaPlayer>().setDataSource(arrangement.context, arrangement.fileUri) }
        verify { anyConstructed<MediaPlayer>().prepareAsync() }
    }

    @Test
    fun givenContentUrl_whenInitialized_thenDataSourceIsSetWithParsedUri() = runTest {
        val (arrangement, _) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(contentUrl = "https://wire.com/audio.mp3"))
            .arrange()

        verify { Uri.parse("https://wire.com/audio.mp3") }
        verify { anyConstructed<MediaPlayer>().setDataSource(arrangement.context, arrangement.contentUri) }
        verify { anyConstructed<MediaPlayer>().prepareAsync() }
    }

    @Test
    fun givenNoSource_whenInitialized_thenDataSourceIsNotSet() = runTest {
        Arrangement()
            .withNavArgs(AudioPlayerNavArgs())
            .arrange()

        verify(exactly = 0) { anyConstructed<MediaPlayer>().setDataSource(any<Context>(), any<Uri>()) }
        verify(exactly = 0) { anyConstructed<MediaPlayer>().prepareAsync() }
    }

    @Test
    fun givenSetDataSourceThrows_whenInitialized_thenExceptionIsHandledSilently() = runTest {
        val (_, viewModel) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .withSetDataSourceThrowing()
            .arrange()

        assertFalse(viewModel.state.value.isPrepared)
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
    fun givenPlayerPrepares_whenOnPrepared_thenStateHasDurationAndIsPrepared() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .withDuration(5000)
            .arrange()

        arrangement.triggerPrepared()

        assertEquals(5000, viewModel.state.value.durationMs)
        assertTrue(viewModel.state.value.isPrepared)
    }

    @Test
    fun givenNotPrepared_whenPlay_thenNothingHappens() = runTest {
        val (_, viewModel) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .arrange()

        viewModel.play()

        verify(exactly = 0) { anyConstructed<MediaPlayer>().start() }
        assertFalse(viewModel.state.value.isPlaying)
    }

    @Test
    fun givenPrepared_whenPlay_thenStartsAndUpdatesStateAndPollsPosition() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .withCurrentPosition(42)
            .arrange()
        arrangement.triggerPrepared()

        viewModel.play()

        verify { anyConstructed<MediaPlayer>().start() }
        assertTrue(viewModel.state.value.isPlaying)
        assertFalse(viewModel.state.value.isCompleted)
        assertEquals(42, viewModel.state.value.currentPositionMs)

        arrangement.clear(viewModel)
    }

    @Test
    fun givenNotPlaying_whenPause_thenNothingHappens() = runTest {
        val (_, viewModel) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .arrange()

        viewModel.pause()

        verify(exactly = 0) { anyConstructed<MediaPlayer>().pause() }
    }

    @Test
    fun givenPlaying_whenPause_thenPausesAndUpdatesState() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .arrange()
        arrangement.triggerPrepared()
        viewModel.play()

        viewModel.pause()

        verify { anyConstructed<MediaPlayer>().pause() }
        assertFalse(viewModel.state.value.isPlaying)
    }

    @Test
    fun givenNotPlaying_whenTogglePlayPause_thenStartsPlaying() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .arrange()
        arrangement.triggerPrepared()

        viewModel.togglePlayPause()

        verify { anyConstructed<MediaPlayer>().start() }
        assertTrue(viewModel.state.value.isPlaying)

        arrangement.clear(viewModel) // stop the position-polling loop before runTest drains the scheduler
    }

    @Test
    fun givenPlaying_whenTogglePlayPause_thenPauses() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .arrange()
        arrangement.triggerPrepared()
        viewModel.play()

        viewModel.togglePlayPause()

        verify { anyConstructed<MediaPlayer>().pause() }
        assertFalse(viewModel.state.value.isPlaying)
    }

    @Test
    fun givenCompleted_whenTogglePlayPause_thenSeeksToStartAndPlays() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .arrange()
        arrangement.triggerPrepared()
        arrangement.triggerCompletion()

        viewModel.togglePlayPause()

        verify { anyConstructed<MediaPlayer>().seekTo(0) }
        verify { anyConstructed<MediaPlayer>().start() }
        assertTrue(viewModel.state.value.isPlaying)
        assertFalse(viewModel.state.value.isCompleted)

        arrangement.clear(viewModel) // stop the position-polling loop before runTest drains the scheduler
    }

    @Test
    fun whenSeekTo_thenMediaPlayerSeeksAndStateUpdated() = runTest {
        val (_, viewModel) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .arrange()

        viewModel.seekTo(1234)

        verify { anyConstructed<MediaPlayer>().seekTo(1234) }
        assertEquals(1234, viewModel.state.value.currentPositionMs)
    }

    @Test
    fun givenPlaying_whenCompletionFires_thenStateIsCompletedAndNotPlaying() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .arrange()
        arrangement.triggerPrepared()
        viewModel.play()

        arrangement.triggerCompletion()

        assertFalse(viewModel.state.value.isPlaying)
        assertTrue(viewModel.state.value.isCompleted)
    }

    @Test
    fun whenCleared_thenPlayerIsStoppedAndReleased() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .arrange()

        arrangement.clear(viewModel)

        verify { anyConstructed<MediaPlayer>().stop() }
        verify { anyConstructed<MediaPlayer>().release() }
    }

    @Test
    fun givenStopThrows_whenCleared_thenReleaseIsStillCalled() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNavArgs(AudioPlayerNavArgs(localPath = "/tmp/audio.mp3"))
            .withStopThrowing()
            .arrange()

        arrangement.clear(viewModel)

        verify { anyConstructed<MediaPlayer>().release() }
    }

    private class Arrangement {

        val context = mockk<Context>(relaxed = true)
        val savedStateHandle = mockk<SavedStateHandle>(relaxed = true)
        val fileUri = mockk<Uri>()
        val contentUri = mockk<Uri>()

        private val preparedMp = mockk<MediaPlayer>(relaxed = true)
        private val preparedListenerSlot = slot<MediaPlayer.OnPreparedListener>()
        private val completionListenerSlot = slot<MediaPlayer.OnCompletionListener>()

        private var navArgs = AudioPlayerNavArgs(localPath = "/tmp/audio.mp3")

        init {
            mockkObject(CellAudioPlayerScreenDestination)

            mockkStatic(Uri::class)
            every { Uri.fromFile(any()) } returns fileUri
            every { Uri.parse(any()) } returns contentUri

            mockkConstructor(MediaPlayer::class)
            every { anyConstructed<MediaPlayer>().setOnPreparedListener(capture(preparedListenerSlot)) } just Runs
            every { anyConstructed<MediaPlayer>().setOnCompletionListener(capture(completionListenerSlot)) } just Runs
            every { anyConstructed<MediaPlayer>().setDataSource(any<Context>(), any<Uri>()) } just Runs
            every { anyConstructed<MediaPlayer>().prepareAsync() } just Runs
            every { anyConstructed<MediaPlayer>().start() } just Runs
            every { anyConstructed<MediaPlayer>().pause() } just Runs
            every { anyConstructed<MediaPlayer>().seekTo(any<Int>()) } just Runs
            every { anyConstructed<MediaPlayer>().stop() } just Runs
            every { anyConstructed<MediaPlayer>().release() } just Runs
            every { anyConstructed<MediaPlayer>().currentPosition } returns 0
        }

        fun withNavArgs(args: AudioPlayerNavArgs) = apply { navArgs = args }

        fun withDuration(durationMs: Int) = apply {
            every { preparedMp.duration } returns durationMs
        }

        fun withCurrentPosition(positionMs: Int) = apply {
            every { anyConstructed<MediaPlayer>().currentPosition } returns positionMs
        }

        fun withSetDataSourceThrowing() = apply {
            every { anyConstructed<MediaPlayer>().setDataSource(any<Context>(), any<Uri>()) } throws RuntimeException("boom")
        }

        fun withStopThrowing() = apply {
            every { anyConstructed<MediaPlayer>().stop() } throws IllegalStateException("boom")
        }

        fun triggerPrepared() {
            preparedListenerSlot.captured.onPrepared(preparedMp)
        }

        fun triggerCompletion() {
            completionListenerSlot.captured.onCompletion(preparedMp)
        }

        fun clear(viewModel: ViewModel) {
            val method = ViewModel::class.java.getDeclaredMethod("onCleared")
            method.isAccessible = true
            method.invoke(viewModel)
        }

        fun arrange(): Pair<Arrangement, AudioPlayerViewModel> {
            every { CellAudioPlayerScreenDestination.argsFrom(savedStateHandle) } returns navArgs
            return this to AudioPlayerViewModel(context, savedStateHandle)
        }
    }
}
