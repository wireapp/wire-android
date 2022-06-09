package com.wire.android.ui.home.conversations

import android.content.res.Resources
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestMessage
import com.wire.android.framework.TestUser
import com.wire.android.mapper.MessageMapper
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.ConversationViewModel.Companion.ASSET_SIZE_DEFAULT_LIMIT_BYTES
import com.wire.android.ui.home.conversations.ConversationViewModel.Companion.IMAGE_SIZE_LIMIT_BYTES
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageViewWrapper
import com.wire.android.util.FileManager
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.conversation.UserType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.SendAssetMessageResult
import com.wire.kalium.logic.feature.asset.SendAssetMessageUseCase
import com.wire.kalium.logic.feature.asset.SendImageMessageResult
import com.wire.kalium.logic.feature.asset.SendImageMessageUseCase
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageDownloadStatusUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveMemberDetailsByIdsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.GetRecentMessagesUseCase
import com.wire.kalium.logic.feature.message.MarkMessagesAsNotifiedUseCase
import com.wire.kalium.logic.feature.message.Result.Success
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
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
        val (_, viewModel) = ConversationsViewModelArrangement().arrange()

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
        val (_, viewModel) = ConversationsViewModelArrangement().arrange()

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
        val (_, viewModel) = ConversationsViewModelArrangement().arrange()

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
        val (_, viewModel) = ConversationsViewModelArrangement().arrange()

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
        val (_, viewModel) = ConversationsViewModelArrangement()
            .withConversationDetailUpdate(
                conversationDetails = oneToOneConversationDetails
            )
            .arrange()

        // When - Then
        assertEquals(oneToOneConversationDetails.otherUser.name, viewModel.conversationViewState.conversationName)
    }

    @Test
    fun `given a group conversation, when solving the conversation name, then the name of the conversation is used`() = runTest {
        // Given
        val groupConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val (_, viewModel) = ConversationsViewModelArrangement().withConversationDetailUpdate(conversationDetails = groupConversationDetails)
            .arrange()

        // When - Then
        assertEquals(groupConversationDetails.conversation.name, viewModel.conversationViewState.conversationName)
    }

    @Test
    fun `given the conversation name is updated, when solving the conversation name, then the state is updated accordingly`() = runTest {
        // Given
        val firstConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val secondConversationDetails = mockConversationDetailsGroup("Conversation Name Was Updated")
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withConversationDetailUpdate(
                conversationDetails = firstConversationDetails
            )
            .arrange()

        // When - Then
        assertEquals(firstConversationDetails.conversation.name, viewModel.conversationViewState.conversationName)

        // When - Then
        arrangement.withConversationDetailUpdate(conversationDetails = secondConversationDetails)
        assertEquals(secondConversationDetails.conversation.name, viewModel.conversationViewState.conversationName)
    }

    @Test
    fun `given message sent a user, when solving the message header, then the state should contain the user name`() = runTest {
        // Given
        val selfUserName = "self user"
        val messages = listOf(mockUITextMessage(selfUserName))
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withMessagesUpdate(messages)
            .arrange()

        // When - Then
        assertEquals(selfUserName, viewModel.conversationViewState.messages.first().messageHeader.username.asString(arrangement.resources))
    }

    @Test
    fun `given the sender is updated, when solving the message header, then the update is propagated in the state`() = runTest {
        // Given
        val firstUserName = "other user"
        val originalMessages = listOf(mockUITextMessage(firstUserName))
        val secondUserName = "User changed their name"
        val updatedMessages = listOf(mockUITextMessage(secondUserName))
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
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
        val (arrangement, viewModel) = ConversationsViewModelArrangement().withSuccessfulSendAttachmentMessage().arrange()
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
        val (arrangement, viewModel) = ConversationsViewModelArrangement().withSuccessfulSendAttachmentMessage().arrange()
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
        val (arrangement, viewModel) = ConversationsViewModelArrangement().withSuccessfulSendAttachmentMessage().arrange()
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
        val (_, viewModel) = ConversationsViewModelArrangement()
            .withConversationDetailUpdate(conversationDetails = conversationDetails)
            .arrange()
        val actualAvatar = viewModel.conversationViewState.conversationAvatar
        // When - Then
        assert(actualAvatar is ConversationAvatar.OneOne)
        assertEquals(otherUserAvatar, (actualAvatar as ConversationAvatar.OneOne).avatarAsset?.userAssetId)
    }

    @Test
    fun `given a user sends an image message larger than 15MB, when invoked, then sendImageMessageUseCase isn't called`() = runTest {
        // Given
        val (arrangement, viewModel) = ConversationsViewModelArrangement().withSuccessfulSendAttachmentMessage().arrange()
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
            val (arrangement, viewModel) = ConversationsViewModelArrangement().withSuccessfulSendAttachmentMessage().arrange()
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
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
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
            val (arrangement, viewModel) = ConversationsViewModelArrangement()
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
            val (arrangement, viewModel) = ConversationsViewModelArrangement()
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
