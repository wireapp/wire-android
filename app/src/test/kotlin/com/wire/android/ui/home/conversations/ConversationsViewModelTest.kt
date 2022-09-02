package com.wire.android.ui.home.conversations

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.ui.home.conversations.ConversationViewModel.Companion.ASSET_SIZE_DEFAULT_LIMIT_BYTES
import com.wire.android.ui.home.conversations.ConversationViewModel.Companion.IMAGE_SIZE_LIMIT_BYTES
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsState
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.util.EMPTY
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.PlainId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.util.fileExtension
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.internal.assertFalse
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ConversationsViewModelTest {

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible for my message`() = runTest {
        // Given
        val (_, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .arrange()

        // When
        viewModel.showDeleteMessageDialog("", true)

        // Then
        viewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden,
            forEveryone = DeleteMessageDialogActiveState.Visible("", viewModel.conversationId)
        )
    }

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible for others message`() = runTest {
        // Given
        val (_, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .arrange()

        // When
        viewModel.showDeleteMessageDialog("", false)

        // Then
        viewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Visible("", viewModel.conversationId),
            forEveryone = DeleteMessageDialogActiveState.Hidden
        )
    }

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageForYourselfDialog is visible`() = runTest {
        // Given
        val (_, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .arrange()

        // When
        viewModel.deleteMessageHelper.showDeleteMessageForYourselfDialog("")

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
        viewModel.deleteMessageHelper.onDeleteDialogDismissed()

        // Then
        viewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden, forEveryone = DeleteMessageDialogActiveState.Hidden
        )
    }

    @Test
    fun `given a failure, when deleting messages, then the error state is updated`() = runTest {
        // Given
        val (_, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withFailureOnDeletingMessages().arrange()

        viewModel.conversationViewState
        // When
        viewModel.deleteMessageHelper.onDeleteMessage("messageId", true)

        // Then
        assert(viewModel.conversationViewState.onSnackbarMessage is ConversationSnackbarMessages.ErrorDeletingMessage)
    }

    @Test
    fun `given a failure, when deleting messages, then the delete dialog state is closed`() = runTest {
        // Given
        val (_, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withFailureOnDeletingMessages()
            .arrange()

        viewModel.conversationViewState
        // When
        viewModel.deleteMessageHelper.onDeleteMessage("messageId", true)

        // Then
        val expectedState = DeleteMessageDialogsState.States(
            DeleteMessageDialogActiveState.Hidden,
            DeleteMessageDialogActiveState.Hidden
        )
        assertEquals(expectedState, viewModel.deleteMessageDialogsState)
    }

    @Test
    fun `given a 1 on 1 conversation, when solving the conversation name, then the name of the other user is used`() = runTest {
        // Given
        val oneToOneConversationDetails = withMockConversationDetailsOneOnOne("Other User Name Goes Here")
        val (_, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withConversationDetailUpdate(
                conversationDetails = oneToOneConversationDetails
            )
            .arrange()

        // When - Then
        assert(viewModel.conversationViewState.conversationName is UIText.DynamicString)
        assertEquals(
            oneToOneConversationDetails.otherUser.name,
            (viewModel.conversationViewState.conversationName as UIText.DynamicString).value
        )
    }

    @Test
    fun `given a 1 on 1 conversation, when solving the conversation name, then unavailable user is used`() = runTest {
        // Given
        val oneToOneConversationDetails = withMockConversationDetailsOneOnOne(senderName = "", unavailable = true)
        val (_, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withConversationDetailUpdate(
                conversationDetails = oneToOneConversationDetails
            )
            .arrange()

        // When - Then
        assert(viewModel.conversationViewState.conversationName is UIText.StringResource)
    }


    @Test
    fun `given a group conversation, when solving the conversation name, then the name of the conversation is used`() = runTest {
        // Given
        val groupConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val (_, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withConversationDetailUpdate(conversationDetails = groupConversationDetails)
            .arrange()

        // When - Then
        assert(viewModel.conversationViewState.conversationName is UIText.DynamicString)
        assertEquals(
            groupConversationDetails.conversation.name,
            (viewModel.conversationViewState.conversationName as UIText.DynamicString).value
        )
    }

    @Test
    fun `given the conversation name is updated, when solving the conversation name, then the state is updated accordingly`() = runTest {
        // Given
        val firstConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val secondConversationDetails = mockConversationDetailsGroup("Conversation Name Was Updated")
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withConversationDetailUpdate(
                conversationDetails = firstConversationDetails
            )
            .arrange()

        // When - Then
        assert(viewModel.conversationViewState.conversationName is UIText.DynamicString)
        assertEquals(
            firstConversationDetails.conversation.name,
            (viewModel.conversationViewState.conversationName as UIText.DynamicString).value
        )

        // When - Then
        arrangement.withConversationDetailUpdate(conversationDetails = secondConversationDetails)
        assert(viewModel.conversationViewState.conversationName is UIText.DynamicString)
        assertEquals(
            secondConversationDetails.conversation.name,
            (viewModel.conversationViewState.conversationName as UIText.DynamicString).value
        )
    }

    @Test
    fun `given the initial state, when solving the conversation name before the data is received, the name should be an empty string`() =
        runTest {
            // Given
            val (_, viewModel) = ConversationsViewModelArrangement()
                .withSuccessfulViewModelInit()
                .arrange()

            // When - Then
            assert(viewModel.conversationViewState.conversationName is UIText.DynamicString)
            assertEquals(String.EMPTY, (viewModel.conversationViewState.conversationName as UIText.DynamicString).value)
        }

    @Test
    fun `given a 1 on 1 conversation, when the user is deleted, then the name of the conversation should be a string resource`() = runTest {
        // Given
        val oneToOneConversationDetails = withMockConversationDetailsOneOnOne("")
        val (_, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withConversationDetailUpdate(
                conversationDetails = oneToOneConversationDetails
            )
            .arrange()

        // When - Then
        assert(viewModel.conversationViewState.conversationName is UIText.StringResource)
    }

    @Test
    fun `given message sent a user, when solving the message header, then the state should contain the user name`() = runTest {
        // Given
        val selfUserName = "self user"
        val messages = listOf(mockUITextMessage(selfUserName))
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
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
        val originalMessages = listOf(mockUITextMessage(firstUserName))
        val secondUserName = "User changed their name"
        val updatedMessages = listOf(mockUITextMessage(secondUserName))
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
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
    fun `given the user sends an asset message, when invoked, then sendAssetMessageUseCase gets called`() = runTest {
        // Given
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendAttachmentMessage()
            .arrange()
        val mockedAttachment = AttachmentBundle(
            "file/x-zip", "Mocked-data-path".toPath(), 1L, "mocked_file.zip", AttachmentType.GENERIC_FILE
        )

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        // Then
        coVerify(exactly = 1) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given the user sends an image message, when invoked, then sendAssetMessageUseCase gets called`() = runTest {
        // Given
        val assetPath = "mocked-asset-data-path".toPath()
        val assetContent = "some-dummy-image".toByteArray()
        val assetName = "mocked_image.jpeg"
        val assetSize = assetContent.size.toLong()
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withStoredAsset(assetPath, assetContent)
            .withSuccessfulSendAttachmentMessage()
            .arrange()
        val mockedAttachment = AttachmentBundle(
            "image/jpeg", assetPath, assetSize, assetName, AttachmentType.IMAGE
        )

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        // Then
        coVerify(exactly = 1) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given the user picks a null attachment, when invoking sendAttachmentMessage, no use case gets called`() = runTest {
        // Given
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendAttachmentMessage()
            .arrange()
        val mockedAttachment = null

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given a 1 on 1 conversation, when solving the conversation avatar, then the avatar of the other user is used`() = runTest {
        // Given
        val conversationDetails = withMockConversationDetailsOneOnOne("", ConversationId("userAssetId", "domain"))
        val otherUserAvatar = conversationDetails.otherUser.previewPicture
        val (_, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withConversationDetailUpdate(conversationDetails = conversationDetails)
            .arrange()
        val actualAvatar = viewModel.conversationViewState.conversationAvatar
        // When - Then
        assert(actualAvatar is ConversationAvatar.OneOne)
        assertEquals(otherUserAvatar, (actualAvatar as ConversationAvatar.OneOne).avatarAsset?.userAssetId)
    }

    @Test
    fun `given a user sends an image message larger than 15MB, when invoked, then sendAssetMessageUseCase isn't called`() = runTest {
        // Given
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendAttachmentMessage()
            .arrange()
        val mockedAttachment = AttachmentBundle(
            "image/jpeg", "some-data-path".toPath(), IMAGE_SIZE_LIMIT_BYTES + 1L, "mocked_image.jpeg", AttachmentType.IMAGE
        )

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        // Then
        coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
        assert(viewModel.conversationViewState.onSnackbarMessage is ConversationSnackbarMessages.ErrorMaxImageSize)
    }

    @Test
    fun `given that a free user sends an asset message larger than 25MB, when invoked, then sendAssetMessageUseCase isn't called`() =
        runTest {
            // Given
            val (arrangement, viewModel) = ConversationsViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .arrange()
            val mockedAttachment = AttachmentBundle(
                "file/x-zip",
                "some-data-path".toPath(),
                ASSET_SIZE_DEFAULT_LIMIT_BYTES + 1L,
                "mocked_asset.jpeg",
                AttachmentType.GENERIC_FILE
            )

            // When
            viewModel.sendAttachmentMessage(mockedAttachment)

            // Then
            coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
            assert(viewModel.conversationViewState.onSnackbarMessage is ConversationSnackbarMessages.ErrorMaxAssetSize)
        }

    @Test
    fun `given that a team user sends an asset message larger than 25MB, when invoked, then sendAssetMessageUseCase is called`() = runTest {
        // Given
        val userTeam = Team("mocked-team-id", "mocked-team-name")
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendAttachmentMessage()
            .withTeamUser(userTeam)
            .arrange()
        val mockedAttachment = AttachmentBundle(
            "file/x-zip", "some-data-path".toPath(), ASSET_SIZE_DEFAULT_LIMIT_BYTES + 1L, "mocked_asset.jpeg", AttachmentType.GENERIC_FILE
        )

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        // Then
        coVerify(exactly = 1) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
        assertFalse(viewModel.conversationViewState.onSnackbarMessage != null)
    }

    @Test
    fun `given an asset message, when downloading to external storage, then the file manager downloads the asset and closes the dialog`() =
        runTest {
            // Given
            val messageId = "mocked-msg-id"
            val assetName = "mocked-asset-name.zip"
            val assetDataPath = "asset-data-path".toPath()
            val (arrangement, viewModel) = ConversationsViewModelArrangement()
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
    fun `given an asset message, when opening it, then the file manager open function gets invoked and closes the dialog`() = runTest {
        // Given
        val messageId = "mocked-msg-id"
        val assetName = "mocked-asset-name.zip"
        val assetDataPath = "asset-data-path".toPath()
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
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
    fun `given self user 1on1 message, when clicking on avatar, then open self profile`() = runTest {
        // Given
        val oneOneDetails = withMockConversationDetailsOneOnOne("Other User Name Goes Here")
        val messageSource = MessageSource.Self
        val userId = UserId("id", "domain")
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withConversationDetailUpdate(oneOneDetails)
            .arrange()
        // When
        viewModel.navigateToProfile(messageSource, userId)
        // Then
        coVerify(exactly = 1) {
            arrangement.navigationManager.navigate(NavigationCommand(NavigationItem.SelfUserProfile.getRouteWithArgs()))
        }
    }

    @Test
    fun `given self user group message, when clicking on avatar, then open self profile`() = runTest {
        // Given
        val groupDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val messageSource = MessageSource.Self
        val userId = UserId("id", "domain")
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withConversationDetailUpdate(groupDetails)
            .arrange()
        // When
        viewModel.navigateToProfile(messageSource, userId)
        // Then
        coVerify(exactly = 1) {
            arrangement.navigationManager.navigate(NavigationCommand(NavigationItem.SelfUserProfile.getRouteWithArgs()))
        }
    }

    @Test
    fun `given other user 1on1 message, when clicking on avatar, then open other user profile without group data`() = runTest {
        // Given
        val oneOneDetails: ConversationDetails.OneOne = withMockConversationDetailsOneOnOne("Other User Name Goes Here")
        val messageSource = MessageSource.OtherUser
        val userId = UserId("id", "domain")
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withConversationDetailUpdate(oneOneDetails)
            .arrange()
        // When
        viewModel.navigateToProfile(messageSource, userId)
        // Then
        coVerify(exactly = 1) {
            arrangement.navigationManager.navigate(NavigationCommand(NavigationItem.OtherUserProfile.getRouteWithArgs(listOf(userId))))
        }
    }

    @Test
    fun `given other user group message, when clicking on avatar, then open other user profile with group data`() = runTest {
        // Given
        val groupDetails: ConversationDetails.Group = mockConversationDetailsGroup("Conversation Name Goes Here")
        val messageSource = MessageSource.OtherUser
        val userId = UserId("id", "domain")
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withConversationDetailUpdate(groupDetails)
            .arrange()
        // When
        viewModel.navigateToProfile(messageSource, userId)
        // Then
        coVerify(exactly = 1) {
            arrangement.navigationManager.navigate(
                NavigationCommand(NavigationItem.OtherUserProfile.getRouteWithArgs(listOf(userId, arrangement.conversationId)))
            )
        }
    }

    @Test
    fun `a`() = runTest {
        val groupDetails: ConversationDetails.Group = mockConversationDetailsGroup("Conversation Name Goes Here")
        val uiMessage = mockUITextMessage("some name")

        val (arrangement, viewModel) = ConversationsViewModelArrangement()
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

        arrangement.conversationDetailsChannel.send(
            groupDetails.copy(lastUnreadMessage = null)
        )
    }
}
