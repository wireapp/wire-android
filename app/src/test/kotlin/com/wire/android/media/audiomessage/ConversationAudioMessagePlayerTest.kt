/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.media.audiomessage

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer.MessageIdWrapper
import com.wire.android.services.ServicesManager
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toOkioPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

private val dispatcher = UnconfinedTestDispatcher()

@Suppress("LongMethod")
class ConversationAudioMessagePlayerTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun givenTheSuccessfulAssetFetch_whenPlayingAudioForFirstTime_thenEmitStatesAsExpected() = runTest(dispatcher) {
        val (arrangement, conversationAudioMessagePlayer) = Arrangement(tempDir)
            .withAudioMediaPlayerReturningTotalTime(1000)
            .withSuccessfulAssetFetch()
            .withCurrentSession()
            .arrange()

        val testAudioMessageId = "some-dummy-message-id"
        val testAssetId = "some-dummy-asset-id"
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val messageIdWrapper = MessageIdWrapper(conversationId, testAudioMessageId)

        conversationAudioMessagePlayer.observableAudioMessagesState.test {
            // skip first emit from onStart
            awaitItem()
            conversationAudioMessagePlayer.playAudio(
                conversationId,
                testAudioMessageId,
                testAssetId
            )

            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)

                val totalTime = currentState!!.totalTimeInMs
                assert(totalTime is AudioState.TotalTimeInMs.Known)
                assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
            }

            cancelAndIgnoreRemainingEvents()
        }

        with(arrangement) {
            verify { mediaPlayer.prepare() }
            verify { mediaPlayer.setDataSource(any(), any()) }
            verify { mediaPlayer.start() }

            verify(exactly = 1) { audioFocusHelper.request() }
            verify(exactly = 1) { servicesManager.startPlayingAudioMessageService() }

            verify(exactly = 0) { mediaPlayer.seekTo(any()) }
        }
    }

    @Test
    fun givenTheSuccessfulAssetFetch_whenPlayingTheSameMessageIdTwiceSequentially_thenEmitStatesAsExpected() = runTest(dispatcher) {
        val (arrangement, conversationAudioMessagePlayer) = Arrangement(tempDir)
            .withSuccessfulAssetFetch()
            .withCurrentSession()
            .withAudioMediaPlayerReturningTotalTime(1000)
            .withMediaPlayerPlaying()
            .arrange()

        val testAudioMessageId = "some-dummy-message-id"
        val testAssetId = "some-dummy-asset-id"

        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val messageIdWrapper = MessageIdWrapper(conversationId, testAudioMessageId)

        conversationAudioMessagePlayer.observableAudioMessagesState.test {
            // skip first emit from onStart
            awaitItem()
            // playing first time
            conversationAudioMessagePlayer.playAudio(
                conversationId,
                testAudioMessageId,
                testAssetId
            )

            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
            }
            awaitItem() // currentPosition update
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)

                val totalTime = currentState!!.totalTimeInMs
                assert(totalTime is AudioState.TotalTimeInMs.Known)
                assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
            }

            // playing second time
            conversationAudioMessagePlayer.playAudio(
                conversationId,
                testAudioMessageId,
                testAssetId
            )
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Paused)
            }

            cancelAndIgnoreRemainingEvents()
        }

        with(arrangement) {
            verify(exactly = 1) { mediaPlayer.prepare() }
            verify(exactly = 1) { mediaPlayer.setDataSource(any(), any()) }
            verify(exactly = 1) { mediaPlayer.start() }
            verify(exactly = 1) { mediaPlayer.pause() }

            verify(exactly = 0) { mediaPlayer.seekTo(any()) }
        }
    }

    @Test
    fun givenTheSuccessfulAssetFetch_whenPlayingDifferentAudioAfterFirstOneIsPlayed_thenEmitStatesAsExpected() =
        runTest(dispatcher) {
            val (arrangement, conversationAudioMessagePlayer) = Arrangement(tempDir)
                .withSuccessfulAssetFetch()
                .withCurrentSession()
                .withAudioMediaPlayerReturningTotalTime(1000)
                .arrange()

            val firstAudioMessageId = "some-dummy-message-id1"
            val secondAudioMessageId = "some-dummy-message-id2"
            val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
            val firstAudioMessageIdWrapper = MessageIdWrapper(conversationId, firstAudioMessageId)
            val secondAudioMessageIdWrapper = MessageIdWrapper(conversationId, secondAudioMessageId)

            val testAssetId = "some-dummy-asset-id"

            conversationAudioMessagePlayer.observableAudioMessagesState.test {
                // skip first emit from onStart
                awaitItem()
                // playing first audio message
                conversationAudioMessagePlayer.playAudio(
                    conversationId,
                    firstAudioMessageId,
                    testAssetId
                )

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    val totalTime = currentState!!.totalTimeInMs
                    assert(totalTime is AudioState.TotalTimeInMs.Known)
                    assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
                }

                // playing second audio message
                conversationAudioMessagePlayer.playAudio(
                    conversationId,
                    secondAudioMessageId,
                    testAssetId
                )
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Stopped)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Completed)
                }
                awaitItem() // seekToAudioPosition
                awaitAndAssertStateUpdate { state ->
                    assertEquals(2, state.size)

                    val currentState = state[secondAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }

                cancelAndIgnoreRemainingEvents()
            }

            with(arrangement) {
                verify(exactly = 2) { mediaPlayer.prepare() }
                verify(exactly = 2) { mediaPlayer.setDataSource(any(), any()) }
                verify(exactly = 2) { mediaPlayer.start() }

                verify(exactly = 0) { mediaPlayer.seekTo(any()) }
            }
        }

    @Test
    fun givenTheSuccessfulAssetFetch_whenPlayingDifferentAudioAfterFirstOneIsPlayedAndSecondResumed_thenEmitStatesAsExpected() =
        runTest(dispatcher) {
            val (arrangement, conversationAudioMessagePlayer) = Arrangement(tempDir)
                .withSuccessfulAssetFetch()
                .withCurrentSession()
                .withAudioMediaPlayerReturningTotalTime(1000)
                .arrange()

            val firstAudioMessageId = "some-dummy-message-id1"
            val secondAudioMessageId = "some-dummy-message-id2"
            val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
            val firstAudioMessageIdWrapper = MessageIdWrapper(conversationId, firstAudioMessageId)
            val secondAudioMessageIdWrapper = MessageIdWrapper(conversationId, secondAudioMessageId)

            val testAssetId = "some-dummy-asset-id"

            conversationAudioMessagePlayer.observableAudioMessagesState.test {
                // skip first emit from onStart
                awaitItem()
                // playing first audio message
                conversationAudioMessagePlayer.playAudio(
                    conversationId,
                    firstAudioMessageId,
                    testAssetId
                )

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)

                    val totalTime = currentState!!.totalTimeInMs
                    assert(totalTime is AudioState.TotalTimeInMs.Known)
                    assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
                }

                // playing second audio message
                conversationAudioMessagePlayer.playAudio(
                    ConversationId("some-dummy-value", "some.dummy.domain"),
                    secondAudioMessageId,
                    testAssetId
                )

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Stopped)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Completed)
                }
                awaitItem() // seekToAudioPosition
                awaitAndAssertStateUpdate { state ->
                    assertEquals(2, state.size)

                    val currentState = state[secondAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    val totalTime = currentState!!.totalTimeInMs
                    assert(totalTime is AudioState.TotalTimeInMs.Known)
                    assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
                }

                // playing first audio message again, resuming it
                conversationAudioMessagePlayer.playAudio(
                    ConversationId("some-dummy-value", "some.dummy.domain"),
                    firstAudioMessageId,
                    testAssetId
                )
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Stopped)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Completed)
                }
                awaitItem() // seekToAudioPosition
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)

                    val totalTime = currentState!!.totalTimeInMs
                    assert(totalTime is AudioState.TotalTimeInMs.Known)
                    assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
                }
                cancelAndIgnoreRemainingEvents()
            }

            with(arrangement) {
                verify(exactly = 3) { mediaPlayer.prepare() }
                verify(exactly = 3) { mediaPlayer.setDataSource(any(), any()) }
                verify(exactly = 3) { mediaPlayer.start() }

                verify(exactly = 1) { mediaPlayer.seekTo(any()) }
            }
        }

    @Test
    fun givenTheSuccessfulAssetFetch_whenPlayingDifferentAudioAfterFirstOneIsPlayedAndSecondStoppedAndResume_thenEmitStatesAsExpected() =
        runTest(dispatcher) {
            val (arrangement, conversationAudioMessagePlayer) = Arrangement(tempDir)
                .withSuccessfulAssetFetch()
                .withCurrentSession()
                .withAudioMediaPlayerReturningTotalTime(1000)
                .withMediaPlayerPlaying()
                .arrange()

            val testAudioMessageId = "some-dummy-message-id"
            val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
            val messageIdWrapper = MessageIdWrapper(conversationId, testAudioMessageId)

            val testAssetId = "some-dummy-asset-id"

            conversationAudioMessagePlayer.observableAudioMessagesState.test {
                // skip first emit from onStart
                awaitItem()
                // playing first time
                conversationAudioMessagePlayer.playAudio(
                    conversationId,
                    testAudioMessageId,
                    testAssetId
                )

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[messageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[messageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[messageIdWrapper]
                    assert(currentState != null)
                    assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[messageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }
                awaitItem() // currentPosition update
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[messageIdWrapper]
                    assert(currentState != null)
                    val totalTime = currentState!!.totalTimeInMs
                    assert(totalTime is AudioState.TotalTimeInMs.Known)
                    assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
                }

                // playing second time
                conversationAudioMessagePlayer.playAudio(
                    conversationId,
                    testAudioMessageId,
                    testAssetId
                )
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[messageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Paused)
                }

                // stop the media player by mocking the return value of isPlaying
                arrangement.withMediaPlayerStopped()

                // playing third time
                conversationAudioMessagePlayer.playAudio(
                    conversationId,
                    testAudioMessageId,
                    testAssetId
                )
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[messageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }

                cancelAndIgnoreRemainingEvents()
            }

            with(arrangement) {
                verify(exactly = 1) { mediaPlayer.prepare() }
                verify(exactly = 1) { mediaPlayer.setDataSource(any(), any()) }
                verify(exactly = 2) { mediaPlayer.start() }
                verify(exactly = 1) { mediaPlayer.pause() }

                verify(exactly = 0) { mediaPlayer.seekTo(any()) }
            }
        }

    @Test
    fun givenTheSuccessfulAssetFetch_whenAudioSpeedChanged_thenMediaPlayerParamsWereUpdated() = runTest(dispatcher) {
        val params = PlaybackParams()
        val (arrangement, conversationAudioMessagePlayer) = Arrangement(tempDir)
            .withSuccessfulAssetFetch()
            .withCurrentSession()
            .withAudioMediaPlayerReturningParams(params)
            .arrange()

        // when
        conversationAudioMessagePlayer.setSpeed(AudioSpeed.MAX)

        // then
        verify(exactly = 1) { arrangement.mediaPlayer.playbackParams = params.setSpeed(2F) }
    }

    @Test
    fun givenPlayingAudioMessage_whenStopAudioCalled_thenServiceStoppedAndAudioFocusAbandoned() = runTest(dispatcher) {
        val (arrangement, conversationAudioMessagePlayer) = Arrangement(tempDir)
            .withAudioMediaPlayerReturningTotalTime(1000)
            .withSuccessfulAssetFetch()
            .withCurrentSession()
            .arrange()

        val testAudioMessageId = "some-dummy-message-id"
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")

        val testAssetId = "some-dummy-asset-id"

        conversationAudioMessagePlayer.observableAudioMessagesState.test {
            // skip first emit from onStart
            awaitItem()
            conversationAudioMessagePlayer.playAudio(
                conversationId,
                testAudioMessageId,
                testAssetId
            )

            conversationAudioMessagePlayer.forceToStopCurrentAudioMessage()

            cancelAndIgnoreRemainingEvents()
        }

        with(arrangement) {
            verify(exactly = 1) { audioFocusHelper.abandon() }
            verify(exactly = 1) { servicesManager.stopPlayingAudioMessageService() }
        }
    }

    @Test
    fun givenCachedSuccessfulAudioMessageFetchWithExistingFile_whenPlayingAgain_thenReuseTheSameAssetResult() = runTest(dispatcher) {
        val (arrangement, conversationAudioMessagePlayer) = Arrangement(tempDir)
            .withAudioMediaPlayerReturningTotalTime(1000)
            .withSuccessfulAssetFetch(fileExists = true)
            .withCurrentSession()
            .arrange()

        val audioMessageId = "some-dummy-message-id"
        val assetId = "some-dummy-asset-id"
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")

        conversationAudioMessagePlayer.playAudio(conversationId, audioMessageId, assetId) // play the first time
        conversationAudioMessagePlayer.forceToStopCurrentAudioMessage() // mock the completion of the audio media player
        conversationAudioMessagePlayer.playAudio(conversationId, audioMessageId, assetId) // play the second time

        with(arrangement) {
            coVerify(exactly = 1) { // only one time because the result is cached
                getAssetMessage(conversationId, audioMessageId)
            }
        }
    }

    @Test
    fun givenCachedSuccessfulAudioMessageFetchWithNonExistingFile_whenPlayingAgain_thenGetAssetAgain() = runTest(dispatcher) {
        val (arrangement, conversationAudioMessagePlayer) = Arrangement(tempDir)
            .withAudioMediaPlayerReturningTotalTime(1000)
            .withSuccessfulAssetFetch()
            .withCurrentSession()
            .arrange()

        val audioMessageId = "some-dummy-message-id"
        val assetId = "some-dummy-asset-id"
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")

        arrangement.withSuccessfulAssetFetch(fileExists = true) // first time the file exists
        conversationAudioMessagePlayer.playAudio(conversationId, audioMessageId, assetId) // play the first time
        conversationAudioMessagePlayer.forceToStopCurrentAudioMessage() // mock the completion of the audio media player
        arrangement.withSuccessfulAssetFetch(fileExists = false) // second time the file does not exist anymore
        conversationAudioMessagePlayer.playAudio(conversationId, audioMessageId, assetId) // play the second time

        with(arrangement) {
            coVerify(exactly = 2) { // two times because the file did not exist so it's fetched again with proper file path
                getAssetMessage(conversationId, audioMessageId)
            }
        }
    }

    @Test
    fun givenCachedFailedAudioMessageFetch_whenPlayingAgain_thenGetAssetAgain() = runTest(dispatcher) {
        val (arrangement, conversationAudioMessagePlayer) = Arrangement(tempDir)
            .withAudioMediaPlayerReturningTotalTime(1000)
            .withFailedAssetFetch()
            .withCurrentSession()
            .arrange()

        val audioMessageId = "some-dummy-message-id"
        val assetId = "some-dummy-asset-id"
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")

        conversationAudioMessagePlayer.playAudio(conversationId, audioMessageId, assetId) // play the first time
        conversationAudioMessagePlayer.forceToStopCurrentAudioMessage() // mock the completion of the audio media player
        conversationAudioMessagePlayer.playAudio(conversationId, audioMessageId, assetId) // play the second time

        with(arrangement) {
            coVerify(exactly = 2) { // two times because the result is failed so it's fetched again
                getAssetMessage(conversationId, audioMessageId)
            }
        }
    }

    private suspend fun <T> TurbineTestContext<T>.awaitAndAssertStateUpdate(assertion: (T) -> Unit) {
        val state = awaitItem()
        assert(state != null)

        assertion(state)
    }
}

