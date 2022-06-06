package com.wire.android.ui.home.conversations

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.conversations.ConversationViewModel.Companion.ASSET_SIZE_DEFAULT_LIMIT_BYTES
import com.wire.android.ui.home.conversations.ConversationViewModel.Companion.IMAGE_SIZE_LIMIT_BYTES
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.UserId
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.internal.assertFalse
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ConversationsViewModelTest {

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible for my message`() {
        // Given
        val (_, viewModel) = Arrangement().arrange()

        // When
        viewModel.showDeleteMessageDialog("", true)

        // Then
        viewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden,
            forEveryone = DeleteMessageDialogActiveState.Visible("", viewModel.conversationId)
        )
    }

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible for others message`() {
        // Given
        val (_, viewModel) = Arrangement().arrange()

        // When
        viewModel.showDeleteMessageDialog("", false)

        // Then
        viewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Visible("", viewModel.conversationId),
            forEveryone = DeleteMessageDialogActiveState.Hidden
        )
    }

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageForYourselfDialog is visible`() {
        // Given
        val (_, viewModel) = Arrangement().arrange()

        // When
        viewModel.showDeleteMessageForYourselfDialog("")

        // Then
        viewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Visible("", viewModel.conversationId),
            forEveryone = DeleteMessageDialogActiveState.Hidden
        )
    }

    @Test
    fun `validate deleteMessageDialogsState states when dialogs are dismissed`() {
        // Given
        val (_, viewModel) = Arrangement().arrange()

        // When
        viewModel.onDeleteDialogDismissed()

        // Then
        viewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden, forEveryone = DeleteMessageDialogActiveState.Hidden
        )
    }

    @Test
    fun `given a 1 on 1 conversation, when solving the conversation name, then the name of the other user is used`() = runTest {
        // Given
        val oneToOneConversationDetails = withMockConversationDetailsOneOnOne("Other User Name Goes Here")
        val (_, viewModel) = Arrangement().withConversationDetailUpdate(conversationDetails = oneToOneConversationDetails).arrange()

        // When - Then
        assertEquals(oneToOneConversationDetails.otherUser.name, viewModel.conversationViewState.conversationName)
    }

    @Test
    fun `given a group conversation, when solving the conversation name, then the name of the conversation is used`() = runTest {
        // Given
        val groupConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val (_, viewModel) = Arrangement().withConversationDetailUpdate(conversationDetails = groupConversationDetails).arrange()

        // When - Then
        assertEquals( groupConversationDetails.conversation.name, viewModel.conversationViewState.conversationName)
    }

    @Test
    fun `given the conversation name is updated, when solving the conversation name, then the state is updated accordingly`() = runTest {
        // Given
        val firstConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val secondConversationDetails = mockConversationDetailsGroup("Conversation Name Was Updated")
        val (arrangement, viewModel) = Arrangement().withConversationDetailUpdate(conversationDetails = firstConversationDetails).arrange()

        // When - Then
        assertEquals(firstConversationDetails.conversation.name, viewModel.conversationViewState.conversationName)

        // When - Then
        arrangement.withConversationDetailUpdate(conversationDetails = secondConversationDetails)
        assertEquals(secondConversationDetails.conversation.name, viewModel.conversationViewState.conversationName)
    }
    @Test
    fun `given message sent by self user, when solving the message header, then the state should contain the self user name`() = runTest {
        // Given
        val selfUserName = "self user"
        val messages = listOf(mockUIMessage(selfUserName))
        val (arrangement, viewModel) = Arrangement()
            .withMessagesUpdate(messages)
            .arrange()

        // When - Then
        every { arrangement.uiText.asString(any()) } returns (selfUserName)
        assertEquals(selfUserName, viewModel.conversationViewState.messages.first().messageHeader.username.asString(arrangement.context))
    }

    @Test
    fun `given message sent by another user, when solving the message header, then the state should contain that user name`() = runTest {
        // Given
        val otherUserName = "other user"
        val messages = listOf(mockUIMessage(otherUserName))
        val (arrangement, viewModel) = Arrangement()
            .withMessagesUpdate(messages)
            .arrange()

        // When - Then
        every { arrangement.uiText.asString(any()) } returns (otherUserName)
        assertEquals(otherUserName, viewModel.conversationViewState.messages.first().messageHeader.username.asString(arrangement.context))
    }

    @Test
    fun `given the sender is updated, when solving the message header, then the update is propagated in the state`() = runTest {
        // Given
        val firstUserName = "other user"
        val originalMessages = listOf(mockUIMessage(firstUserName))
        val secondUserName = "User changed their name"
        val updatedMessages = listOf(mockUIMessage(secondUserName))
        val (arrangement, viewModel) = Arrangement()
            .withMessagesUpdate(originalMessages)
            .arrange()

        // When - Then
        every { arrangement.uiText.asString(any()) } returns (firstUserName)
        assertEquals(firstUserName, viewModel.conversationViewState.messages.first().messageHeader.username.asString(arrangement.context))

        // When - Then
        every { arrangement.uiText.asString(any()) } returns (secondUserName)
        arrangement
            .withMessagesUpdate(updatedMessages)
            .arrange()

        assertEquals(secondUserName, viewModel.conversationViewState.messages.first().messageHeader.username.asString(arrangement.context))
    }

    @Test
    fun `given the user sends an asset message, when invoked, then sendAssetMessageUseCase gets called`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement().withSuccessfulSendAttachmentMessage().arrange()
        val mockedAttachment = AttachmentBundle(
            "file/x-zip", "Mocked asset data".toByteArray(), "mocked_file.zip", AttachmentType.GENERIC_FILE
        )

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        // Then
        coVerify(exactly = 1) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any()) }
    }

    @Test
    fun `given the user sends an image message, when invoked, then sendImageMessageUseCase gets called`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement().withSuccessfulSendAttachmentMessage().arrange()
        val mockedAttachment = AttachmentBundle(
            "image/jpeg", "Mocked asset data".toByteArray(), "mocked_image.jpeg", AttachmentType.IMAGE
        )

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        // Then
        coVerify(exactly = 1) { arrangement.sendImageMessage.invoke(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given the user picks a null attachment, when invoking sendAttachmentMessage, no use case gets called`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement().withSuccessfulSendAttachmentMessage().arrange()
        val mockedAttachment = null

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any()) }
        coVerify(inverse = true) { arrangement.sendImageMessage.invoke(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given a 1 on 1 conversation, when solving the conversation avatar, then the avatar of the other user is used`() = runTest {
        // Given
        val conversationDetails = withMockConversationDetailsOneOnOne("", "userAssetId")
        val otherUserAvatar = conversationDetails.otherUser.previewPicture
        val (_, viewModel) = Arrangement().withConversationDetailUpdate(conversationDetails = conversationDetails).arrange()
        val actualAvatar = viewModel.conversationViewState.conversationAvatar
        // When - Then
        assert(actualAvatar is ConversationAvatar.OneOne)
        assertEquals(otherUserAvatar, (actualAvatar as ConversationAvatar.OneOne).avatarAsset?.userAssetId)
    }

    @Test
    fun `given a user sends an image message larger than 15MB, when invoked, then sendImageMessageUseCase isn't called`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement().withSuccessfulSendAttachmentMessage().arrange()
        val mockedAttachment = AttachmentBundle(
            "image/jpeg", ByteArray(IMAGE_SIZE_LIMIT_BYTES + 1), "mocked_image.jpeg", AttachmentType.IMAGE
        )

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        // Then
        coVerify(inverse = true) { arrangement.sendImageMessage.invoke(any(), any(), any(), any(), any()) }
        assert(viewModel.conversationViewState.onSnackbarMessage is ConversationSnackbarMessages.ErrorMaxImageSize)
    }

    @Test
    fun `given that a free user sends an asset message larger than 25MB, when invoked, then sendAssetMessageUseCase isn't called`() =
        runTest {
            // Given
            val (arrangement, viewModel) = Arrangement().withSuccessfulSendAttachmentMessage().arrange()
            val mockedAttachment = AttachmentBundle(
                "file/x-zip", ByteArray(ASSET_SIZE_DEFAULT_LIMIT_BYTES + 1), "mocked_asset.jpeg", AttachmentType.GENERIC_FILE
            )

            // When
            viewModel.sendAttachmentMessage(mockedAttachment)

            // Then
            coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any()) }
            assert(viewModel.conversationViewState.onSnackbarMessage is ConversationSnackbarMessages.ErrorMaxAssetSize)
        }

    @Test
    fun `given that a team user sends an asset message larger than 25MB, when invoked, then sendAssetMessageUseCase is called`() = runTest {
        // Given
        val userTeam = Team("mocked-team-id", "mocked-team-name")
        val (arrangement, viewModel) = Arrangement()
            .withSuccessfulSendAttachmentMessage()
            .withTeamUser(userTeam)
            .arrange()
        val mockedAttachment = AttachmentBundle(
            "file/x-zip", ByteArray(ASSET_SIZE_DEFAULT_LIMIT_BYTES + 1), "mocked_asset.jpeg", AttachmentType.GENERIC_FILE
        )

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        // Then
        coVerify(exactly = 1) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any()) }
        assertFalse(viewModel.conversationViewState.onSnackbarMessage != null)
    }

    @Test
    fun `given an asset message, when downloading to external storage, then the file manager downloads the asset and closes the dialog`() =
        runTest {
            // Given
            val messageId = "mocked-msg-id"
            val assetName = "mocked-asset"
            val assetData = assetName.toByteArray()
            val (arrangement, viewModel) = Arrangement()
                .withSuccessfulSaveAssetMessage(assetName, assetData, messageId)
                .arrange()

            // When
            assert(viewModel.conversationViewState.downloadedAssetDialogState is DownloadedAssetDialogVisibilityState.Displayed)
            viewModel.onSaveFile(assetName, assetData, messageId)

            // Then
            coVerify(exactly = 1) { arrangement.fileManager.saveToExternalStorage(any(), any(), any()) }
            assert(viewModel.conversationViewState.downloadedAssetDialogState == DownloadedAssetDialogVisibilityState.Hidden)
        }

    @Test
    fun `given an asset message, when opening it, then the file manager open function gets invoked and closes the dialog`() =
        runTest {
            // Given
            val messageId = "mocked-msg-id"
            val assetName = "mocked-asset"
            val assetData = assetName.toByteArray()
            val (arrangement, viewModel) = Arrangement()
                .withSuccessfulOpenAssetMessage(assetName, assetData, messageId)
                .arrange()

            // When
            assert(viewModel.conversationViewState.downloadedAssetDialogState is DownloadedAssetDialogVisibilityState.Displayed)
            viewModel.onOpenFileWithExternalApp(assetName, assetData)

            // Then
            verify(exactly = 1) { arrangement.fileManager.openWithExternalApp(any(), any(), any()) }
            assert(viewModel.conversationViewState.downloadedAssetDialogState == DownloadedAssetDialogVisibilityState.Hidden)
        }

}
