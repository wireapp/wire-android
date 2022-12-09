package com.wire.android.ui.home.conversations.messages

import androidx.paging.PagingData
import androidx.paging.map
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestMessage
import com.wire.android.ui.home.conversations.DownloadedAssetDialogVisibilityState
import com.wire.android.ui.home.conversations.mockUITextMessage
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.util.fileExtension
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ConversationMessagesViewModelTest {

    @Test
    fun `given an message ID, when downloading or fetching into internal storage, then should get message details by ID`() = runTest {
        val message = TestMessage.ASSET_MESSAGE
        val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
            .withGetMessageAssetUseCaseReturning("path".toPath(), 42L)
            .withGetMessageByIdReturning(message)
            .arrange()

        viewModel.downloadOrFetchAssetToInternalStorage(message.id)

        coVerify(exactly = 1) { arrangement.getMessageById(arrangement.conversationId, message.id) }
    }

    @Test
    fun `given an asset message, when opening it, then the file manager open function gets invoked and closes the dialog`() = runTest {
        // Given
        val messageId = "mocked-msg-id"
        val assetName = "mocked-asset-name.zip"
        val assetDataPath = "asset-data-path".toPath()
        val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulOpenAssetMessage(assetName, assetDataPath, 1L, messageId)
            .arrange()

        // When
        assert(viewModel.conversationViewState.downloadedAssetDialogState is DownloadedAssetDialogVisibilityState.Displayed)
        viewModel.onOpenFileWithExternalApp(assetDataPath, assetName.fileExtension())

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
            val assetDataPath = "asset-data-path".toPath()
            val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
                .withSuccessfulSaveAssetMessage(assetName, assetDataPath, 1L, messageId)
                .arrange()

            // When
            assert(viewModel.conversationViewState.downloadedAssetDialogState is DownloadedAssetDialogVisibilityState.Displayed)
            viewModel.onSaveFile(assetName, assetDataPath, 1L, messageId)

            // Then
            coVerify(exactly = 1) { arrangement.fileManager.saveToExternalStorage(any(), any(), any(), any()) }
            assert(viewModel.conversationViewState.downloadedAssetDialogState == DownloadedAssetDialogVisibilityState.Hidden)
        }

    @Test
    fun `given the PagingData is updated, when getting paging flow, then the update is propagated in the state`() = runTest {
        // Given
        val firstMessage = mockUITextMessage(userName = "firstUserName")
        val originalPagingData = PagingData.from(listOf(firstMessage))
        val secondMessage = mockUITextMessage(userName = "secondUserName")
        val updatedPagingData = PagingData.from(listOf(secondMessage))

        val (arrangement, viewModel) = ConversationMessagesViewModelArrangement().arrange()

        viewModel.conversationViewState.messages.test {
            arrangement.withPaginatedMessagesReturning(originalPagingData)
            awaitItem().map { it shouldBeEqualTo firstMessage }
            arrangement.withPaginatedMessagesReturning(updatedPagingData)
            awaitItem().map { it shouldBeEqualTo secondMessage }
        }
    }

    @Test
    fun `given a message and a reaction, when toggleReaction is called, then should call ToggleReactionUseCase`() = runTest {
        val (arrangement, viewModel) = ConversationMessagesViewModelArrangement().arrange()

        val messageId = "mID"
        val reaction = "🤌🏼"

        viewModel.toggleReaction(messageId, reaction)

        coVerify(exactly = 1) {
            arrangement.toggleReaction(arrangement.conversationId, messageId, reaction)
        }
    }

    @Test
    fun `given a message with failed decryption, when resetting the session, then should call ResetSessionUseCase`() = runTest {
        val userId = UserId("someID", "someDomain")
        val clientId = "someClientId"
        val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulResetSessionCall()
            .arrange()

        viewModel.onResetSession(userId, clientId)

        coVerify(exactly = 1) { arrangement.resetSession(any(), any(), any()) }
    }
}