class Arrangement(private val tempDir: File) {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var coreLogic: CoreLogic

    @MockK
    lateinit var mediaPlayer: MediaPlayer

    @MockK
    lateinit var wavesMaskHelper: AudioWavesMaskHelper

    @MockK
    lateinit var servicesManager: ServicesManager

    @MockK
    lateinit var audioFocusHelper: AudioFocusHelper

    @MockK
    lateinit var getAssetMessage: GetMessageAssetUseCase

    private val testScope: CoroutineScope = CoroutineScope(dispatcher)

    private val conversationAudioMessagePlayer by lazy {
        ConversationAudioMessagePlayer(
            context = context,
            audioMediaPlayer = mediaPlayer,
            wavesMaskHelper = wavesMaskHelper,
            servicesManager = { servicesManager },
            audioFocusHelper = audioFocusHelper,
            coreLogic = coreLogic,
            scope = testScope,
            dispatchers = TestDispatcherProvider(dispatcher),
        )
    }

    init {
        MockKAnnotations.init(this, relaxed = true)

        every { coreLogic.getSessionScope(any()).messages.getAssetMessage } returns getAssetMessage
        every { wavesMaskHelper.getWaveMask(any<Path>()) } returns WAVES_MASK
        every { wavesMaskHelper.clear() } returns Unit
        every { mediaPlayer.currentPosition } returns 100

        every { servicesManager.stopPlayingAudioMessageService() } returns Unit
        every { servicesManager.startPlayingAudioMessageService() } returns Unit

        every { audioFocusHelper.setListener(any(), any()) } returns Unit
        every { audioFocusHelper.abandon() } returns Unit
        every { audioFocusHelper.request() } returns true
    }

