package com.wire.android.ui.home.conversations.messages

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.conversations.DownloadedAssetDialogVisibilityState
import com.wire.android.ui.home.conversations.mockConversationDetailsGroup
import com.wire.android.ui.home.conversations.mockUITextMessage
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.PlainId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.util.fileExtension
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ConversationMessagesViewModelTest {


    @Test
    fun `given an asset message, when opening it, then the file manager open function gets invoked and closes the dialog`() = runTest {
        // Given
        val messageId = "mocked-msg-id"
        val assetName = "mocked-asset-name.zip"
        val assetDataPath = "asset-data-path".toPath()
        val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
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
    fun `given message sent a user, when solving the message header, then the state should contain the user name`() = runTest {
        // Given
        val selfUserName = "self user"
        val messages = listOf(mockUITextMessage(userName = selfUserName))
        val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withMessagesUpdate(messages)
            .arrange()

        // When - Then
        assertEquals(
            selfUserName,
            viewModel.conversationViewState.messages.first().messageHeader.username.asString(arrangement.resources)
        )
    }

    @Test
    fun `given the sender is updated, when solving the message header, then the update is propagated in the state`() = runTest {
        // Given
        val firstUserName = "other user"
        val originalMessages = listOf(mockUITextMessage(userName = firstUserName))
        val secondUserName = "User changed their name"
        val updatedMessages = listOf(mockUITextMessage(userName = secondUserName))

        val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withMessagesUpdate(originalMessages)
            .arrange()

        // When - Then
        every { arrangement.uiText.asString(any()) } returns (firstUserName)
        assertEquals(
            firstUserName,
            viewModel.conversationViewState.messages.first().messageHeader.username.asString(arrangement.resources)
        )

        // When - Then
        every { arrangement.uiText.asString(any()) } returns (secondUserName)
        arrangement
            .withMessagesUpdate(updatedMessages)
            .arrange()

        assertEquals(
            secondUserName,
            viewModel.conversationViewState.messages.first().messageHeader.username.asString(arrangement.resources)
        )
    }

    @Test
    fun `given group conversation, when lastUnreadMessage is cleared, then correctly propagate it up to state`() =
        runTest {
            val groupDetails: ConversationDetails.Group = mockConversationDetailsGroup("Conversation Name Goes Here")
            val uiMessage = mockUITextMessage("commonId")

            val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withConversationDetailUpdate(groupDetails)
                .withMessagesUpdate(listOf(uiMessage))
                .arrange()

            val sendMessage = Message.Regular(
                id = "commonId",
                content = MessageContent.Text("some Text"),
                conversationId = QualifiedID("someValue", "someId"),
                date = "someDate",
                senderUserId = QualifiedID("someValue", "someId"),
                status = Message.Status.SENT,
                visibility = Message.Visibility.VISIBLE,
                senderClientId = PlainId(value = "someValue"),
                editStatus = Message.EditStatus.NotEdited
            )

            arrangement.conversationDetailsChannel.send(
                groupDetails.copy(lastUnreadMessage = sendMessage)
            )

            assert(viewModel.conversationViewState.lastUnreadMessage != null)
            assert(viewModel.conversationViewState.lastUnreadMessage!!.messageHeader.messageId == sendMessage.id)

            arrangement.conversationDetailsChannel.send(
                groupDetails.copy(lastUnreadMessage = null)
            )

            assert(viewModel.conversationViewState.lastUnreadMessage == null)
        }

    @Test
    fun `given group conversation, when new lastUnreadMessage arrive, then correctly propagate it up to state`() =
        runTest {
            val groupDetails: ConversationDetails.Group = mockConversationDetailsGroup("Conversation Name Goes Here")
            val uiMessage = mockUITextMessage("commonId")

            val (arrangement, viewModel) = ConversationMessagesViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withConversationDetailUpdate(groupDetails)
                .withMessagesUpdate(listOf(uiMessage))
                .arrange()

            val sendMessage = Message.Regular(
                id = "commonId",
                content = MessageContent.Text("some Text"),
                conversationId = QualifiedID("someValue", "someId"),
                date = "someDate",
                senderUserId = QualifiedID("someValue", "someId"),
                status = Message.Status.SENT,
                visibility = Message.Visibility.VISIBLE,
                senderClientId = PlainId(value = "someValue"),
                editStatus = Message.EditStatus.NotEdited
            )

            arrangement.conversationDetailsChannel.send(
                groupDetails.copy(lastUnreadMessage = null)
            )

            assert(viewModel.conversationViewState.lastUnreadMessage == null)

            arrangement.conversationDetailsChannel.send(
                groupDetails.copy(lastUnreadMessage = sendMessage)
            )

            assert(viewModel.conversationViewState.lastUnreadMessage != null)
            assert(viewModel.conversationViewState.lastUnreadMessage!!.messageHeader.messageId == sendMessage.id)
        }
}
