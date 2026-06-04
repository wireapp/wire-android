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
import com.wire.android.di.CurrentAccount
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.cells.ui.edit.OnlineEditor
import com.wire.android.mapper.ContactMapper
import com.wire.android.media.PingRinger
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentsViewModel
import com.wire.android.ui.home.conversations.banner.ConversationBannerViewModel
import com.wire.android.ui.home.conversations.banner.usecase.ObserveConversationMembersByTypesUseCase
import com.wire.android.ui.home.conversations.composer.MessageComposerViewModel
import com.wire.android.ui.home.conversations.info.ConversationInfoViewModel
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.draft.MessageDraftViewModel
import com.wire.android.ui.home.conversations.messages.item.ConversationAssetPathsViewModelImpl
import com.wire.android.ui.home.conversations.messages.QuotedMultipartMessageViewModel
import com.wire.android.ui.home.conversations.media.ConversationAssetMessagesViewModel
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewViewModel
import com.wire.android.ui.home.conversations.messagedetails.MessageDetailsViewModel
import com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReactionsForMessageUseCase
import com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReceiptsForMessageUseCase
import com.wire.android.ui.home.conversations.migration.ConversationMigrationViewModel
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.CellAssetRefreshHelper
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.MultipartAttachmentsViewModelImpl
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModel
import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
import com.wire.android.ui.home.conversations.usecase.GetQuoteMessageForConversationUseCase
import com.wire.android.ui.home.conversations.usecase.GetAssetMessagesFromConversationUseCase
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.ui.home.conversations.usecase.ObserveImageAssetMessagesFromConversationUseCase
import com.wire.android.ui.home.conversations.usecase.ObserveQuoteMessageForConversationUseCase
import com.wire.android.ui.home.gallery.MediaGalleryViewModel
import com.wire.android.ui.home.messagecomposer.location.LocationPickerHelperFlavor
import com.wire.android.ui.home.messagecomposer.location.LocationPickerViewModel
import com.wire.android.util.FileManager
import com.wire.android.util.GetMediaMetadataUseCase
import com.wire.android.util.ImageUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.cells.domain.usecase.download.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.CellUploadManager
import com.wire.kalium.cells.domain.usecase.AddAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetMessageAttachmentUseCase
import com.wire.kalium.cells.domain.usecase.GetWireCellConfigurationUseCase
import com.wire.kalium.cells.domain.usecase.ObserveAttachmentDraftsUseCase
import com.wire.kalium.cells.domain.usecase.RemoveAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.RetryAttachmentUploadUseCase
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.ObserveAssetStatusesUseCase
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageTransferStatusUseCase
import com.wire.kalium.logic.feature.asset.upload.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledForConversationUseCase
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.conversation.ClearUsersTypingEventsUseCase
import com.wire.kalium.logic.feature.conversation.GetConversationUnreadEventsCountUseCase
import com.wire.kalium.logic.feature.conversation.MarkConversationAsReadLocallyUseCase
import com.wire.kalium.logic.feature.conversation.MembersToMentionUseCase
import com.wire.kalium.logic.feature.conversation.NotifyConversationIsOpenUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationInteractionAvailabilityUseCase
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
import com.wire.kalium.logic.feature.e2ei.usecase.FetchConversationMLSVerificationStatusUseCase
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import com.wire.kalium.network.NetworkStateObserver
import dev.zacsweers.metro.Inject

