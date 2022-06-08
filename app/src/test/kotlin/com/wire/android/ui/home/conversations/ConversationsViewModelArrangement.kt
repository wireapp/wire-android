package com.wire.android.ui.home.conversations

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.model.UserStatus
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.User
import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
import com.wire.android.util.FileManager
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.conversation.UserType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.SendAssetMessageResult
import com.wire.kalium.logic.feature.asset.SendAssetMessageUseCase
import com.wire.kalium.logic.feature.asset.SendImageMessageResult
import com.wire.kalium.logic.feature.asset.SendImageMessageUseCase
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageDownloadStatusUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.MarkMessagesAsNotifiedUseCase
import com.wire.kalium.logic.feature.message.Result
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf

internal class ConversationsViewModelArrangement {
    init {
        // Tests setup
        val dummyConversationId = "some-dummy-value@some.dummy.domain"
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { savedStateHandle.get<String>(any()) } returns dummyConversationId
        every { savedStateHandle.set(any(), any<String>()) } returns Unit

        // Default empty values
        coEvery { observeConversationDetails(any()) } returns flowOf()
        coEvery { markMessagesAsNotified(any(), any()) } returns Result.Success
        coEvery { getSelfUserTeam() } returns flowOf()
    }

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var navigationManager: NavigationManager

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
    lateinit var markMessagesAsNotified: MarkMessagesAsNotifiedUseCase

    @MockK
    lateinit var updateAssetMessageDownloadStatus: UpdateAssetMessageDownloadStatusUseCase

    @MockK
    lateinit var getSelfUserTeam: GetSelfTeamUseCase

    @MockK
    lateinit var fileManager: FileManager

    @MockK
    lateinit var getMessagesForConversationUseCase: GetMessagesForConversationUseCase

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var uiText: UIText

    private val conversationDetailsChannel = Channel<ConversationDetails>(capacity = Channel.UNLIMITED)

    private val messagesChannel = Channel<List<UIMessage>>(capacity = Channel.UNLIMITED)

    private val viewModel by lazy {
        ConversationViewModel(
            savedStateHandle = savedStateHandle,
            navigationManager = navigationManager,
            observeConversationDetails = observeConversationDetails,
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
            getMessagesForConversation = getMessagesForConversationUseCase
        )
    }

    suspend fun withMessagesUpdate(messages: List<UIMessage>): ConversationsViewModelArrangement {
        coEvery { getMessagesForConversationUseCase(any()) } returns messagesChannel.consumeAsFlow()
        messagesChannel.send(messages)

        return this
    }

    suspend fun withConversationDetailUpdate(conversationDetails: ConversationDetails): ConversationsViewModelArrangement {
        coEvery { observeConversationDetails(any()) } returns conversationDetailsChannel.consumeAsFlow()
        conversationDetailsChannel.send(conversationDetails)

        return this
    }


    fun withSuccessfulSendAttachmentMessage(): ConversationsViewModelArrangement {
        coEvery { sendAssetMessage(any(), any(), any(), any()) } returns SendAssetMessageResult.Success
        coEvery { sendImageMessage(any(), any(), any(), any(), any()) } returns SendImageMessageResult.Success
        return this
    }

    fun withSuccessfulSaveAssetMessage(assetName: String, assetData: ByteArray, messageId: String): ConversationsViewModelArrangement {
        viewModel.showOnAssetDownloadedDialog(assetName, assetData, messageId)
        coEvery { fileManager.saveToExternalStorage(any(), any(), any()) }.answers {
            viewModel.hideOnAssetDownloadedDialog()
        }
        return this
    }

    fun withSuccessfulOpenAssetMessage(assetName: String, assetData: ByteArray, messageId: String): ConversationsViewModelArrangement {
        viewModel.showOnAssetDownloadedDialog(assetName, assetData, messageId)
        every { fileManager.openWithExternalApp(any(), any(), any()) }.answers {
            viewModel.hideOnAssetDownloadedDialog()
        }
        return this
    }

    fun withTeamUser(userTeam: Team): ConversationsViewModelArrangement {
        coEvery { getSelfUserTeam() } returns flowOf(userTeam)
        return this
    }

    fun arrange() = this to viewModel

}

internal fun withMockConversationDetailsOneOnOne(senderName: String, senderAvatar: UserAssetId? = null) = ConversationDetails.OneOne(
    mockk(),
    mockk<OtherUser>().apply {
        every { name } returns senderName
        every { previewPicture } returns senderAvatar
    },
    ConnectionState.PENDING,
    LegalHoldStatus.DISABLED,
    UserType.INTERNAL
)

internal fun mockConversationDetailsGroup(conversationName: String) = ConversationDetails.Group(mockk<Conversation>().apply {
    every { name } returns conversationName
    every { id } returns ConversationId("someId", "someDomain")
}, mockk())

internal fun mockUITextMessage(userName: String = "mockUserName"): UIMessage {
    return mockk<UIMessage>().also {
        every { it.user } returns mockk<User>().also {
            every { it.avatarAsset } returns null
            every { it.availabilityStatus } returns UserStatus.AVAILABLE
        }
        every { it.messageSource } returns MessageSource.OtherUser
        every { it.messageHeader } returns mockk<MessageHeader>().also {
            every { it.messageId } returns "someId"
            every { it.username } returns UIText.DynamicString(userName)
            every { it.isLegalHold } returns false
            every { it.time } returns ""
            every { it.messageStatus } returns MessageStatus.Untouched
        }
        every { it.messageContent } returns null
    }
}
