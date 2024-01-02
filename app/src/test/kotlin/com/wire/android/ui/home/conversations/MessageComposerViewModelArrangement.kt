/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.conversations

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.framework.TestConversation
import com.wire.android.mapper.ContactMapper
import com.wire.android.media.PingRinger
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.navArgs
import com.wire.android.util.FileManager
import com.wire.android.util.ImageUtil
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.configuration.FileSharingStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.LegalHoldStatus
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageResult
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.IsInteractionAvailableResult
import com.wire.kalium.logic.feature.conversation.MembersToMentionUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationInteractionAvailabilityUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationUnderLegalHoldNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.SendTypingEventUseCase
import com.wire.kalium.logic.feature.conversation.SetNotifiedAboutConversationUnderLegalHoldUseCase
import com.wire.kalium.logic.feature.conversation.SetUserInformedAboutVerificationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReadDateUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.RetryFailedMessageUseCase
import com.wire.kalium.logic.feature.message.SendEditTextMessageUseCase
import com.wire.kalium.logic.feature.message.SendKnockUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.message.ephemeral.EnqueueMessageSelfDeletionUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
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

internal class MessageComposerViewModelArrangement {

    val conversationId: ConversationId = ConversationId("some-dummy-value", "some.dummy.domain")

    init {
        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        every { savedStateHandle.navArgs<ConversationNavArgs>() } returns ConversationNavArgs(conversationId = conversationId)

        // Default empty values
        every { isFileSharingEnabledUseCase() } returns FileSharingStatus(FileSharingStatus.Value.EnabledAll, null)
        coEvery { observeOngoingCallsUseCase() } returns flowOf(listOf())
        coEvery { observeEstablishedCallsUseCase() } returns flowOf(listOf())
        coEvery { observeSyncState() } returns flowOf(SyncState.Live)
        every { pingRinger.ping(any(), any()) } returns Unit
        coEvery { sendKnockUseCase(any(), any()) } returns Either.Right(Unit)
        coEvery { fileManager.getTempWritableVideoUri(any(), any()) } returns Uri.parse("video.mp4")
        coEvery { fileManager.getTempWritableImageUri(any(), any()) } returns Uri.parse("image.jpg")
        coEvery { setUserInformedAboutVerificationUseCase(any()) } returns Unit
        coEvery { observeDegradedConversationNotifiedUseCase(any()) } returns flowOf(true)
        coEvery { setNotifiedAboutConversationUnderLegalHold(any()) } returns Unit
        coEvery { observeConversationUnderLegalHoldNotified(any()) } returns flowOf(true)
    }

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var sendTextMessage: SendTextMessageUseCase

    @MockK
    lateinit var sendEditTextMessage: SendEditTextMessageUseCase

    @MockK
    lateinit var sendAssetMessage: ScheduleNewAssetMessageUseCase

    @MockK
    lateinit var deleteMessage: DeleteMessageUseCase

    @MockK
    lateinit var isFileSharingEnabledUseCase: IsFileSharingEnabledUseCase

    @MockK
    lateinit var observeOngoingCallsUseCase: ObserveOngoingCallsUseCase

    @MockK
    private lateinit var observeEstablishedCallsUseCase: ObserveEstablishedCallsUseCase

    @MockK
    private lateinit var observeConversationInteractionAvailabilityUseCase: ObserveConversationInteractionAvailabilityUseCase

    @MockK
    private lateinit var endCall: EndCallUseCase

    @MockK
    private lateinit var updateConversationReadDateUseCase: UpdateConversationReadDateUseCase

    @MockK
    lateinit var sendKnockUseCase: SendKnockUseCase

    @MockK
    lateinit var fileManager: FileManager

    @MockK
    private lateinit var observeSyncState: ObserveSyncStateUseCase

    @MockK
    private lateinit var contactMapper: ContactMapper

    @MockK
    lateinit var pingRinger: PingRinger

    @MockK
    private lateinit var imageUtil: ImageUtil

    @MockK
    private lateinit var membersToMention: MembersToMentionUseCase

    @MockK
    private lateinit var getAssetSizeLimitUseCase: GetAssetSizeLimitUseCase