@Suppress("LongParameterList", "TooManyFunctions")
class ConversationCoreViewModelFactory @Inject constructor(
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val getMessageAsset: GetMessageAssetUseCase,
    private val getMessageById: GetMessageByIdUseCase,
    private val updateAssetMessageDownloadStatus: UpdateAssetMessageTransferStatusUseCase,
    private val observeAssetStatuses: ObserveAssetStatusesUseCase,
    private val fileManager: FileManager,
    private val dispatchers: DispatcherProvider,
    private val getMessagesForConversation: GetMessagesForConversationUseCase,
    private val fetchOlderNomadMessages: FetchOlderNomadMessagesByConversationUseCase,
    private val toggleReaction: ToggleReactionUseCase,
    private val resetSession: ResetSessionUseCase,
    private val audioMessagePlayer: ConversationAudioMessagePlayer,
    private val getConversationUnreadEventsCount: GetConversationUnreadEventsCountUseCase,
    private val clearUsersTypingEvents: ClearUsersTypingEventsUseCase,
    private val getSearchedConversationMessagePosition: GetSearchedConversationMessagePositionUseCase,
    private val deleteMessage: DeleteMessageUseCase,
    private val isWireCellsEnabled: IsWireCellsEnabledUseCase,
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
    private val messageSharedState: MessageSharedState,
    private val getMessageDraft: GetMessageDraftUseCase,
    private val getQuotedMessage: GetQuoteMessageForConversationUseCase,
    private val observeQuotedMessage: ObserveQuoteMessageForConversationUseCase,
    private val saveMessageDraft: SaveMessageDraftUseCase,
    private val observeAttachments: ObserveAttachmentDraftsUseCase,
    private val addAttachment: AddAttachmentDraftUseCase,
    private val removeAttachment: RemoveAttachmentDraftUseCase,
    private val retryAttachmentUpload: RetryAttachmentUploadUseCase,
    private val cellUploadManager: CellUploadManager,
    private val getMediaMetadata: GetMediaMetadataUseCase,
    private val getAttachment: GetMessageAttachmentUseCase,
    private val getCellNode: GetCellFileUseCase,
    private val locationPickerHelper: LocationPickerHelperFlavor,
    private val getImageMessages: ObserveImageAssetMessagesFromConversationUseCase,
    private val getAssetMessages: GetAssetMessagesFromConversationUseCase,
    private val observeConversationMembersByTypes: ObserveConversationMembersByTypesUseCase,
    private val notifyConversationIsOpen: NotifyConversationIsOpenUseCase,
    private val qualifiedIdMapper: QualifiedIdMapper,
    private val fetchConversationMLSVerificationStatus: FetchConversationMLSVerificationStatusUseCase,
    private val observeReactionsForMessage: ObserveReactionsForMessageUseCase,
    private val observeReceiptsForMessage: ObserveReceiptsForMessageUseCase,
    private val refreshHelper: CellAssetRefreshHelper,
    private val downloadCellFile: DownloadCellFileUseCase,
    private val getEditorUrl: GetEditorUrlUseCase,
    private val onlineEditor: OnlineEditor,
    private val featureFlags: KaliumConfigs,
    private val getWireCellsConfig: GetWireCellConfigurationUseCase,
    private val networkStateObserver: NetworkStateObserver,
    @CurrentAccount private val selfUserId: UserId,
) {
    fun conversationMessagesViewModel(savedStateHandle: SavedStateHandle) = ConversationMessagesViewModel(
        savedStateHandle = savedStateHandle,
        observeConversationDetails = observeConversationDetails,
        getMessageAsset = getMessageAsset,
        getMessageByIdUseCase = getMessageById,
        updateAssetMessageDownloadStatus = updateAssetMessageDownloadStatus,
        observeAssetStatusesUseCase = observeAssetStatuses,
        fileManager = fileManager,
        dispatchers = dispatchers,
        getMessageForConversation = getMessagesForConversation,
        fetchOlderNomadMessages = fetchOlderNomadMessages,
        toggleReaction = toggleReaction,
        resetSession = resetSession,
        audioMessagePlayer = audioMessagePlayer,
        getConversationUnreadEventsCount = getConversationUnreadEventsCount,
        clearUsersTypingEvents = clearUsersTypingEvents,
        getSearchedConversationMessagePosition = getSearchedConversationMessagePosition,
        deleteMessage = deleteMessage,
        isWireCellFeatureEnabled = isWireCellsEnabled,
        networkStateObserver = networkStateObserver,
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
        sharedState = messageSharedState,
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
        retryUpload = retryAttachmentUpload,
        uploadManager = cellUploadManager,
        fileManager = fileManager,
        sharedState = messageSharedState,
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

    fun mediaGalleryViewModel(savedStateHandle: SavedStateHandle) = MediaGalleryViewModel(
        savedStateHandle = savedStateHandle,
        getConversationDetails = observeConversationDetails,
        dispatchers = dispatchers,
        getImageData = getMessageAsset,
        fileManager = fileManager,
        deleteMessage = deleteMessage,
        getAttachment = getAttachment,
        getCellNode = getCellNode,
    )

    fun locationPickerViewModel() = LocationPickerViewModel(
        locationPickerHelper = locationPickerHelper,
    )

    fun conversationAssetMessagesViewModel(savedStateHandle: SavedStateHandle) = ConversationAssetMessagesViewModel(
        savedStateHandle = savedStateHandle,
        getImageMessages = getImageMessages,
        getAssetMessages = getAssetMessages,
        observeAssetStatuses = observeAssetStatuses,
    )

    fun imagesPreviewViewModel(savedStateHandle: SavedStateHandle) = ImagesPreviewViewModel(
        savedStateHandle = savedStateHandle,
        handleUriAsset = handleUriAsset,
        dispatchers = dispatchers,
    )

    fun messageDetailsViewModel(savedStateHandle: SavedStateHandle) = MessageDetailsViewModel(
        savedStateHandle = savedStateHandle,
        observeReactionsForMessage = observeReactionsForMessage,
        observeReceiptsForMessage = observeReceiptsForMessage,
    )

    fun quotedMultipartMessageViewModel() = QuotedMultipartMessageViewModel(
        observeQuotedMessage = observeQuotedMessage,
    )

    fun conversationBannerViewModel(savedStateHandle: SavedStateHandle) = ConversationBannerViewModel(
        savedStateHandle = savedStateHandle,
        observeConversationMembersByTypes = observeConversationMembersByTypes,
        observeConversationDetails = observeConversationDetails,
        notifyConversationIsOpen = notifyConversationIsOpen,
    )

    fun conversationInfoViewModel(savedStateHandle: SavedStateHandle) = ConversationInfoViewModel(
        qualifiedIdMapper = qualifiedIdMapper,
        savedStateHandle = savedStateHandle,
        observeConversationDetails = observeConversationDetails,
        fetchConversationMLSVerificationStatus = fetchConversationMLSVerificationStatus,
        isWireCellFeatureEnabled = isWireCellsEnabled,
        selfUserId = selfUserId,
    )

    fun multipartAttachmentsViewModel() = MultipartAttachmentsViewModelImpl(
        refreshHelper = refreshHelper,
        download = downloadCellFile,
        getEditorUrl = getEditorUrl,
        onlineEditor = onlineEditor,
        fileManager = fileManager,
        kaliumFileSystem = kaliumFileSystem,
        featureFlags = featureFlags,
        getWireCellsConfig = getWireCellsConfig,
    )
}