    fun withCurrentSession() = apply {
        coEvery { coreLogic.getGlobalScope().session.currentSession.invoke() } returns CurrentSessionResult.Success(
            AccountInfo.Valid(UserId("some-user-value", "some.user.domain"))
        )
    }

    fun withSuccessfulAssetFetch(fileExists: Boolean = true) = apply {
        val assetFile = File(tempDir, "some-dummy-asset-name")
        if (fileExists) {
            assetFile.createNewFile()
        } else {
            assetFile.delete()
        }
        coEvery {
            getAssetMessage.invoke(any<ConversationId>(), any<String>())
        } returns CompletableDeferred(
            MessageAssetResult.Success(
                decodedAssetPath = assetFile.toOkioPath(),
                assetSize = 0,
                assetName = "some-dummy-asset-name"
            )
        )
    }

    fun withFailedAssetFetch() = apply {
        coEvery {
            getAssetMessage.invoke(any<ConversationId>(), any<String>())
        } returns CompletableDeferred(
            MessageAssetResult.Failure(NetworkFailure.NoNetworkConnection(null), false)
        )
    }

    fun withMediaPlayerPlaying() = apply {
        every { mediaPlayer.isPlaying } returns true
    }

    fun withMediaPlayerStopped() = apply {
        every { mediaPlayer.isPlaying } returns false
    }

    fun withAudioMediaPlayerReturningTotalTime(total: Int) = apply {
        every { mediaPlayer.duration } returns total
    }

    fun withAudioMediaPlayerReturningParams(params: PlaybackParams = PlaybackParams()) = apply {
        every { mediaPlayer.playbackParams } returns params
    }

    fun arrange() = this to conversationAudioMessagePlayer

    companion object {
        val WAVES_MASK = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
    }
}