    @MockK
    private lateinit var enqueueMessageSelfDeletionUseCase: EnqueueMessageSelfDeletionUseCase

    @MockK
    lateinit var observeConversationSelfDeletionStatus: ObserveSelfDeletionTimerSettingsForConversationUseCase

    @MockK
    lateinit var persistSelfDeletionStatus: PersistNewSelfDeletionTimerUseCase

    @MockK
    lateinit var retryFailedMessageUseCase: RetryFailedMessageUseCase

    @MockK
    lateinit var sendTypingEvent: SendTypingEventUseCase

    @MockK
    lateinit var setUserInformedAboutVerificationUseCase: SetUserInformedAboutVerificationUseCase

    @MockK
    lateinit var observeDegradedConversationNotifiedUseCase: ObserveDegradedConversationNotifiedUseCase

    @MockK
    lateinit var setNotifiedAboutConversationUnderLegalHold: SetNotifiedAboutConversationUnderLegalHoldUseCase

    @MockK
    lateinit var observeConversationUnderLegalHoldNotified: ObserveConversationUnderLegalHoldNotifiedUseCase

    private val fakeKaliumFileSystem = FakeKaliumFileSystem()

    private val viewModel by lazy {
        MessageComposerViewModel(
            savedStateHandle = savedStateHandle,
            sendTextMessage = sendTextMessage,
            sendEditTextMessage = sendEditTextMessage,
            sendAssetMessage = sendAssetMessage,
            deleteMessage = deleteMessage,
            dispatchers = TestDispatcherProvider(),
            isFileSharingEnabled = isFileSharingEnabledUseCase,
            kaliumFileSystem = fakeKaliumFileSystem,
            updateConversationReadDate = updateConversationReadDateUseCase,
            observeConversationInteractionAvailability = observeConversationInteractionAvailabilityUseCase,
            contactMapper = contactMapper,
            membersToMention = membersToMention,
            getAssetSizeLimit = getAssetSizeLimitUseCase,
            imageUtil = imageUtil,
            pingRinger = pingRinger,
            sendKnockUseCase = sendKnockUseCase,
            fileManager = fileManager,
            enqueueMessageSelfDeletion = enqueueMessageSelfDeletionUseCase,
            observeSelfDeletingMessages = observeConversationSelfDeletionStatus,
            persistNewSelfDeletingStatus = persistSelfDeletionStatus,
            retryFailedMessage = retryFailedMessageUseCase,
            sendTypingEvent = sendTypingEvent,
            setUserInformedAboutVerification = setUserInformedAboutVerificationUseCase,
            observeDegradedConversationNotified = observeDegradedConversationNotifiedUseCase,
            setNotifiedAboutConversationUnderLegalHold = setNotifiedAboutConversationUnderLegalHold,
            observeConversationUnderLegalHoldNotified = observeConversationUnderLegalHoldNotified,
        )
    }

    fun withSuccessfulViewModelInit() = apply {
        coEvery { isFileSharingEnabledUseCase() } returns FileSharingStatus(FileSharingStatus.Value.EnabledAll, null)
        coEvery { observeOngoingCallsUseCase() } returns emptyFlow()
        coEvery { observeEstablishedCallsUseCase() } returns emptyFlow()
        coEvery { imageUtil.extractImageWidthAndHeight(any(), any()) } returns (1 to 1)
        coEvery { observeConversationSelfDeletionStatus(any(), any()) } returns emptyFlow()
        coEvery { observeConversationInteractionAvailabilityUseCase(any()) } returns flowOf(
            IsInteractionAvailableResult.Success(
                InteractionAvailability.ENABLED
            )
        )
    }

    fun withStoredAsset(dataPath: Path, dataContent: ByteArray) = apply {
        fakeKaliumFileSystem.sink(dataPath).buffer().use {
            it.write(dataContent)
        }
    }

