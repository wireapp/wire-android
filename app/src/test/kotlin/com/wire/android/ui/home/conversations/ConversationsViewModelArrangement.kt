package com.wire.android.ui.home.conversations

import android.content.res.Resources
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.model.UserAvatarData
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
import com.wire.android.util.FileManager
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.SendAssetMessageResult
import com.wire.kalium.logic.feature.asset.SendAssetMessageUseCase
import com.wire.kalium.logic.feature.asset.SendImageMessageResult
import com.wire.kalium.logic.feature.asset.SendImageMessageUseCase
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageDownloadStatusUseCase
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import com.wire.kalium.logic.functional.Either
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
    lateinit var updateAssetMessageDownloadStatus: UpdateAssetMessageDownloadStatusUseCase

    @MockK
    lateinit var getSelfUserTeam: GetSelfTeamUseCase

    @MockK
    lateinit var fileManager: FileManager

    @MockK
    lateinit var getMessagesForConversationUseCase: GetMessagesForConversationUseCase

    @MockK
    lateinit var isFileSharingEnabledUseCase: IsFileSharingEnabledUseCase

    @MockK
    lateinit var resources: Resources

    @MockK
    lateinit var uiText: UIText

    @MockK
    lateinit var observeOngoingCallsUseCase: ObserveOngoingCallsUseCase

    @MockK
    lateinit var answerCallUseCase: AnswerCallUseCase

    @MockK
    private lateinit var wireSessionImageLoader: WireSessionImageLoader

   @MockK
    private lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

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
            updateAssetMessageDownloadStatus = updateAssetMessageDownloadStatus,
            getSelfUserTeam = getSelfUserTeam,
            fileManager = fileManager,
            observeEstablishedCalls = observeEstablishedCalls,
            getMessageForConversation = getMessagesForConversationUseCase,
            isFileSharingEnabled = isFileSharingEnabledUseCase,
            observeOngoingCalls = observeOngoingCallsUseCase,
            answerCall = answerCallUseCase,
            wireSessionImageLoader = wireSessionImageLoader
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

    fun withFailureOnDeletingMessages(): ConversationsViewModelArrangement {
        coEvery { deleteMessage(any(), any(), any()) } returns Either.Left(CoreFailure.Unknown(null))
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

internal fun withMockConversationDetailsOneOnOne(
    senderName: String,
    senderAvatar: UserAssetId? = null,
    senderId: UserId = UserId("user-id", "user-domain")
) = ConversationDetails.OneOne(
    mockk(),
    mockk<OtherUser>().apply {
        every { id } returns senderId
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
        every { it.userAvatarData } returns UserAvatarData()
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
