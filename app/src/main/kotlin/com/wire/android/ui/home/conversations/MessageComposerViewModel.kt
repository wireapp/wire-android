/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.mapper.ContactMapper
import com.wire.android.media.PingRinger
import com.wire.android.model.SnackBarMessage
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorDeletingMessage
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogHelper
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsState
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.messagecomposer.state.ComposableMessageBundle
import com.wire.android.ui.home.messagecomposer.state.MessageBundle
import com.wire.android.ui.home.messagecomposer.state.Ping
import com.wire.android.ui.navArgs
import com.wire.android.util.FileManager
import com.wire.android.util.ImageUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getAudioLengthInMs
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.configuration.FileSharingStatus
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.conversation.Conversation.TypingIndicatorMode
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.failure.LegalHoldEnabledForConversationFailure
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageResult
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.IsInteractionAvailableResult
import com.wire.kalium.logic.feature.conversation.MembersToMentionUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationInteractionAvailabilityUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationUnderLegalHoldNotifiedUseCase
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
import com.wire.kalium.logic.functional.onFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import okio.Path
import okio.Path.Companion.toPath
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class MessageComposerViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val sendAssetMessage: ScheduleNewAssetMessageUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val sendEditTextMessage: SendEditTextMessageUseCase,
    private val retryFailedMessage: RetryFailedMessageUseCase,
    private val deleteMessage: DeleteMessageUseCase,
    private val dispatchers: DispatcherProvider,
    private val isFileSharingEnabled: IsFileSharingEnabledUseCase,
    private val observeConversationInteractionAvailability: ObserveConversationInteractionAvailabilityUseCase,
    private val kaliumFileSystem: KaliumFileSystem,
    private val updateConversationReadDate: UpdateConversationReadDateUseCase,
    private val contactMapper: ContactMapper,
    private val membersToMention: MembersToMentionUseCase,
    private val getAssetSizeLimit: GetAssetSizeLimitUseCase,
    private val sendKnockUseCase: SendKnockUseCase,
    private val enqueueMessageSelfDeletion: EnqueueMessageSelfDeletionUseCase,
    private val observeSelfDeletingMessages: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    private val persistNewSelfDeletingStatus: PersistNewSelfDeletionTimerUseCase,
    private val sendTypingEvent: SendTypingEventUseCase,
    private val pingRinger: PingRinger,
    private val imageUtil: ImageUtil,
    private val fileManager: FileManager,
    private val setUserInformedAboutVerification: SetUserInformedAboutVerificationUseCase,
    private val observeDegradedConversationNotified: ObserveDegradedConversationNotifiedUseCase,
    private val setNotifiedAboutConversationUnderLegalHold: SetNotifiedAboutConversationUnderLegalHoldUseCase,
    private val observeConversationUnderLegalHoldNotified: ObserveConversationUnderLegalHoldNotifiedUseCase,
) : SavedStateViewModel(savedStateHandle) {

    var messageComposerViewState = mutableStateOf(MessageComposerViewState())
        private set

    var tempWritableVideoUri: Uri? = null
        private set

    var tempWritableImageUri: Uri? = null
        private set

    // TODO: should be moved to ConversationMessagesViewModel?
    var deleteMessageDialogsState: DeleteMessageDialogsState by mutableStateOf(
        DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden,
            forEveryone = DeleteMessageDialogActiveState.Hidden
        )
    )
        private set

    private val conversationNavArgs: ConversationNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = conversationNavArgs.conversationId

    val deleteMessageHelper = DeleteMessageDialogHelper(
        viewModelScope,
        conversationId,
        ::updateDeleteDialogState
    ) { messageId, deleteForEveryone, _ ->
        deleteMessage(
            conversationId = conversationId,
            messageId = messageId,
            deleteForEveryone = deleteForEveryone
        )
            .onFailure { onSnackbarMessage(ErrorDeletingMessage) }
    }

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    val infoMessage = _infoMessage.asSharedFlow()

    var assetTooLargeDialogState: AssetTooLargeDialogState by mutableStateOf(
        AssetTooLargeDialogState.Hidden
    )

    var visitLinkDialogState: VisitLinkDialogState by mutableStateOf(
        VisitLinkDialogState.Hidden
    )

    var invalidLinkDialogState: InvalidLinkDialogState by mutableStateOf(
        InvalidLinkDialogState.Hidden
    )

    var sureAboutMessagingDialogState: SureAboutMessagingDialogState by mutableStateOf(
        SureAboutMessagingDialogState.Hidden
    )

    init {
        initTempWritableVideoUri()
        initTempWritableImageUri()
        observeIsTypingAvailable()
        observeSelfDeletingMessagesStatus()
        setFileSharingStatus()
    }

    private fun onSnackbarMessage(type: SnackBarMessage) = viewModelScope.launch {
        _infoMessage.emit(type)
    }

    private fun observeIsTypingAvailable() = viewModelScope.launch {
        observeConversationInteractionAvailability(conversationId).collect { result ->
            messageComposerViewState.value = messageComposerViewState.value.copy(
                interactionAvailability = when (result) {
                    is IsInteractionAvailableResult.Failure -> InteractionAvailability.DISABLED
                    is IsInteractionAvailableResult.Success -> result.interactionAvailability
                }
            )
        }
    }

    private fun observeSelfDeletingMessagesStatus() = viewModelScope.launch {
        observeSelfDeletingMessages(
            conversationId,
            considerSelfUserSettings = true
        ).collect { selfDeletingStatus ->
            messageComposerViewState.value =
                messageComposerViewState.value.copy(selfDeletionTimer = selfDeletingStatus)
        }
    }

    private suspend fun shouldInformAboutDegradedBeforeSendingMessage(): Boolean =
        observeDegradedConversationNotified(conversationId).first().let { !it }

    private suspend fun shouldInformAboutUnderLegalHoldBeforeSendingMessage() =
        observeConversationUnderLegalHoldNotified(conversationId).first().let { !it }

    fun trySendMessage(messageBundle: MessageBundle) {
        viewModelScope.launch {
            when {
                shouldInformAboutDegradedBeforeSendingMessage() ->
                    sureAboutMessagingDialogState = SureAboutMessagingDialogState.Visible.ConversationVerificationDegraded(messageBundle)
                shouldInformAboutUnderLegalHoldBeforeSendingMessage() ->
                    sureAboutMessagingDialogState =
                        SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold.BeforeSending(messageBundle)
                else -> sendMessage(messageBundle)
            }
        }
    }

    private suspend fun sendMessage(messageBundle: MessageBundle) {
        when (messageBundle) {
            is ComposableMessageBundle.EditMessageBundle -> {
                with(messageBundle) {
                    sendEditTextMessage(
                        conversationId = conversationId,
                        originalMessageId = originalMessageId,
                        text = newContent,
                        mentions = newMentions.map { it.intoMessageMention() },
                    ).handleLegalHoldFailureAfterSendingMessage()
                }
                sendTypingEvent(conversationId, TypingIndicatorMode.STOPPED)
            }

            is ComposableMessageBundle.AttachmentPickedBundle -> {
                handleAssetMessageBundle(
                    attachmentUri = messageBundle.attachmentUri
                )
            }

            is ComposableMessageBundle.AudioMessageBundle -> {
                handleAssetMessageBundle(
                    attachmentUri = messageBundle.attachmentUri,
                    audioPath = messageBundle.attachmentUri.uri.path?.toPath()
                )
            }

            is ComposableMessageBundle.SendTextMessageBundle -> {
                with(messageBundle) {
                    sendTextMessage(
                        conversationId = conversationId,
                        text = message,
                        mentions = mentions.map { it.intoMessageMention() },
                        quotedMessageId = quotedMessageId
                    ).handleLegalHoldFailureAfterSendingMessage()
                }
                sendTypingEvent(conversationId, TypingIndicatorMode.STOPPED)
            }

            Ping -> {
                pingRinger.ping(R.raw.ping_from_me, isReceivingPing = false)
                sendKnockUseCase(conversationId = conversationId, hotKnock = false)
            }
        }
    }

    private suspend fun handleAssetMessageBundle(
        attachmentUri: UriAsset,
        audioPath: Path? = null
    ) {
        val tempCachePath = kaliumFileSystem.rootCachePath
        val assetBundle = fileManager.getAssetBundleFromUri(
            attachmentUri = attachmentUri.uri,
            tempCachePath = tempCachePath,
            audioPath = audioPath
        )
        if (assetBundle != null) {
            // The max limit for sending assets changes between user and asset types.
            // Check [GetAssetSizeLimitUseCase] class for more detailed information about the real limits.
            val maxSizeLimitInBytes =
                getAssetSizeLimit(isImage = assetBundle.assetType == AttachmentType.IMAGE)
            handleBundle(assetBundle, maxSizeLimitInBytes, attachmentUri)
        } else {
            onSnackbarMessage(ConversationSnackbarMessages.ErrorPickingAttachment)
        }
    }

    private suspend fun handleBundle(
        assetBundle: AssetBundle,
        maxSizeLimitInBytes: Long,
        attachmentUri: UriAsset
    ) {
        if (assetBundle.dataSize <= maxSizeLimitInBytes) {
            sendAttachment(assetBundle)
        } else {
            if (attachmentUri.saveToDeviceIfInvalid) {
                with(assetBundle) {
                    fileManager.saveToExternalMediaStorage(
                        fileName,
                        dataPath,
                        dataSize,
                        mimeType,
                        dispatchers
                    )
                }
            }
            assetTooLargeDialogState = AssetTooLargeDialogState.Visible(
                assetType = assetBundle.assetType,
                maxLimitInMB = maxSizeLimitInBytes.div(sizeOf1MB).toInt(),
                savedToDevice = attachmentUri.saveToDeviceIfInvalid
            )
        }
    }

    internal fun sendAttachment(attachmentBundle: AssetBundle?) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                attachmentBundle?.run {
                    when (assetType) {
                        AttachmentType.IMAGE -> {
                            val (imgWidth, imgHeight) = imageUtil.extractImageWidthAndHeight(
                                kaliumFileSystem,
                                attachmentBundle.dataPath
                            )
                            sendAssetMessage(
                                conversationId = conversationId,
                                assetDataPath = dataPath,
                                assetName = fileName,
                                assetWidth = imgWidth,
                                assetHeight = imgHeight,
                                assetDataSize = dataSize,
                                assetMimeType = mimeType,
                                audioLengthInMs = 0L
                            ).handleLegalHoldFailureAfterSendingMessage()
                        }

                        AttachmentType.VIDEO,
                        AttachmentType.GENERIC_FILE,
                        AttachmentType.AUDIO -> {
                            try {
                                sendAssetMessage(
                                    conversationId = conversationId,
                                    assetDataPath = dataPath,
                                    assetName = fileName,
                                    assetMimeType = mimeType,
                                    assetDataSize = dataSize,
                                    assetHeight = null,
                                    assetWidth = null,
                                    audioLengthInMs = getAudioLengthInMs(
                                        dataPath = dataPath,
                                        mimeType = mimeType
                                    )
                                ).handleLegalHoldFailureAfterSendingMessage()
                            } catch (e: OutOfMemoryError) {
                                appLogger.e("There was an OutOfMemory error while uploading the asset")
                                onSnackbarMessage(ConversationSnackbarMessages.ErrorSendingAsset)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun CoreFailure.handleLegalHoldFailureAfterSendingMessage() = also {
        if (this is LegalHoldEnabledForConversationFailure) {
            sureAboutMessagingDialogState = SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold.AfterSending(this.messageId)
        }
    }
    private fun Either<CoreFailure, Unit>.handleLegalHoldFailureAfterSendingMessage() =
        onFailure { it.handleLegalHoldFailureAfterSendingMessage() }
    private fun ScheduleNewAssetMessageResult.handleLegalHoldFailureAfterSendingMessage() = also {
        if (it is ScheduleNewAssetMessageResult.Failure) {
            it.coreFailure.handleLegalHoldFailureAfterSendingMessage()
        }
    }

    fun retrySendingMessage(messageId: String) {
        viewModelScope.launch {
            retryFailedMessage(messageId = messageId, conversationId = conversationId)
        }
    }

    private fun initTempWritableVideoUri() {
        viewModelScope.launch {
            tempWritableVideoUri =
                fileManager.getTempWritableVideoUri(kaliumFileSystem.rootCachePath)
        }
    }

    private fun initTempWritableImageUri() {
        viewModelScope.launch {
            tempWritableImageUri =
                fileManager.getTempWritableImageUri(kaliumFileSystem.rootCachePath)
        }
    }

    fun searchMembersToMention(searchQuery: String) {
        viewModelScope.launch(dispatchers.io()) {
            val members = membersToMention(conversationId, searchQuery).map {
                contactMapper.fromOtherUser(it.user as OtherUser)
            }

            messageComposerViewState.value =
                messageComposerViewState.value.copy(mentionSearchResult = members)
        }
    }

    fun clearMentionSearchResult() {
        messageComposerViewState.value =
            messageComposerViewState.value.copy(mentionSearchResult = emptyList())
    }

    private fun setFileSharingStatus() {
        // TODO: handle restriction when sending assets
        viewModelScope.launch {
            messageComposerViewState.value = when (isFileSharingEnabled().state) {
                FileSharingStatus.Value.Disabled,
                is FileSharingStatus.Value.EnabledSome ->
                    messageComposerViewState.value.copy(isFileSharingEnabled = false)

                FileSharingStatus.Value.EnabledAll ->
                    messageComposerViewState.value.copy(isFileSharingEnabled = true)
            }
        }
    }

    fun showDeleteMessageDialog(messageId: String, deleteForEveryone: Boolean) =
        if (deleteForEveryone) {
            updateDeleteDialogState {
                it.copy(
                    forEveryone = DeleteMessageDialogActiveState.Visible(
                        messageId = messageId,
                        conversationId = conversationId
                    )
                )
            }
        } else {
            updateDeleteDialogState {
                it.copy(
                    forYourself = DeleteMessageDialogActiveState.Visible(
                        messageId = messageId,
                        conversationId = conversationId
                    )
                )
            }
        }

    private fun updateDeleteDialogState(newValue: (DeleteMessageDialogsState.States) -> DeleteMessageDialogsState) =
        (deleteMessageDialogsState as? DeleteMessageDialogsState.States)?.let {
            deleteMessageDialogsState = newValue(it)
        }

    fun updateConversationReadDate(utcISO: String) {
        viewModelScope.launch(dispatchers.io()) {
            updateConversationReadDate(conversationId, Instant.parse(utcISO))
        }
    }

    fun startSelfDeletion(uiMessage: UIMessage) {
        enqueueMessageSelfDeletion(conversationId, uiMessage.header.messageId)
    }

    fun updateSelfDeletingMessages(newSelfDeletionTimer: SelfDeletionTimer) =
        viewModelScope.launch {
            messageComposerViewState.value =
                messageComposerViewState.value.copy(selfDeletionTimer = newSelfDeletionTimer)
            persistNewSelfDeletingStatus(conversationId, newSelfDeletionTimer)
        }

    fun hideAssetTooLargeError() {
        assetTooLargeDialogState = AssetTooLargeDialogState.Hidden
    }

    fun hideVisitLinkDialog() {
        visitLinkDialogState = VisitLinkDialogState.Hidden
    }

    fun hideInvalidLinkError() {
        invalidLinkDialogState = InvalidLinkDialogState.Hidden
    }

    fun sendTypingEvent(typingIndicatorMode: TypingIndicatorMode) {
        viewModelScope.launch {
            sendTypingEvent(conversationId, typingIndicatorMode)
        }
    }

    fun acceptSureAboutSendingMessage() {
        (sureAboutMessagingDialogState as? SureAboutMessagingDialogState.Visible)?.let {
            viewModelScope.launch {
                it.markAsNotified()
                when (it) {
                    is SureAboutMessagingDialogState.Visible.ConversationVerificationDegraded ->
                        trySendMessage(it.messageBundleToSend)
                    is SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold.BeforeSending ->
                        trySendMessage(it.messageBundleToSend)
                    is SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold.AfterSending ->
                        retrySendingMessage(it.messageId)
                }
            }
        }
    }

    fun dismissSureAboutSendingMessage() {
        (sureAboutMessagingDialogState as? SureAboutMessagingDialogState.Visible)?.let {
            viewModelScope.launch {
                it.markAsNotified()
            }
        }
    }

    private suspend fun SureAboutMessagingDialogState.markAsNotified() {
        when (this) {
            is SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold ->
                setNotifiedAboutConversationUnderLegalHold(conversationId)

            is SureAboutMessagingDialogState.Visible.ConversationVerificationDegraded ->
                setUserInformedAboutVerification(conversationId)

            SureAboutMessagingDialogState.Hidden -> { /* do nothing */ }
        }
        sureAboutMessagingDialogState = SureAboutMessagingDialogState.Hidden
    }

    companion object {
        private const val sizeOf1MB = 1024 * 1024
    }
}
