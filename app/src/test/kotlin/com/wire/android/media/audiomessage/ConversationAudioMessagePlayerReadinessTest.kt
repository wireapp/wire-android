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
package com.wire.android.media.audiomessage

import app.cash.turbine.test
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer.MessageIdWrapper
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toOkioPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

private val dispatcher = UnconfinedTestDispatcher()

class ConversationAudioMessagePlayerReadinessTest {
    @TempDir
    lateinit var tempDir: File

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun givenPendingAudioFetch_whenTogglingPlayback_thenWaitForAssetBeforeStarting(fromNotification: Boolean) = runTest(dispatcher) {
        val audioMessageId = "some-dummy-message-id"
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val messageIdWrapper = MessageIdWrapper(conversationId, audioMessageId)
        val pendingAsset = CompletableDeferred<MessageAssetResult>()
        val (arrangement, conversationAudioMessagePlayer) = Arrangement(tempDir)
            .withCurrentSession()
            .arrange()
        coEvery { arrangement.getAssetMessage(conversationId, audioMessageId) } returns pendingAsset

        conversationAudioMessagePlayer.observableAudioMessagesState.test {
            awaitItem()
            val playbackJob = backgroundScope.launch(dispatcher) {
                conversationAudioMessagePlayer.playAudio(conversationId, audioMessageId)
            }
            assertEquals(AudioMediaPlayingState.Fetching, awaitItem()[messageIdWrapper]?.audioMediaPlayingState)

            if (fromNotification) {
                conversationAudioMessagePlayer.resumeOrPauseCurrentAudioMessage()
            } else {
                conversationAudioMessagePlayer.playAudio(conversationId, audioMessageId)
            }

            verify(exactly = 0) { arrangement.mediaPlayer.start() }
            verify(exactly = 0) { arrangement.mediaPlayer.pause() }
            verify(exactly = 0) { arrangement.audioFocusHelper.request() }
            expectNoEvents()

            val assetFile = File(tempDir, "pending-audio").apply { createNewFile() }
            pendingAsset.complete(
                MessageAssetResult.Success(assetFile.toOkioPath(), 0, assetFile.name)
            )
            playbackJob.join()

            assertEquals(AudioMediaPlayingState.Playing, expectMostRecentItem()[messageIdWrapper]?.audioMediaPlayingState)
            verify(exactly = 1) { arrangement.mediaPlayer.start() }
            verify(exactly = 1) { arrangement.audioFocusHelper.request() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenPreparedAudio_whenTogglingNotificationPlayback_thenPauseAndResume() = runTest(dispatcher) {
        val audioMessageId = "some-dummy-message-id"
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val messageIdWrapper = MessageIdWrapper(conversationId, audioMessageId)
        val (arrangement, conversationAudioMessagePlayer) = Arrangement(tempDir)
            .withCurrentSession()
            .withSuccessfulAssetFetch()
            .arrange()
        var isPlaying = false
        every { arrangement.mediaPlayer.isPlaying } answers { isPlaying }
        every { arrangement.mediaPlayer.start() } answers { isPlaying = true }
        every { arrangement.mediaPlayer.pause() } answers { isPlaying = false }

        conversationAudioMessagePlayer.observableAudioMessagesState.test {
            awaitItem()
            conversationAudioMessagePlayer.playAudio(conversationId, audioMessageId)
            assertEquals(AudioMediaPlayingState.Playing, expectMostRecentItem()[messageIdWrapper]?.audioMediaPlayingState)

            conversationAudioMessagePlayer.resumeOrPauseCurrentAudioMessage()
            assertEquals(AudioMediaPlayingState.Paused, awaitItem()[messageIdWrapper]?.audioMediaPlayingState)

            conversationAudioMessagePlayer.resumeOrPauseCurrentAudioMessage()
            assertEquals(AudioMediaPlayingState.Playing, expectMostRecentItem()[messageIdWrapper]?.audioMediaPlayingState)
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { arrangement.mediaPlayer.pause() }
        verify(exactly = 2) { arrangement.mediaPlayer.start() }
    }

    @Test
    fun givenFetchedAudioBeforePreparing_whenTogglingNotificationPlayback_thenWaitForPreparation() = runTest(dispatcher) {
        val audioMessageId = "some-dummy-message-id"
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val messageIdWrapper = MessageIdWrapper(conversationId, audioMessageId)
        val (arrangement, conversationAudioMessagePlayer) = Arrangement(tempDir)
            .withCurrentSession()
            .withSuccessfulAssetFetch()
            .arrange()
        var toggledBeforePreparation = false

        backgroundScope.launch(dispatcher) {
            conversationAudioMessagePlayer.observableAudioMessagesState.collect { states ->
                if (states[messageIdWrapper]?.audioMediaPlayingState == AudioMediaPlayingState.SuccessfulFetching) {
                    verify(exactly = 0) { arrangement.mediaPlayer.prepare() }
                    toggledBeforePreparation = true
                    conversationAudioMessagePlayer.resumeOrPauseCurrentAudioMessage()
                    verify(exactly = 0) { arrangement.mediaPlayer.start() }
                    verify(exactly = 0) { arrangement.audioFocusHelper.request() }
                }
            }
        }

        conversationAudioMessagePlayer.playAudio(conversationId, audioMessageId)

        assertTrue(toggledBeforePreparation)
        verify(exactly = 1) { arrangement.mediaPlayer.start() }
    }
}
