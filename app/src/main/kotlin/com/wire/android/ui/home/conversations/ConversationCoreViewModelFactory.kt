/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
 */
package com.wire.android.ui.home.conversations

import androidx.lifecycle.SavedStateHandle
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.mapper.ContactMapper
import com.wire.android.media.PingRinger
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentsViewModel
import com.wire.android.ui.home.conversations.composer.MessageComposerViewModel
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.draft.MessageDraftViewModel
import com.wire.android.ui.home.conversations.messages.item.ConversationAssetPathsViewModelImpl
import com.wire.android.ui.home.conversations.migration.ConversationMigrationViewModel
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModel
import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
import com.wire.android.ui.home.conversations.usecase.GetQuoteMessageForConversationUseCase
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.util.FileManager
import com.wire.android.util.GetMediaMetadataUseCase
import com.wire.android.util.ImageUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.kalium.cells.domain.CellUploadManager
import com.wire.kalium.cells.domain.usecase.AddAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.ObserveAttachmentDraftsUseCase
import com.wire.kalium.cells.domain.usecase.RemoveAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.RetryAttachmentUploadUseCase
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.ObserveAssetStatusesUseCase
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageTransferStatusUseCase
import com.wire.kalium.logic.feature.asset.upload.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledForConversationUseCase
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.conversation.ClearUsersTypingEventsUseCase
import com.wire.kalium.logic.feature.conversation.GetConversationUnreadEventsCountUseCase
import com.wire.kalium.logic.feature.conversation.MembersToMentionUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationInteractionAvailabilityUseCase
import com.wire.kalium.logic.feature.conversation.MarkConversationAsReadLocallyUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationUnderLegalHoldNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.SendTypingEventUseCase
import com.wire.kalium.logic.feature.conversation.SetNotifiedAboutConversationUnderLegalHoldUseCase
import com.wire.kalium.logic.feature.conversation.SetUserInformedAboutVerificationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReadDateUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.FetchOlderNomadMessagesByConversationUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import com.wire.kalium.logic.feature.message.GetSearchedConversationMessagePositionUseCase
import com.wire.kalium.logic.feature.message.RetryFailedMessageUseCase
import com.wire.kalium.logic.feature.message.SendEditMultipartMessageUseCase
import com.wire.kalium.logic.feature.message.SendEditTextMessageUseCase
import com.wire.kalium.logic.feature.message.SendKnockUseCase
import com.wire.kalium.logic.feature.message.SendLocationUseCase
import com.wire.kalium.logic.feature.message.SendMultipartMessageUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.message.ToggleReactionUseCase
import com.wire.kalium.logic.feature.message.draft.GetMessageDraftUseCase
import com.wire.kalium.logic.feature.message.draft.RemoveMessageDraftUseCase
import com.wire.kalium.logic.feature.message.draft.SaveMessageDraftUseCase
import com.wire.kalium.logic.feature.message.ephemeral.EnqueueMessageSelfDeletionUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.sessionreset.ResetSessionUseCase
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import javax.inject.Inject

