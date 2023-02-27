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
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorMaxAssetSize
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorMaxImageSize
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogHelper
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsState
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.ui.home.messagecomposer.UiMention
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.FileManager
import com.wire.android.util.ImageUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageResult
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.IsInteractionAvailableResult
import com.wire.kalium.logic.feature.conversation.MembersToMentionUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationInteractionAvailabilityUseCase
import com.wire.kalium.logic.feature.conversation.ObserveSecurityClassificationLabelUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReadDateUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.SendKnockUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import com.wire.kalium.logic.functional.onFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import okio.Path
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
    private val deleteMessage: DeleteMessageUseCase,
    private val dispatchers: DispatcherProvider,
    private val getSelfUserTeam: GetSelfTeamUseCase,
    private val isFileSharingEnabled: IsFileSharingEnabledUseCase,
    private val observeConversationInteractionAvailability: ObserveConversationInteractionAvailabilityUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val kaliumFileSystem: KaliumFileSystem,
    private val updateConversationReadDateUseCase: UpdateConversationReadDateUseCase,
    private val observeSecurityClassificationLabel: ObserveSecurityClassificationLabelUseCase,
    private val contactMapper: ContactMapper,
    private val membersToMention: MembersToMentionUseCase,
    private val getAssetSizeLimit: GetAssetSizeLimitUseCase,
    private val sendKnockUseCase: SendKnockUseCase,
    private val pingRinger: PingRinger,
    private val imageUtil: ImageUtil,
    private val fileManager: FileManager
) : SavedStateViewModel(savedStateHandle) {

    var conversationViewState by mutableStateOf(ConversationViewState())
        private set

    var tempWritableVideoUri: Uri? = null
        private set

    var tempWritableImageUri: Uri? = null
        private set

    var interactionAvailability by mutableStateOf(InteractionAvailability.ENABLED)

    var mentionsToSelect by mutableStateOf<List<Contact>>(listOf())

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
        observeIsTypingAvailable()
        fetchSelfUserTeam()
        fetchConversationClassificationType()
        setFileSharingStatus()
        initTempWritableVideoUri()
        initTempWritableImageUri()
    }

    fun onSnackbarMessage(type: SnackBarMessage) = viewModelScope.launch {
        _infoMessage.emit(type)
    }

    private fun observeIsTypingAvailable() = viewModelScope.launch {
        observeConversationInteractionAvailability(conversationId).collect { result ->
            interactionAvailability = when (result) {
                is IsInteractionAvailableResult.Failure -> InteractionAvailability.DISABLED
                is IsInteractionAvailableResult.Success -> result.interactionAvailability
            }
        }
    }

    private fun fetchSelfUserTeam() = viewModelScope.launch {
        getSelfUserTeam().collect {
            conversationViewState = conversationViewState.copy(userTeam = it)
        }
    }

    private fun fetchConversationClassificationType() = viewModelScope.launch {
        observeSecurityClassificationLabel(conversationId).collect { classificationType ->
            conversationViewState = conversationViewState.copy(securityClassificationType = classificationType)
        }
    }

    internal fun checkPendingActions() {
        // Check if there are messages to delete
        val messageToDeleteId = savedStateHandle
            .getBackNavArg<String>(EXTRA_MESSAGE_TO_DELETE_ID)
        val messageToDeleteIsSelf = savedStateHandle
            .getBackNavArg<Boolean>(EXTRA_MESSAGE_TO_DELETE_IS_SELF)

        val groupDeletedName = savedStateHandle
            .getBackNavArg<String>(EXTRA_GROUP_DELETED_NAME)

        val leftGroup = savedStateHandle
            .getBackNavArg(EXTRA_LEFT_GROUP) ?: false

        if (messageToDeleteId != null && messageToDeleteIsSelf != null) {
            showDeleteMessageDialog(messageToDeleteId, messageToDeleteIsSelf)
        }
        if (leftGroup || groupDeletedName != null) {
            navigateBack(savedStateHandle.getBackNavArgs())
        }
    }

    fun sendMessage(
        message: String,
        mentions: List<UiMention>,
        quotedMessageId: String?
    ) {
        viewModelScope.launch {
            sendTextMessage(
                conversationId = conversationId,
                text = message,
                mentions = mentions.map { it.intoMessageMention() },
                quotedMessageId = quotedMessageId
            )
        }
    }

    @Suppress("MagicNumber")
    fun sendAttachmentMessage(attachmentBundle: AttachmentBundle?) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                attachmentBundle?.run {
                    when (attachmentType) {
                        AttachmentType.IMAGE -> {
                            if (dataSize > getAssetSizeLimit(isImage = true)) onSnackbarMessage(ErrorMaxImageSize)
                            else {
                                val (imgWidth, imgHeight) = imageUtil.extractImageWidthAndHeight(
                                    kaliumFileSystem,
                                    attachmentBundle.dataPath
                                )
                                val result = sendAssetMessage(
                                    conversationId = conversationId,
                                    assetDataPath = dataPath,
                                    assetName = fileName,
                                    assetWidth = imgWidth,
                                    assetHeight = imgHeight,
                                    assetDataSize = dataSize,
                                    assetMimeType = mimeType
                                )
                                if (result is ScheduleNewAssetMessageResult.Failure) {
                                    onSnackbarMessage(ConversationSnackbarMessages.ErrorSendingImage)
                                }
                            }
                        }

                        AttachmentType.GENERIC_FILE -> {
                            // The max limit for sending assets changes between user types. Currently, is25MB for free users, and 100MB for
                            // users that belong to a team
                            val assetLimitInBytes = getAssetLimitInBytes()
                            val sizeOf1MB = 1024 * 1024
                            if (dataSize > assetLimitInBytes) {
                                onSnackbarMessage(ErrorMaxAssetSize(assetLimitInBytes.div(sizeOf1MB)))
                            } else {
                                try {
                                    val result = sendAssetMessage(
                                        conversationId = conversationId,
                                        assetDataPath = dataPath,
                                        assetName = fileName,
                                        assetMimeType = mimeType,
                                        assetDataSize = dataSize,
                                        assetHeight = null,
                                        assetWidth = null
                                    )
                                    if (result is ScheduleNewAssetMessageResult.Failure) {
                                        onSnackbarMessage(ConversationSnackbarMessages.ErrorSendingAsset)
                                    }
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
    }

    fun sendPing() {
        viewModelScope.launch {
            pingRinger.ping(R.raw.ping_from_me, isReceivingPing = false)
            sendKnockUseCase(conversationId = conversationId, hotKnock = false)
        }
    }

    private fun initTempWritableVideoUri() {
        viewModelScope.launch {
            tempWritableVideoUri = fileManager.getTempWritableVideoUri(provideTempCachePath())
        }
    }

    private fun initTempWritableImageUri() {
        viewModelScope.launch {
            tempWritableImageUri = fileManager.getTempWritableImageUri(provideTempCachePath())
        }
    }

    fun mentionMember(searchQuery: String?) {
        viewModelScope.launch(dispatchers.io()) {
            mentionsToSelect = if (searchQuery == null) {
                listOf()
            } else {
                val members = membersToMention(conversationId, searchQuery)
                members.map {
                    contactMapper.fromOtherUser(it.user as OtherUser)
                }
            }
        }
    }

    private fun setFileSharingStatus() {
        viewModelScope.launch {
            if (isFileSharingEnabled().isFileSharingEnabled != null) {
                conversationViewState = conversationViewState.copy(isFileSharingEnabled = isFileSharingEnabled().isFileSharingEnabled!!)
            }
        }
    }

    private suspend fun getAssetLimitInBytes(): Int {
        // Users with a team attached have larger asset sending limits than default users
        return getAssetSizeLimit(false).toInt()
    }

    fun showDeleteMessageDialog(messageId: String, isMyMessage: Boolean) =
        if (isMyMessage) {
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
            updateConversationReadDateUseCase(conversationId, Instant.parse(utcISO))
        }
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

    fun provideTempCachePath(): Path = kaliumFileSystem.rootCachePath
}
