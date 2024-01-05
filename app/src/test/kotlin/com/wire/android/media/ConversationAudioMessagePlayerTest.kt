/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.media

import android.content.Context
import android.media.MediaPlayer
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.media.audiomessage.AudioMediaPlayingState
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@Suppress("LongMethod")
class ConversationAudioMessagePlayerTest {

    @Test
    fun givenTheSuccessFullAssetFetch_whenPlayingAudioForFirstTime_thenEmitStatesAsExpected() = runTest {
        val (arrangement, conversationAudioMessagePlayer) = Arrangement()
            .withAudioMediaPlayerReturningTotalTime(1000)
            .withSuccessFullAssetFetch()
            .arrange()

        val testAudioMessageId = "some-dummy-message-id"

        conversationAudioMessagePlayer.observableAudioMessagesState.test {
            conversationAudioMessagePlayer.playAudio(
                ConversationId("some-dummy-value", "some.dummy.domain"),
                testAudioMessageId
            )

            awaitAndAssertStateUpdate { state ->
                val currentState = state[testAudioMessageId]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[testAudioMessageId]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[testAudioMessageId]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[testAudioMessageId]
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

            verify(exactly = 0) { mediaPlayer.seekTo(any()) }
        }
    }

    @Test
    fun givenTheSuccessFullAssetFetch_whenPlayingTheSameMessageIdTwiceSequentially_thenEmitStatesAsExpected() = runTest {
        val (arrangement, conversationAudioMessagePlayer) = Arrangement()
            .withSuccessFullAssetFetch()
            .withAudioMediaPlayerReturningTotalTime(1000)
            .withMediaPlayerPlaying()
            .arrange()

        val testAudioMessageId = "some-dummy-message-id"

        conversationAudioMessagePlayer.observableAudioMessagesState.test {
            // playing first time
            conversationAudioMessagePlayer.playAudio(
                ConversationId("some-dummy-value", "some.dummy.domain"),
                testAudioMessageId
            )

            awaitAndAssertStateUpdate { state ->
                val currentState = state[testAudioMessageId]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[testAudioMessageId]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[testAudioMessageId]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[testAudioMessageId]
                assert(currentState != null)

                val totalTime = currentState!!.totalTimeInMs
                assert(totalTime is AudioState.TotalTimeInMs.Known)
                assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
            }

            // playing second time
            conversationAudioMessagePlayer.playAudio(
                ConversationId("some-dummy-value", "some.dummy.domain"),
                testAudioMessageId
            )
            awaitAndAssertStateUpdate { state ->
                val currentState = state[testAudioMessageId]
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
    fun givenTheSuccessFullAssetFetch_whenPlayingDifferentAudioAfterFirstOneIsPlayed_thenEmitStatesAsExpected() =
        runTest {
            val (arrangement, conversationAudioMessagePlayer) = Arrangement()
                .withSuccessFullAssetFetch()
                .withAudioMediaPlayerReturningTotalTime(1000)
                .arrange()

            val firstAudioMessageId = "some-dummy-message-id1"
            val secondAudioMessageId = "some-dummy-message-id2"

            conversationAudioMessagePlayer.observableAudioMessagesState.test {
                // playing first audio message
                conversationAudioMessagePlayer.playAudio(
                    ConversationId("some-dummy-value", "some.dummy.domain"),
                    firstAudioMessageId
                )

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
                    assert(currentState != null)
                    val totalTime = currentState!!.totalTimeInMs
                    assert(totalTime is AudioState.TotalTimeInMs.Known)
                    assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
                }

                // playing second audio message
                conversationAudioMessagePlayer.playAudio(
                    ConversationId("some-dummy-value", "some.dummy.domain"),
                    secondAudioMessageId
                )
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Stopped)
                }
                awaitAndAssertStateUpdate { state ->
                    assert(state.size == 2)

                    val currentState = state[secondAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageId]
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
    fun givenTheSuccessFullAssetFetch_whenPlayingDifferentAudioAfterFirstOneIsPlayedAndSecondResumed_thenEmitStatesAsExpected() =
        runTest {
            val (arrangement, conversationAudioMessagePlayer) = Arrangement()
                .withSuccessFullAssetFetch()
                .withAudioMediaPlayerReturningTotalTime(1000)
                .arrange()

            val firstAudioMessageId = "some-dummy-message-id1"
            val secondAudioMessageId = "some-dummy-message-id2"

            conversationAudioMessagePlayer.observableAudioMessagesState.test {
                // playing first audio message
                conversationAudioMessagePlayer.playAudio(
                    ConversationId("some-dummy-value", "some.dummy.domain"),
                    firstAudioMessageId
                )

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
                    assert(currentState != null)

                    val totalTime = currentState!!.totalTimeInMs
                    assert(totalTime is AudioState.TotalTimeInMs.Known)
                    assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
                }

                // playing second audio message
                conversationAudioMessagePlayer.playAudio(
                    ConversationId("some-dummy-value", "some.dummy.domain"),
                    secondAudioMessageId
                )

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Stopped)
                }
                awaitAndAssertStateUpdate { state ->
                    assert(state.size == 2)

                    val currentState = state[secondAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
                    assert(currentState != null)
                    val totalTime = currentState!!.totalTimeInMs
                    assert(totalTime is AudioState.TotalTimeInMs.Known)
                    assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
                }

                // playing first audio message again, resuming it
                conversationAudioMessagePlayer.playAudio(
                    ConversationId("some-dummy-value", "some.dummy.domain"),
                    firstAudioMessageId
                )
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Stopped)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageId]
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
    fun givenTheSuccessFullAssetFetch_whenPlayingDifferentAudioAfterFirstOneIsPlayedAndSecondStoppedAndResume_thenEmitStatesAsExpected() =
        runTest {
            val (arrangement, conversationAudioMessagePlayer) = Arrangement()
                .withSuccessFullAssetFetch()
                .withAudioMediaPlayerReturningTotalTime(1000)
                .withMediaPlayerPlaying()
                .arrange()

            val testAudioMessageId = "some-dummy-message-id"

            conversationAudioMessagePlayer.observableAudioMessagesState.test {
                // playing first time
                conversationAudioMessagePlayer.playAudio(
                    ConversationId("some-dummy-value", "some.dummy.domain"),
                    testAudioMessageId
                )

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[testAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[testAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[testAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[testAudioMessageId]
                    assert(currentState != null)
                    val totalTime = currentState!!.totalTimeInMs
                    assert(totalTime is AudioState.TotalTimeInMs.Known)
                    assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
                }

                // playing second time
                conversationAudioMessagePlayer.playAudio(
                    ConversationId("some-dummy-value", "some.dummy.domain"),
                    testAudioMessageId
                )
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[testAudioMessageId]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Paused)
                }

                // stop the media player by mocking the return value of isPlaying
                arrangement.withMediaPlayerStopped()

                // playing third time
                conversationAudioMessagePlayer.playAudio(
                    ConversationId("some-dummy-value", "some.dummy.domain"),
                    testAudioMessageId
                )
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[testAudioMessageId]
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

    private suspend fun <T> TurbineTestContext<T>.awaitAndAssertStateUpdate(assertion: (T) -> Unit) {
        val state = awaitItem()
        assert(state != null)

        assertion(state)
    }
}

class Arrangement {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var getMessageAssetUseCase: GetMessageAssetUseCase

    @MockK
    lateinit var mediaPlayer: MediaPlayer

    private val conversationAudioMessagePlayer by lazy {
        ConversationAudioMessagePlayer(
            context,
            mediaPlayer,
            getMessageAssetUseCase,
        )
    }

    init {
        MockKAnnotations.init(this, relaxed = true)
    }

    fun withSuccessFullAssetFetch() = apply {
        coEvery { getMessageAssetUseCase.invoke(any(), any()) } returns CompletableDeferred(
            MessageAssetResult.Success(
                decodedAssetPath = FakeKaliumFileSystem().selfUserAvatarPath(),
                assetSize = 0,
                assetName = "some-dummy-asset-name"
            )
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

    fun arrange() = this to conversationAudioMessagePlayer
}
