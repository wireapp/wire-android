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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.mapper.ContactMapper
import com.wire.android.media.PingRinger
import com.wire.android.model.ImageAsset.PrivateAsset
import com.wire.android.model.SnackBarMessage
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_GROUP_DELETED_NAME
import com.wire.android.navigation.EXTRA_LEFT_GROUP
import com.wire.android.navigation.EXTRA_MESSAGE_TO_DELETE_ID
import com.wire.android.navigation.EXTRA_MESSAGE_TO_DELETE_IS_SELF
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.navigation.getBackNavArg
import com.wire.android.navigation.getBackNavArgs
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorDeletingMessage
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogHelper
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsState
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.android.ui.home.conversations.model.EditMessageBundle
import com.wire.android.ui.home.conversations.model.SendMessageBundle
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.util.FileManager
import com.wire.android.util.ImageUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.configuration.FileSharingStatus
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.IsInteractionAvailableResult
import com.wire.kalium.logic.feature.conversation.MembersToMentionUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationInteractionAvailabilityUseCase
import com.wire.kalium.logic.feature.conversation.ObserveSecurityClassificationLabelUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReadDateUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.RetryFailedMessageUseCase
import com.wire.kalium.logic.feature.message.SendEditTextMessageUseCase
import com.wire.kalium.logic.feature.message.SendKnockUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.message.ephemeral.EnqueueMessageSelfDeletionUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import com.wire.kalium.logic.functional.onFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import javax.inject.Inject
import com.wire.kalium.logic.data.id.QualifiedID as ConversationId

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class MessageComposerViewModel @Inject constructor(
    qualifiedIdMapper: QualifiedIdMapper,
    override val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val sendAssetMessage: ScheduleNewAssetMessageUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val sendEditTextMessage: SendEditTextMessageUseCase,
    private val retryFailedMessage: RetryFailedMessageUseCase,
    private val deleteMessage: DeleteMessageUseCase,
    private val dispatchers: DispatcherProvider,
    private val isFileSharingEnabled: IsFileSharingEnabledUseCase,
    private val observeConversationInteractionAvailability: ObserveConversationInteractionAvailabilityUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val kaliumFileSystem: KaliumFileSystem,
    private val updateConversationReadDate: UpdateConversationReadDateUseCase,
    private val observeSecurityClassificationLabel: ObserveSecurityClassificationLabelUseCase,
    private val contactMapper: ContactMapper,
    private val membersToMention: MembersToMentionUseCase,
    private val getAssetSizeLimit: GetAssetSizeLimitUseCase,
    private val sendKnockUseCase: SendKnockUseCase,
    private val enqueueMessageSelfDeletion: EnqueueMessageSelfDeletionUseCase,
    private val observeSelfDeletingMessages: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    private val persistNewSelfDeletingStatus: PersistNewSelfDeletionTimerUseCase,
    private val pingRinger: PingRinger,
    private val imageUtil: ImageUtil,
    private val fileManager: FileManager
) : SavedStateViewModel(savedStateHandle) {

    var messageComposerViewState by mutableStateOf(MessageComposerViewState())
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

    val conversationId: ConversationId = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    val deleteMessageHelper = DeleteMessageDialogHelper(
        viewModelScope,
        conversationId,
        ::updateDeleteDialogState
    ) { messageId, deleteForEveryone ->
        deleteMessage(conversationId = conversationId, messageId = messageId, deleteForEveryone = deleteForEveryone)
            .onFailure { onSnackbarMessage(ErrorDeletingMessage) }
    }

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    val infoMessage = _infoMessage.asSharedFlow()

    init {
        initTempWritableVideoUri()
        initTempWritableImageUri()
        fetchConversationClassificationType()
        observeIsTypingAvailable()
        observeSelfDeletingMessagesStatus()
        setFileSharingStatus()
    }

    fun onSnackbarMessage(type: SnackBarMessage) = viewModelScope.launch {
        _infoMessage.emit(type)
    }

    private fun observeIsTypingAvailable() = viewModelScope.launch {
        observeConversationInteractionAvailability(conversationId).collect { result ->
            messageComposerViewState = messageComposerViewState.copy(
                interactionAvailability = when (result) {
                    is IsInteractionAvailableResult.Failure -> InteractionAvailability.DISABLED
                    is IsInteractionAvailableResult.Success -> result.interactionAvailability
                }
            )
        }
    }

    private fun observeSelfDeletingMessagesStatus() = viewModelScope.launch {
        observeSelfDeletingMessages(conversationId, considerSelfUserSettings = true).collect { selfDeletingStatus ->
            messageComposerViewState = messageComposerViewState.copy(selfDeletionTimer = selfDeletingStatus)
        }
    }

    private fun fetchConversationClassificationType() = viewModelScope.launch {
        observeSecurityClassificationLabel(conversationId).collect { classificationType ->
            messageComposerViewState = messageComposerViewState.copy(securityClassificationType = classificationType)
        }
    }

    internal fun checkPendingActions() {
        // Check if there are messages to delete
        val messageToDeleteId = savedStateHandle.getBackNavArg<String>(EXTRA_MESSAGE_TO_DELETE_ID)

        val messageToDeleteIsSelf = savedStateHandle.getBackNavArg<Boolean>(EXTRA_MESSAGE_TO_DELETE_IS_SELF)

        val groupDeletedName = savedStateHandle.getBackNavArg<String>(EXTRA_GROUP_DELETED_NAME)

        val leftGroup = savedStateHandle.getBackNavArg(EXTRA_LEFT_GROUP) ?: false

        if (messageToDeleteId != null && messageToDeleteIsSelf != null) {
            showDeleteMessageDialog(messageToDeleteId, messageToDeleteIsSelf)
        }
        if (leftGroup || groupDeletedName != null) {
            navigateBack(savedStateHandle.getBackNavArgs())
        }
    }

    fun sendMessage(sendMessageBundle: SendMessageBundle) {
        viewModelScope.launch {
            sendTextMessage(
                conversationId = conversationId,
                text = sendMessageBundle.message,
                mentions = sendMessageBundle.mentions.map { it.intoMessageMention() },
                quotedMessageId = sendMessageBundle.quotedMessageId
            )
        }
    }

    fun sendEditMessage(editMessageBundle: EditMessageBundle) {
        viewModelScope.launch {
            sendEditTextMessage(
                conversationId = conversationId,
                originalMessageId = editMessageBundle.originalMessageId,
                text = editMessageBundle.newContent,
                mentions = editMessageBundle.newMentions.map { it.intoMessageMention() },
            )
        }
    }

    fun sendAttachmentMessage(attachmentBundle: AssetBundle?) {
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
                                assetMimeType = mimeType
                            )
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
                                    assetWidth = null
                                )
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

    fun sendPing() {
        viewModelScope.launch {
            pingRinger.ping(R.raw.ping_from_me, isReceivingPing = false)
            sendKnockUseCase(conversationId = conversationId, hotKnock = false)
        }
    }

    fun retrySendingMessage(messageId: String) {
        viewModelScope.launch {
            retryFailedMessage(messageId = messageId, conversationId = conversationId)
        }
    }

    private fun initTempWritableVideoUri() {
        viewModelScope.launch {
            tempWritableVideoUri = fileManager.getTempWritableVideoUri(kaliumFileSystem.rootCachePath)
        }
    }

    private fun initTempWritableImageUri() {
        viewModelScope.launch {
            tempWritableImageUri = fileManager.getTempWritableImageUri(kaliumFileSystem.rootCachePath)
        }
    }

    fun mentionMember(searchQuery: String?) {
        viewModelScope.launch(dispatchers.io()) {
            messageComposerViewState = messageComposerViewState.copy(
                mentionsToSelect = if (searchQuery == null) {
                    listOf()
                } else {
                    val members = membersToMention(conversationId, searchQuery)
                    members.map {
                        contactMapper.fromOtherUser(it.user as OtherUser)
                    }
                }
            )
        }
    }

    private fun setFileSharingStatus() {
        // TODO: handle restriction when sending assets
        viewModelScope.launch {
            when (isFileSharingEnabled().state) {
                FileSharingStatus.Value.Disabled,
                is FileSharingStatus.Value.EnabledSome ->
                    messageComposerViewState = messageComposerViewState.copy(isFileSharingEnabled = false)

                FileSharingStatus.Value.EnabledAll ->
                    messageComposerViewState = messageComposerViewState.copy(isFileSharingEnabled = true)
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
        (deleteMessageDialogsState as? DeleteMessageDialogsState.States)?.let { deleteMessageDialogsState = newValue(it) }

    fun updateConversationReadDate(utcISO: String) {
        viewModelScope.launch(dispatchers.io()) {
            updateConversationReadDate(conversationId, Instant.parse(utcISO))
        }
    }

    fun startSelfDeletion(uiMessage: UIMessage.Regular) {
        enqueueMessageSelfDeletion(conversationId, uiMessage.header.messageId)
    }

    fun updateSelfDeletingMessages(newSelfDeletionTimer: SelfDeletionTimer) = viewModelScope.launch {
        messageComposerViewState = messageComposerViewState.copy(selfDeletionTimer = newSelfDeletionTimer)
        persistNewSelfDeletingStatus(conversationId, newSelfDeletionTimer)
    }

    fun navigateBack(previousBackStackPassedArgs: Map<String, Any> = mapOf()) {
        viewModelScope.launch {
            navigationManager.navigateBack(previousBackStackPassedArgs)
        }
    }

    fun navigateToGallery(messageId: String, isSelfMessage: Boolean) {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.Gallery.getRouteWithArgs(
                        listOf(
                            PrivateAsset(wireSessionImageLoader, conversationId, messageId, isSelfMessage)
                        )
                    )
                )
            )
        }
    }

    fun attachmentPicked(attachmentUri: UriAsset) = viewModelScope.launch(dispatchers.io()) {
        val tempCachePath = kaliumFileSystem.rootCachePath
        val assetBundle = fileManager.getAssetBundleFromUri(attachmentUri.uri, tempCachePath)
        if (assetBundle != null) {
            // The max limit for sending assets changes between user and asset types.
            // Check [GetAssetSizeLimitUseCase] class for more detailed information about the real limits.
            val maxSizeLimitInBytes = getAssetSizeLimit(isImage = assetBundle.assetType == AttachmentType.IMAGE)
            if (assetBundle.dataSize <= maxSizeLimitInBytes) {
                sendAttachmentMessage(assetBundle)
            } else {
                if (attachmentUri.saveToDeviceIfInvalid) {
                    with(assetBundle) { fileManager.saveToExternalMediaStorage(fileName, dataPath, dataSize, mimeType, dispatchers) }
                }

                messageComposerViewState = messageComposerViewState.copy(
                    assetTooLargeDialogState = AssetTooLargeDialogState.Visible(
                        assetType = assetBundle.assetType,
                        maxLimitInMB = maxSizeLimitInBytes.div(sizeOf1MB).toInt(),
                        savedToDevice = attachmentUri.saveToDeviceIfInvalid
                    )
                )
            }
        } else {
            onSnackbarMessage(ConversationSnackbarMessages.ErrorPickingAttachment)
        }
    }

    fun hideAssetTooLargeError() {
        messageComposerViewState = messageComposerViewState.copy(assetTooLargeDialogState = AssetTooLargeDialogState.Hidden)
    }

    companion object {
        private const val sizeOf1MB = 1024 * 1024
    }
}
