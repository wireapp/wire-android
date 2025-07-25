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

package com.wire.android.ui.home.conversations.messages

import androidx.paging.PagingData
import androidx.paging.map
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.framework.TestMessage
import com.wire.android.framework.TestMessage.GENERIC_ASSET_CONTENT
import com.wire.android.media.audiomessage.AudioMediaPlayingState
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer.MessageIdWrapper
import com.wire.android.media.audiomessage.PlayingAudioMessage
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.composer.mockUITextMessage
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogType
import com.wire.android.util.ui.UIText
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.GetConversationUnreadEventsCountUseCase
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import com.wire.android.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class ConversationMessagesViewModelTest {

    @Test
    fun `given an message ID, when downloading or fetching into internal storage, then should get message details by ID`() = runTest {
        val message = TestMessage.ASSET_MESSAGE
        val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withGetMessageAssetUseCaseReturning("path".toPath(), 42L)
            .withGetMessageByIdReturning(message)
            .arrange()

        viewModel.openOrFetchAsset(message.id)

        coVerify(exactly = 1) { arrangement.getMessageById(arrangement.conversationId, message.id) }
    }

    @Test
    fun `given an message ID and Wire Cell is Enabled, when downloading or fetching into internal storage, then download dialog is shown`() =
        runTest {
            val message = TestMessage.ASSET_MESSAGE
            val (_, viewModel) = ConversationMessagesViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withGetMessageAssetUseCaseReturning("path".toPath(), 42L)
                .withGetMessageByIdReturning(message)
                .withWireCellEnabled()
                .arrange()

            viewModel.openOrFetchAsset(message.id)

            val state = viewModel.conversationViewState

            advanceUntilIdle()

            assertTrue(state.downloadedAssetDialogState == DownloadedAssetDialogVisibilityState.Hidden)
        }

    @Test
    fun `given an message ID and Wire Cell is Disabled, when downloading or fetching into internal storage, then download dialog is shown`() =
        runTest {
            val message = TestMessage.ASSET_MESSAGE
            val (_, viewModel) = ConversationMessagesViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withGetMessageAssetUseCaseReturning("path".toPath(), 42L)
                .withGetMessageByIdReturning(message)
//                .withWireCellEnabled()
                .arrange()

            viewModel.openOrFetchAsset(message.id)

            val state = viewModel.conversationViewState

            advanceUntilIdle()

            assertTrue(state.downloadedAssetDialogState is DownloadedAssetDialogVisibilityState.Displayed)
        }

    @Test
    fun `given an asset message, when opening it, then the file manager open function gets invoked and closes the dialog`() = runTest {
        // Given
        val messageId = "mocked-msg-id"
        val assetName = "mocked-asset-name.zip"
        val assetDataPath = "asset-data-path".toPath()
        val assetMimeType = "application/zip"
        val assetSize = 8192L
        val message = TestMessage.ASSET_MESSAGE.copy(
            id = messageId,
            content = MessageContent.Asset(GENERIC_ASSET_CONTENT.copy(name = assetName, mimeType = assetMimeType, sizeInBytes = assetSize))
        )
        val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
            .withGetMessageByIdReturning(message)
            .withSuccessfulViewModelInit()
            .withGetMessageAssetUseCaseReturning(assetDataPath, assetSize)
            .withSuccessfulOpenAssetMessage(assetMimeType, assetName, assetDataPath, assetSize, messageId)
            .arrange()

        // When
        assert(viewModel.conversationViewState.downloadedAssetDialogState is DownloadedAssetDialogVisibilityState.Displayed)
        viewModel.downloadAndOpenAsset(messageId)

        // Then
        verify(exactly = 1) { arrangement.fileManager.openWithExternalApp(any(), any(), any()) }
        assert(viewModel.conversationViewState.downloadedAssetDialogState == DownloadedAssetDialogVisibilityState.Hidden)
    }

    @Test
    fun `given an asset message, when downloading to external storage, then the file manager downloads the asset and closes the dialog`() =
        runTest {
            // Given
            val messageId = "mocked-msg-id"
            val assetName = "mocked-asset-name.zip"
            val dataPath = "asset-data-path".toPath()
            val mimeType = "application/zip"
            val assetSize = 42L
            val message = TestMessage.ASSET_MESSAGE.copy(
                id = messageId,
                content = MessageContent.Asset(GENERIC_ASSET_CONTENT.copy(name = assetName, mimeType = mimeType, sizeInBytes = assetSize))
            )
            val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withGetMessageByIdReturning(message)
                .withGetMessageAssetUseCaseReturning(dataPath, assetSize)
                .withSuccessfulSaveAssetMessage(mimeType, assetName, dataPath, assetSize, messageId)
                .arrange()

            // When
            assert(viewModel.conversationViewState.downloadedAssetDialogState is DownloadedAssetDialogVisibilityState.Displayed)
            viewModel.downloadAssetExternally(messageId)

            // Then
            coVerify(exactly = 1) { arrangement.fileManager.saveToExternalStorage(any(), any(), any(), any(), any()) }
            assert(viewModel.conversationViewState.downloadedAssetDialogState == DownloadedAssetDialogVisibilityState.Hidden)
        }

    @Test
    fun `given the PagingData is updated, when getting paging flow, then the update is propagated in the state`() = runTest {
        // Given
        val firstMessage = mockUITextMessage(userName = "firstUserName")
        val originalPagingData = PagingData.from(listOf(firstMessage))
        val secondMessage = mockUITextMessage(userName = "secondUserName")
        val updatedPagingData = PagingData.from(listOf(secondMessage))

        val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .arrange()

        viewModel.conversationViewState.messages.test {
            arrangement.withPaginatedMessagesReturning(originalPagingData)
            awaitItem().map { it shouldBeEqualTo firstMessage }
            arrangement.withPaginatedMessagesReturning(updatedPagingData)
            awaitItem().map { it shouldBeEqualTo secondMessage }
        }
    }

    @Test
    fun `given a message and a reaction, when toggleReaction is called, then should call ToggleReactionUseCase`() = runTest {
        val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .arrange()

        val messageId = "mID"
        val reaction = "🤌🏼"

        viewModel.toggleReaction(messageId, reaction)

        coVerify(exactly = 1) {
            arrangement.toggleReaction(arrangement.conversationId, messageId, reaction)
        }
    }

    @Test
    fun `given getting UnreadEventsCount failed, then messages requested anyway`() = runTest {
        val (arrangement, _) = ConversationMessagesViewModelArrangement()
            .withConversationUnreadEventsCount(GetConversationUnreadEventsCountUseCase.Result.Failure(StorageFailure.DataNotFound))
            .withSuccessfulViewModelInit()
            .arrange()

        coVerify(exactly = 1) { arrangement.getMessagesForConversationUseCase(any(), 0) }
    }

    @Test
    fun `given getting UnreadEventsCount succeed, then messages requested with corresponding lastReadIndex`() = runTest {
        val (arrangement, _) = ConversationMessagesViewModelArrangement()
            .withConversationUnreadEventsCount(GetConversationUnreadEventsCountUseCase.Result.Success(12))
            .withSuccessfulViewModelInit()
            .arrange()

        coVerify(exactly = 1) { arrangement.getMessagesForConversationUseCase(any(), 12) }
    }

    @Test
    fun `given a message with failed decryption, when resetting the session, then should call ResetSessionUseCase`() = runTest {
        val userId = UserId("someID", "someDomain")
        val clientId = "someClientId"
        val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withResetSessionResult()
            .arrange()

        viewModel.onResetSession(userId, clientId)

        coVerify(exactly = 1) { arrangement.resetSession(any(), any(), any()) }
    }

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible for my message`() =
        runTest {
            // Given
            val (_, viewModel) = ConversationMessagesViewModelArrangement()
                .withSuccessfulViewModelInit()
                .arrange()

            // When
            viewModel.deleteMessageDialogState.show(DeleteMessageDialogState(true, "messageId", viewModel.conversationId))

            // Then
            viewModel.deleteMessageDialogState.isVisible shouldBeEqualTo true
            viewModel.deleteMessageDialogState.savedState shouldBeEqualTo DeleteMessageDialogState(
                DeleteMessageDialogType.ForEveryone, "messageId", viewModel.conversationId
            )
        }

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible for others message`() =
        runTest {
            // Given
            val (_, viewModel) = ConversationMessagesViewModelArrangement()
                .withSuccessfulViewModelInit()
                .arrange()

            // When
            viewModel.deleteMessageDialogState.show(DeleteMessageDialogState(false, "messageId", viewModel.conversationId))

            // Then
            viewModel.deleteMessageDialogState.isVisible shouldBeEqualTo true
            viewModel.deleteMessageDialogState.savedState shouldBeEqualTo DeleteMessageDialogState(
                DeleteMessageDialogType.ForYourself, "messageId", viewModel.conversationId
            )
        }

    @Test
    fun `validate deleteMessageDialogsState states when dialogs are dismissed`() {
        // Given
        val (_, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .arrange()

        // When
        viewModel.deleteMessageDialogState.dismiss()

        // Then
        viewModel.deleteMessageDialogState.isVisible shouldBeEqualTo false
    }

    @Test
    fun `given a failure, when deleting messages, then the error state is updated`() = runTest {
        // Given
        val (_, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withFailureOnDeletingMessages()
            .arrange()

        viewModel.infoMessage.test {

            // when
            expectNoEvents()
            viewModel.deleteMessage("messageId", true)

            // Then
            assertEquals(ConversationSnackbarMessages.ErrorDeletingMessage, awaitItem())
        }
    }

    @Test
    fun `given a failure, when deleting messages, then the delete dialog state is closed`() =
        runTest {
            // Given
            val (_, viewModel) = ConversationMessagesViewModelArrangement()
                .withFailureOnDeletingMessages()
                .withSuccessfulViewModelInit()
                .arrange()

            // When
            viewModel.deleteMessage("messageId", true)

            // Then
            viewModel.deleteMessageDialogState.isVisible shouldBeEqualTo false
        }

    @Test
    fun `given an message ID, when some Audio is played, then state contains it`() = runTest {
        val message = TestMessage.ASSET_MESSAGE
        val audioState = AudioState.DEFAULT.copy(
            audioMediaPlayingState = AudioMediaPlayingState.Playing,
            totalTimeInMs = AudioState.TotalTimeInMs.Known(10000),
            currentPositionInMs = 300
        )
        val playingAudioMessage = PlayingAudioMessage.Some(
            conversationId = message.conversationId,
            messageId = message.id,
            authorName = UIText.DynamicString("some name"),
            state = AudioState.DEFAULT.copy(currentPositionInMs = audioState.currentPositionInMs)
        )
        val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withPlayingAudioMessageFlow(flowOf(playingAudioMessage))
            .withObservableAudioMessagesState(
                flowOf(
                    mapOf(
                        MessageIdWrapper(message.conversationId, message.id) to audioState.copy(currentPositionInMs = 100),
                        MessageIdWrapper(message.conversationId, message.id) to audioState
                    )
                )
            )
            .arrange()

        advanceUntilIdle()

        assertEquals(playingAudioMessage, viewModel.conversationViewState.playingAudioMessage)
    }

    @Test
    fun `given an message ID, when no playing Audio message, then PlayingAudioMessage is None`() = runTest {
        val message = TestMessage.ASSET_MESSAGE
        val audioState = AudioState.DEFAULT.copy(
            audioMediaPlayingState = AudioMediaPlayingState.Stopped,
            totalTimeInMs = AudioState.TotalTimeInMs.Known(10000),
            currentPositionInMs = 300
        )
        val (_, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withPlayingAudioMessageFlow(flowOf(PlayingAudioMessage.None))
            .withObservableAudioMessagesState(flowOf(mapOf(MessageIdWrapper(message.conversationId, message.id) to audioState)))
            .arrange()

        advanceUntilIdle()

        assertEquals(PlayingAudioMessage.None, viewModel.conversationViewState.playingAudioMessage)
    }
}