@Suppress("LongParameterList")
class ConversationCoreViewModelFactory @Inject constructor(
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val getMessageAsset: GetMessageAssetUseCase,
    private val getMessageByIdUseCase: GetMessageByIdUseCase,
    private val updateAssetMessageDownloadStatus: UpdateAssetMessageTransferStatusUseCase,
    private val observeAssetStatusesUseCase: ObserveAssetStatusesUseCase,
    private val fileManager: FileManager,
    private val dispatchers: DispatcherProvider,
    private val getMessageForConversation: GetMessagesForConversationUseCase,
    private val fetchOlderNomadMessages: FetchOlderNomadMessagesByConversationUseCase,
    private val toggleReaction: ToggleReactionUseCase,
    private val resetSession: ResetSessionUseCase,
    private val audioMessagePlayer: ConversationAudioMessagePlayer,
    private val getConversationUnreadEventsCount: GetConversationUnreadEventsCountUseCase,
    private val clearUsersTypingEvents: ClearUsersTypingEventsUseCase,
    private val getSearchedConversationMessagePosition: GetSearchedConversationMessagePositionUseCase,
    private val deleteMessage: DeleteMessageUseCase,
    private val isWireCellFeatureEnabled: IsWireCellsEnabledUseCase,
    private val isFileSharingEnabled: IsFileSharingEnabledUseCase,
    private val observeConversationInteractionAvailability: ObserveConversationInteractionAvailabilityUseCase,
    private val updateConversationReadDate: UpdateConversationReadDateUseCase,
    private val markConversationAsReadLocally: MarkConversationAsReadLocallyUseCase,
    private val contactMapper: ContactMapper,
    private val membersToMention: MembersToMentionUseCase,
    private val enqueueMessageSelfDeletion: EnqueueMessageSelfDeletionUseCase,
    private val persistNewSelfDeletingStatus: PersistNewSelfDeletionTimerUseCase,
    private val sendTypingEvent: SendTypingEventUseCase,
    private val kaliumFileSystem: KaliumFileSystem,
    private val currentSessionFlowUseCase: CurrentSessionFlowUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val globalDataStore: GlobalDataStore,
    private val sendAssetMessage: ScheduleNewAssetMessageUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val sendMultipartMessage: SendMultipartMessageUseCase,
    private val sendEditTextMessage: SendEditTextMessageUseCase,
    private val sendEditMultipartMessage: SendEditMultipartMessageUseCase,
    private val retryFailedMessage: RetryFailedMessageUseCase,
    private val handleUriAsset: HandleUriAssetUseCase,
    private val sendKnock: SendKnockUseCase,
    private val pingRinger: PingRinger,
    private val imageUtil: ImageUtil,
    private val setUserInformedAboutVerification: SetUserInformedAboutVerificationUseCase,
    private val observeDegradedConversationNotified: ObserveDegradedConversationNotifiedUseCase,
    private val setNotifiedAboutConversationUnderLegalHold: SetNotifiedAboutConversationUnderLegalHoldUseCase,
    private val observeConversationUnderLegalHoldNotified: ObserveConversationUnderLegalHoldNotifiedUseCase,
    private val sendLocation: SendLocationUseCase,
    private val removeMessageDraft: RemoveMessageDraftUseCase,
    private val analyticsManager: AnonymousAnalyticsManager,
    private val isWireCellsEnabledForConversation: IsWireCellsEnabledForConversationUseCase,
    private val sharedState: MessageSharedState,
    private val getMessageDraft: GetMessageDraftUseCase,
    private val getQuotedMessage: GetQuoteMessageForConversationUseCase,
    private val saveMessageDraft: SaveMessageDraftUseCase,
    private val observeAttachments: ObserveAttachmentDraftsUseCase,
    private val addAttachment: AddAttachmentDraftUseCase,
    private val removeAttachment: RemoveAttachmentDraftUseCase,
    private val retryUpload: RetryAttachmentUploadUseCase,
    private val uploadManager: CellUploadManager,
    private val getMediaMetadata: GetMediaMetadataUseCase,
) {
    fun conversationMessagesViewModel(savedStateHandle: SavedStateHandle) = ConversationMessagesViewModel(
        savedStateHandle = savedStateHandle,
        observeConversationDetails = observeConversationDetails,
        getMessageAsset = getMessageAsset,
        getMessageByIdUseCase = getMessageByIdUseCase,
        updateAssetMessageDownloadStatus = updateAssetMessageDownloadStatus,
        observeAssetStatusesUseCase = observeAssetStatusesUseCase,
        fileManager = fileManager,
        dispatchers = dispatchers,
        getMessageForConversation = getMessageForConversation,
        fetchOlderNomadMessages = fetchOlderNomadMessages,
        toggleReaction = toggleReaction,
        resetSession = resetSession,
        audioMessagePlayer = audioMessagePlayer,
        getConversationUnreadEventsCount = getConversationUnreadEventsCount,
        clearUsersTypingEvents = clearUsersTypingEvents,
        getSearchedConversationMessagePosition = getSearchedConversationMessagePosition,
        deleteMessage = deleteMessage,
        isWireCellFeatureEnabled = isWireCellFeatureEnabled,
    )

    fun messageComposerViewModel(savedStateHandle: SavedStateHandle) = MessageComposerViewModel(
        savedStateHandle = savedStateHandle,
        dispatchers = dispatchers,
        isFileSharingEnabled = isFileSharingEnabled,
        observeConversationInteractionAvailability = observeConversationInteractionAvailability,
        updateConversationReadDate = updateConversationReadDate,
        markConversationAsReadLocally = markConversationAsReadLocally,
        contactMapper = contactMapper,
        membersToMention = membersToMention,
        enqueueMessageSelfDeletion = enqueueMessageSelfDeletion,
        persistNewSelfDeletingStatus = persistNewSelfDeletingStatus,
        sendTypingEvent = sendTypingEvent,
        fileManager = fileManager,
        kaliumFileSystem = kaliumFileSystem,
        currentSessionFlowUseCase = currentSessionFlowUseCase,
        observeEstablishedCalls = observeEstablishedCalls,
        globalDataStore = globalDataStore,
    )

    fun sendMessageViewModel(savedStateHandle: SavedStateHandle) = SendMessageViewModel(
        savedStateHandle = savedStateHandle,
        sendAssetMessage = sendAssetMessage,
        sendTextMessage = sendTextMessage,
        sendMultipartMessage = sendMultipartMessage,
        sendEditTextMessage = sendEditTextMessage,
        sendEditMultipartMessage = sendEditMultipartMessage,
        retryFailedMessage = retryFailedMessage,
        dispatchers = dispatchers,
        kaliumFileSystem = kaliumFileSystem,
        handleUriAsset = handleUriAsset,
        sendKnock = sendKnock,
        sendTypingEvent = sendTypingEvent,
        pingRinger = pingRinger,
        imageUtil = imageUtil,
        setUserInformedAboutVerification = setUserInformedAboutVerification,
        observeDegradedConversationNotified = observeDegradedConversationNotified,
        setNotifiedAboutConversationUnderLegalHold = setNotifiedAboutConversationUnderLegalHold,
        observeConversationUnderLegalHoldNotified = observeConversationUnderLegalHoldNotified,
        sendLocation = sendLocation,
        removeMessageDraft = removeMessageDraft,
        analyticsManager = analyticsManager,
        isWireCellsEnabledForConversation = isWireCellsEnabledForConversation,
        sharedState = sharedState,
    )

    fun messageDraftViewModel(savedStateHandle: SavedStateHandle) = MessageDraftViewModel(
        savedStateHandle = savedStateHandle,
        getMessageDraft = getMessageDraft,
        getQuotedMessage = getQuotedMessage,
        saveMessageDraft = saveMessageDraft,
    )

    fun messageAttachmentsViewModel(savedStateHandle: SavedStateHandle) = MessageAttachmentsViewModel(
        savedStateHandle = savedStateHandle,
        handleUriAsset = handleUriAsset,
        observeAttachments = observeAttachments,
        addAttachment = addAttachment,
        removeAttachment = removeAttachment,
        retryUpload = retryUpload,
        uploadManager = uploadManager,
        fileManager = fileManager,
        sharedState = sharedState,
        getMediaMetadata = getMediaMetadata,
    )

    fun conversationMigrationViewModel(savedStateHandle: SavedStateHandle) = ConversationMigrationViewModel(
        savedStateHandle = savedStateHandle,
        observeConversationDetails = observeConversationDetails,
    )

    fun conversationAssetPathsViewModel() = ConversationAssetPathsViewModelImpl(
        getMessageAsset = getMessageAsset,
        dispatchers = dispatchers,
    )
}
