package com.wire.android.ui.home.conversations

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mapper.MessageMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.ConversationViewModel.Companion.ASSET_SIZE_DEFAULT_LIMIT_BYTES
import com.wire.android.ui.home.conversations.ConversationViewModel.Companion.IMAGE_SIZE_LIMIT_BYTES
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.FileManager
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.conversation.UserType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent.Text
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.SendAssetMessageResult
import com.wire.kalium.logic.feature.asset.SendAssetMessageUseCase
import com.wire.kalium.logic.feature.asset.SendImageMessageResult
import com.wire.kalium.logic.feature.asset.SendImageMessageUseCase
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageDownloadStatusUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
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
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
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
        val conversationDetails = withMockConversationDetailsOneOnOne("Other User Name Goes Here")
        val otherUserName = conversationDetails.otherUser.name
        val (_, viewModel) = Arrangement().withChannelUpdates(conversationDetails = conversationDetails).arrange()

        // When - Then
        assertEquals(otherUserName, viewModel.conversationViewState.conversationName)
    }

    @Test
    fun `given a group conversation, when solving the conversation name, then the name of the conversation is used`() = runTest {
        // Given
        val conversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val conversationName = conversationDetails.conversation.name
        val (_, viewModel) = Arrangement().withChannelUpdates(conversationDetails = conversationDetails).arrange()

        // When - Then
        assertEquals(conversationName, viewModel.conversationViewState.conversationName)
    }

    @Test
    fun `given the conversation name is updated, when solving the conversation name, then the state is updated accordingly`() = runTest {
        // Given
        val firstConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val secondConversationDetails = mockConversationDetailsGroup("Conversation Name Was Updated")
        val (arrangement, viewModel) = Arrangement().withChannelUpdates(conversationDetails = firstConversationDetails).arrange()

        // When - Then
        assertEquals(firstConversationDetails.conversation.name, viewModel.conversationViewState.conversationName)

        // When - Then
        arrangement.withChannelUpdates(conversationDetails = secondConversationDetails)
        assertEquals(secondConversationDetails.conversation.name, viewModel.conversationViewState.conversationName)
    }

    @Test
    fun `given message sent by self user, when solving the message header, then the state should contain the self user name`() = runTest {
        // Given
        val senderId = UserId("value", "domain")
        val messages = listOf(mockedMessage(senderId = senderId))
        val selfUserName = "self user"
        val selfMember = mockSelfUserDetails(selfUserName, senderId)
        val (arrangement, viewModel) = Arrangement().withChannelUpdates(messages, listOf(selfMember)).arrange()

        // When - Then
        every { arrangement.uiText.asString(any()) } returns (selfUserName)
        assertEquals(selfUserName, viewModel.conversationViewState.messages.first().messageHeader.username.asString(arrangement.context))
    }

    @Test
    fun `given message sent by another user, when solving the message header, then the state should contain that user name`() = runTest {
        // Given
        val senderId = UserId("value", "domain")
        val messages = listOf(mockedMessage(senderId = senderId))
        val otherUserName = "other user"


        val otherMember = mockOtherUserDetails(otherUserName, senderId)
        val (arrangement, viewModel) = Arrangement().withChannelUpdates(messages, listOf(otherMember)).arrange()

        // When - Then
        every { arrangement.uiText.asString(any()) } returns (otherUserName)
        assertEquals(otherUserName, viewModel.conversationViewState.messages.first().messageHeader.username.asString(arrangement.context))
    }

    @Test
    fun `given the sender is updated, when solving the message header, then the update is propagated in the state`() = runTest {
        // Given
        val senderId = UserId("value", "domain")
        val messages = listOf(mockedMessage(senderId))
        val firstUserName = "other user"
        val secondUserName = "User changed their name"
        val (arrangement, viewModel) = Arrangement().withChannelUpdates(messages, listOf(mockOtherUserDetails(firstUserName, senderId)))
            .arrange()

        // When - Then
        every { arrangement.uiText.asString(any()) } returns (firstUserName)
        assertEquals(firstUserName, viewModel.conversationViewState.messages.first().messageHeader.username.asString(arrangement.context))

        // When - Then
        every { arrangement.uiText.asString(any()) } returns (secondUserName)
        arrangement.withChannelUpdates(messages, listOf(mockOtherUserDetails(secondUserName, senderId)))
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
        val (_, viewModel) = Arrangement().withChannelUpdates(conversationDetails = conversationDetails).arrange()
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

    private class Arrangement {
        init {
            // Tests setup
            val dummyConversationId = "some-dummy-value@some.dummy.domain"
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.get<String>(any()) } returns dummyConversationId
            every { savedStateHandle.set(any(), any<String>()) } returns Unit

            // Default empty values
            coEvery { getMessages(any()) } returns flowOf(listOf())
            coEvery { observeMemberDetails(any()) } returns flowOf(listOf())
            coEvery { observeConversationDetails(any()) } returns flowOf()
            coEvery { markMessagesAsNotified(any(), any()) } returns Success
            coEvery { getSelfUserTeam() } returns flowOf()
            coEvery { messageMapper.toUIMessages(any(),any()) } returns listOf()
        }

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var navigationManager: NavigationManager

        @MockK
        lateinit var getMessages: GetRecentMessagesUseCase

        @MockK
        lateinit var sendTextMessage: SendTextMessageUseCase

        @MockK
        lateinit var sendAssetMessage: SendAssetMessageUseCase

        @MockK
        lateinit var sendImageMessage: SendImageMessageUseCase

        @MockK
        lateinit var getMessageAsset: GetMessageAssetUseCase

        @MockK
        lateinit var deleteMessage: DeleteMessageUseCase

        @MockK
        lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

        @MockK
        lateinit var observeMemberDetails: ObserveConversationMembersUseCase

        @MockK
        lateinit var markMessagesAsNotified: MarkMessagesAsNotifiedUseCase

        @MockK
        lateinit var updateAssetMessageDownloadStatus: UpdateAssetMessageDownloadStatusUseCase

        @MockK
        lateinit var getSelfUserTeam: GetSelfTeamUseCase

        @MockK
        lateinit var fileManager: FileManager

        @MockK
        lateinit var messageMapper: MessageMapper

        @MockK
        lateinit var context: Context

        @MockK
        lateinit var uiText: UIText

        val otherMemberUpdatesChannel = Channel<List<MemberDetails>>(capacity = Channel.UNLIMITED)
        val conversationDetailsChannel = Channel<ConversationDetails>(capacity = Channel.UNLIMITED)


        private val viewModel by lazy {
            ConversationViewModel(
                savedStateHandle = savedStateHandle,
                navigationManager = navigationManager,
                getMessages = getMessages,
                observeConversationDetails = observeConversationDetails,
                observeMemberDetails = observeMemberDetails,
                sendTextMessage = sendTextMessage,
                sendAssetMessage = sendAssetMessage,
                sendImageMessage = sendImageMessage,
                getMessageAsset = getMessageAsset,
                deleteMessage = deleteMessage,
                dispatchers = TestDispatcherProvider(),
                markMessagesAsNotified = markMessagesAsNotified,
                updateAssetMessageDownloadStatus = updateAssetMessageDownloadStatus,
                getSelfUserTeam = getSelfUserTeam,
                fileManager = fileManager,
                messageMapper = messageMapper
            )
        }

        suspend fun withChannelUpdates(
            messages: List<Message> = emptyList(),
            members: List<MemberDetails> = emptyList(),
            conversationDetails: ConversationDetails? = null
        ): Arrangement {
            coEvery { getMessages(any()) } returns flowOf(messages)
            coEvery { observeMemberDetails(any()) } returns otherMemberUpdatesChannel.consumeAsFlow()
            coEvery { observeConversationDetails(any()) } returns conversationDetailsChannel.consumeAsFlow()
            otherMemberUpdatesChannel.send(members)
            conversationDetails?.run { conversationDetailsChannel.send(this) }
            return this
        }

        fun withSuccessfulSendAttachmentMessage(): Arrangement {
            coEvery { sendAssetMessage(any(), any(), any(), any()) } returns SendAssetMessageResult.Success
            coEvery { sendImageMessage(any(), any(), any(), any(), any()) } returns SendImageMessageResult.Success
            return this
        }

        fun withSuccessfulSaveAssetMessage(assetName: String, assetData: ByteArray, messageId: String): Arrangement {
            viewModel.showOnAssetDownloadedDialog(assetName, assetData, messageId)
            coEvery { fileManager.saveToExternalStorage(any(), any(), any()) }.answers {
                viewModel.hideOnAssetDownloadedDialog()
            }
            return this
        }

        fun withSuccessfulOpenAssetMessage(assetName: String, assetData: ByteArray, messageId: String): Arrangement {
            viewModel.showOnAssetDownloadedDialog(assetName, assetData, messageId)
            every { fileManager.openWithExternalApp(any(), any(), any()) }.answers {
                viewModel.hideOnAssetDownloadedDialog()
            }
            return this
        }

        fun withTeamUser(userTeam: Team): Arrangement {
            coEvery { getSelfUserTeam() } returns flowOf(userTeam)
            return this
        }

        fun arrange() = this to viewModel

    }

    private fun withMockConversationDetailsOneOnOne(senderName: String, senderAvatar: UserAssetId? = null) = ConversationDetails.OneOne(
        mockk(),
        mockk<OtherUser>().apply {
            every { name } returns senderName
            every { previewPicture } returns senderAvatar
        },
        ConnectionState.PENDING,
        LegalHoldStatus.DISABLED,
        UserType.INTERNAL
    )

    private fun mockConversationDetailsGroup(conversationName: String) = ConversationDetails.Group(mockk<Conversation>().apply {
        every { name } returns conversationName
        every { id } returns ConversationId("someId", "someDomain")
    }, mockk())

    private fun mockSelfUserDetails(
        name: String,
        id: UserId = UserId("self", "user")
    ): MemberDetails.Self = mockk<MemberDetails.Self>().also {
        every { it.selfUser } returns mockk<SelfUser>().also { user ->
            every { user.id } returns id
            every { user.name } returns name
            every { user.previewPicture } returns null
        }
    }

    private fun mockOtherUserDetails(
        name: String,
        id: UserId = UserId("other", "user"),
        userType: UserType = UserType.INTERNAL
    ): MemberDetails.Other = mockk<MemberDetails.Other>().also {
        every { it.otherUser } returns mockk<OtherUser>().also { user ->
            every { user.id } returns id
            every { user.name } returns name
            every { user.previewPicture } returns null
        }
        every { it.userType } returns userType
    }

    private fun mockedMessage(senderId: UserId) = Message(
        id = "messageID",
        content = Text("Some Text Message"),
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = senderId,
        senderClientId = ClientId("client-id"),
        status = Message.Status.SENT,
        editStatus = Message.EditStatus.NotEdited
    )
}