    fun withSuccessfulSendAttachmentMessage() = apply {
        coEvery {
            sendAssetMessage(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns ScheduleNewAssetMessageResult.Success("some-message-id")
    }

    fun withSuccessfulSendTextMessage() = apply {
        coEvery {
            sendTextMessage(
                any(),
                any(),
                any(),
                any()
            )
        } returns Either.Right(Unit)
    }
    fun withFailedSendTextMessage(failure: CoreFailure) = apply {
        coEvery {
            sendTextMessage(
                any(),
                any(),
                any(),
                any()
            )
        } returns Either.Left(failure)
    }

    fun withSuccessfulSendEditTextMessage() = apply {
        coEvery {
            sendEditTextMessage(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Either.Right(Unit)
    }

    fun withSuccessfulSendTypingEvent() = apply {
        coEvery {
            sendTypingEvent(
                any(),
                any()
            )
        } returns Unit
    }

    fun withFailureOnDeletingMessages() = apply {
        coEvery { deleteMessage(any(), any(), any()) } returns Either.Left(CoreFailure.Unknown(null))
        return this
    }

    fun withGetAssetSizeLimitUseCase(isImage: Boolean, assetSizeLimit: Long) = apply {
        coEvery { getAssetSizeLimitUseCase(eq(isImage)) } returns assetSizeLimit
        return this
    }

    fun withGetAssetBundleFromUri(assetBundle: AssetBundle?) = apply {
        coEvery { fileManager.getAssetBundleFromUri(any(), any(), any(), any()) } returns assetBundle
    }

    fun withSaveToExternalMediaStorage(resultFileName: String?) = apply {
        coEvery { fileManager.saveToExternalMediaStorage(any(), any(), any(), any(), any()) } returns resultFileName
    }

    fun withObserveSelfDeletingStatus(expectedSelfDeletionTimer: SelfDeletionTimer) = apply {
        coEvery { observeConversationSelfDeletionStatus(conversationId, true) } returns flowOf(expectedSelfDeletionTimer)
    }

    fun withPersistSelfDeletionStatus() = apply {
        coEvery { persistSelfDeletionStatus(any(), any()) } returns Unit
    }

    fun withInformAboutVerificationBeforeMessagingFlag(flag: Boolean) = apply {
        coEvery { observeDegradedConversationNotifiedUseCase(any()) } returns flowOf(flag)
    }

    fun withObserveConversationUnderLegalHoldNotified(flag: Boolean) = apply {
        coEvery { observeConversationUnderLegalHoldNotified(any()) } returns flowOf(flag)
    }

    fun withSuccessfulRetryFailedMessage() = apply {
        coEvery { retryFailedMessageUseCase(any(), any()) } returns Either.Right(Unit)
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
    conversation = TestConversation.ONE_ON_ONE,
    otherUser = mockk<OtherUser>().apply {
        every { id } returns senderId
        every { name } returns senderName
        every { previewPicture } returns senderAvatar
        every { availabilityStatus } returns UserAvailabilityStatus.NONE
        every { connectionStatus } returns connectionState
        every { isUnavailableUser } returns unavailable
        every { deleted } returns false
    },
    legalHoldStatus = LegalHoldStatus.DISABLED,
    userType = UserType.INTERNAL,
    lastMessage = null,
    unreadEventCount = emptyMap()
)

internal fun mockConversationDetailsGroup(
    conversationName: String,
    mockedConversationId: ConversationId = ConversationId("someId", "someDomain")
) = ConversationDetails.Group(
    conversation = TestConversation.GROUP()
        .copy(name = conversationName, id = mockedConversationId),
    legalHoldStatus = mockk(),
    hasOngoingCall = false,
    lastMessage = null,
    isSelfUserCreator = true,
    isSelfUserMember = true,
    unreadEventCount = emptyMap(),
    selfRole = Conversation.Member.Role.Member
)

internal fun mockUITextMessage(id: String = "someId", userName: String = "mockUserName"): UIMessage {
    return mockk<UIMessage.Regular>().also {
        every { it.userAvatarData } returns UserAvatarData()
        every { it.source } returns MessageSource.OtherUser
        every { it.header } returns mockk<MessageHeader>().also {
            every { it.messageId } returns id
            every { it.username } returns UIText.DynamicString(userName)
            every { it.isLegalHold } returns false
            every { it.messageTime } returns MessageTime("")
            every { it.messageStatus } returns MessageStatus(
                flowStatus = MessageFlowStatus.Sent,
                expirationStatus = ExpirationStatus.NotExpirable
            )
        }
        every { it.messageContent } returns null
    }
}
