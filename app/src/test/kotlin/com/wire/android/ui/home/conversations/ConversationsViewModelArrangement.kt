package com.wire.android.ui.home.conversations

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.model.UserAvatarData
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.configuration.FileSharingStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.asset.SendAssetMessageResult
import com.wire.kalium.logic.feature.asset.SendAssetMessageUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.feature.conversation.GetSecurityClassificationTypeUseCase
import com.wire.kalium.logic.feature.conversation.IsSelfUserMemberResult
import com.wire.kalium.logic.feature.conversation.ObserveIsSelfUserMemberUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReadDateUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import okio.Path
import okio.buffer

internal class ConversationsViewModelArrangement {

    val conversationId: ConversationId = ConversationId("some-dummy-value", "some.dummy.domain")

    init {
        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        every { savedStateHandle.get<String>(any()) } returns conversationId.toString()

        // Default empty values
        coEvery { getSelfUserTeam() } returns flowOf()
        every { isFileSharingEnabledUseCase() } returns FileSharingStatus(null, null)
        coEvery { observeOngoingCallsUseCase() } returns flowOf(listOf())
        coEvery { observeEstablishedCallsUseCase() } returns flowOf(listOf())
        coEvery { observeSyncState() } returns flowOf(SyncState.Live)
        every {
            qualifiedIdMapper.fromStringToQualifiedID("some-dummy-value@some.dummy.domain")
        } returns QualifiedID("some-dummy-value", "some.dummy.domain")
    }

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    @MockK
    lateinit var sendTextMessage: SendTextMessageUseCase

    @MockK
    lateinit var sendAssetMessage: SendAssetMessageUseCase

    @MockK
    lateinit var deleteMessage: DeleteMessageUseCase

    @MockK
    lateinit var getSelfUserTeam: GetSelfTeamUseCase

    @MockK
    lateinit var isFileSharingEnabledUseCase: IsFileSharingEnabledUseCase

    @MockK
    lateinit var observeOngoingCallsUseCase: ObserveOngoingCallsUseCase

    @MockK
    private lateinit var wireSessionImageLoader: WireSessionImageLoader

    @MockK
    private lateinit var observeEstablishedCallsUseCase: ObserveEstablishedCallsUseCase

    @MockK
    private lateinit var observeIsSelfUserMemberUseCase: ObserveIsSelfUserMemberUseCase

    @MockK
    private lateinit var endCall: EndCallUseCase

    @MockK
    private lateinit var updateConversationReadDateUseCase: UpdateConversationReadDateUseCase

    @MockK
    private lateinit var getSecurityClassificationType: GetSecurityClassificationTypeUseCase

    @MockK
    private lateinit var observeSyncState: ObserveSyncStateUseCase

    private val fakeKaliumFileSystem = FakeKaliumFileSystem()

    private val viewModel by lazy {
        MessageComposerViewModel(
            savedStateHandle = savedStateHandle,
            navigationManager = navigationManager,
            qualifiedIdMapper = qualifiedIdMapper,
            sendTextMessage = sendTextMessage,
            sendAssetMessage = sendAssetMessage,
            deleteMessage = deleteMessage,
            dispatchers = TestDispatcherProvider(),
            getSelfUserTeam = getSelfUserTeam,
            isFileSharingEnabled = isFileSharingEnabledUseCase,
            wireSessionImageLoader = wireSessionImageLoader,
            kaliumFileSystem = fakeKaliumFileSystem,
            updateConversationReadDateUseCase = updateConversationReadDateUseCase,
            observeIsSelfConversationMember = observeIsSelfUserMemberUseCase,
            getConversationClassifiedType = getSecurityClassificationType
        )
    }

    suspend fun withSuccessfulViewModelInit() = apply {
        coEvery { isFileSharingEnabledUseCase() } returns FileSharingStatus(null, null)
        coEvery { observeOngoingCallsUseCase() } returns emptyFlow()
        coEvery { observeEstablishedCallsUseCase() } returns emptyFlow()
        coEvery { observeIsSelfUserMemberUseCase(any()) } returns flowOf(IsSelfUserMemberResult.Success(true))
    }

    fun withStoredAsset(dataPath: Path, dataContent: ByteArray) = apply {
        fakeKaliumFileSystem.sink(dataPath).buffer().use {
            it.write(dataContent)
        }
    }

    fun withSuccessfulSendAttachmentMessage() = apply {
        coEvery { sendAssetMessage(any(), any(), any(), any(), any(), any(), any()) } returns SendAssetMessageResult.Success
    }

    fun withFailureOnDeletingMessages() = apply {
        coEvery { deleteMessage(any(), any(), any()) } returns Either.Left(CoreFailure.Unknown(null))
        return this
    }

    fun withTeamUser(userTeam: Team) = apply {
        coEvery { getSelfUserTeam() } returns flowOf(userTeam)
        return this
    }

    fun arrange() = this to viewModel

}

internal fun withMockConversationDetailsOneOnOne(
    senderName: String,
    senderAvatar: UserAssetId? = null,
    senderId: UserId = UserId("user-id", "user-domain"),
    connectionState: ConnectionState = ConnectionState.ACCEPTED,
    unavailable: Boolean = false
) = ConversationDetails.OneOne(
    conversation = mockk(),
    otherUser = mockk<OtherUser>().apply {
        every { id } returns senderId
        every { name } returns senderName
        every { previewPicture } returns senderAvatar
        every { availabilityStatus } returns UserAvailabilityStatus.NONE
        every { connectionStatus } returns connectionState
        every { isUnavailableUser } returns unavailable
    },
    legalHoldStatus = LegalHoldStatus.DISABLED,
    userType = UserType.INTERNAL,
    unreadMessagesCount = 0L,
    lastUnreadMessage = null
)

internal fun mockConversationDetailsGroup(
    conversationName: String,
    mockedConversationId: ConversationId = ConversationId("someId", "someDomain")
) = ConversationDetails.Group(
    conversation = mockk<Conversation>().apply {
        every { name } returns conversationName
        every { id } returns mockedConversationId
    },
    legalHoldStatus = mockk(),
    hasOngoingCall = false,
    unreadMessagesCount = 0,
    lastUnreadMessage = null
)

internal fun mockUITextMessage(id:String = "someId", userName: String = "mockUserName"): UIMessage {
    return mockk<UIMessage>().also {
        every { it.userAvatarData } returns UserAvatarData()
        every { it.messageSource } returns MessageSource.OtherUser
        every { it.messageHeader } returns mockk<MessageHeader>().also {
            every { it.messageId } returns id
            every { it.username } returns UIText.DynamicString(userName)
            every { it.isLegalHold } returns false
            every { it.messageTime } returns MessageTime("")
            every { it.messageStatus } returns MessageStatus.Untouched
        }
        every { it.messageContent } returns null
    }
}
