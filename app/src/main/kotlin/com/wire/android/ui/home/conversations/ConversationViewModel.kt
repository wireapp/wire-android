package com.wire.android.ui.home.conversations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.model.ImageAsset.PrivateAsset
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserStatus
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_MESSAGE_TO_DELETE
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorMaxAssetSize
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorMaxImageSize
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorOpeningAssetFile
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.OnFileDownloaded
import com.wire.android.ui.home.conversations.DownloadedAssetDialogVisibilityState.Displayed
import com.wire.android.ui.home.conversations.DownloadedAssetDialogVisibilityState.Hidden
import com.wire.android.ui.home.conversations.delete.MessageDeletion
import com.wire.android.ui.home.conversations.delete.parseIntoMessageDeletion
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.android.ui.home.conversations.model.MessageContent.AssetMessage
import com.wire.android.ui.home.conversations.model.MessageContent.DeletedMessage
import com.wire.android.ui.home.conversations.model.MessageContent.TextMessage
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageViewWrapper
import com.wire.android.ui.home.conversations.model.User
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.extractImageParams
import com.wire.android.util.getConversationColor
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.conversation.UserType
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.AssetContent.AssetMetadata.Image
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.Message.DownloadStatus.FAILED
import com.wire.kalium.logic.data.message.Message.DownloadStatus.IN_PROGRESS
import com.wire.kalium.logic.data.message.Message.DownloadStatus.SAVED_EXTERNALLY
import com.wire.kalium.logic.data.message.Message.DownloadStatus.SAVED_INTERNALLY
import com.wire.kalium.logic.data.message.MessageContent.Asset
import com.wire.kalium.logic.data.message.MessageContent.Text
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
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
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.util.toStringDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.wire.kalium.logic.data.id.QualifiedID as ConversationId

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class ConversationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val getMessages: GetRecentMessagesUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeMemberDetails: ObserveConversationMembersUseCase,
    private val sendImageMessage: SendImageMessageUseCase,
    private val sendAssetMessage: SendAssetMessageUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val getMessageAsset: GetMessageAssetUseCase,
    private val deleteMessage: DeleteMessageUseCase,
    private val dispatchers: DispatcherProvider,
    private val markMessagesAsNotified: MarkMessagesAsNotifiedUseCase,
    private val updateAssetMessageDownloadStatus: UpdateAssetMessageDownloadStatusUseCase,
    private val getSelfUserTeam: GetSelfTeamUseCase,
    private val fileManager: FileManager,
    private val userTypeMapper: UserTypeMapper
) : ViewModel() {

    var conversationViewState by mutableStateOf(ConversationViewState())
        private set

    var deleteMessageDialogsState: DeleteMessageDialogsState by mutableStateOf(
        DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden,
            forEveryone = DeleteMessageDialogActiveState.Hidden
        )
    )
        private set

    val conversationId: ConversationId = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    val messageToDeleteId: MessageDeletion? = savedStateHandle
        .get<String>(EXTRA_MESSAGE_TO_DELETE)
        ?.parseIntoMessageDeletion()

    init {
        fetchMessages()
        listenConversationDetails()
        fetchSelfUserTeam()
        setMessagesAsNotified()
        checkPendingActions()
    }

    // region ------------------------------ Init Methods -------------------------------------
    private fun fetchMessages() = viewModelScope.launch {
        getMessages(conversationId).combine(observeMemberDetails(conversationId)) { messages, members ->
            messages.toUIMessages(members)
        }.flowOn(dispatchers.default()).collect { uiMessages ->
            conversationViewState = conversationViewState.copy(messages = uiMessages)
        }
    }

    private fun listenConversationDetails() = viewModelScope.launch {
        observeConversationDetails(conversationId).collect { conversationDetails ->
            val conversationName = when (conversationDetails) {
                is ConversationDetails.OneOne -> conversationDetails.otherUser.name.orEmpty()
                else -> conversationDetails.conversation.name.orEmpty()
            }
            val conversationAvatar = when (conversationDetails) {
                is ConversationDetails.OneOne ->
                    ConversationAvatar.OneOne(conversationDetails.otherUser.previewPicture?.let { UserAvatarAsset(it) })
                is ConversationDetails.Group ->
                    ConversationAvatar.Group(getConversationColor(conversationDetails.conversation.id))
                else -> ConversationAvatar.None
            }
            conversationViewState = conversationViewState.copy(
                conversationName = conversationName,
                conversationAvatar = conversationAvatar
            )
        }
    }

    private fun fetchSelfUserTeam() = viewModelScope.launch {
        getSelfUserTeam().collect {
            conversationViewState = conversationViewState.copy(userTeam = it)
        }
    }

    private fun setMessagesAsNotified() = viewModelScope.launch {
        markMessagesAsNotified(conversationId, System.currentTimeMillis().toStringDate()) //TODO Failure is ignored
    }

    private fun checkPendingActions() {
        messageToDeleteId?.run {
            showDeleteMessageDialog(messageToDeleteId, isSelfMessage)
        }
    }
    // endregion

    // region ------------------------------ UI triggered actions -----------------------------
    fun onMessageChanged(message: String) {
        conversationViewState = conversationViewState.copy(messageText = message)
    }

    fun sendMessage() {
        viewModelScope.launch {
            sendTextMessage(conversationId, conversationViewState.messageText)
        }
        conversationViewState = conversationViewState.copy(messageText = "")
    }

    @Suppress("MagicNumber")
    fun sendAttachmentMessage(attachmentBundle: AttachmentBundle?) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                attachmentBundle?.run {
                    when (attachmentType) {
                        AttachmentType.IMAGE -> {
                            if (rawContent.size > IMAGE_SIZE_LIMIT_BYTES) onSnackbarMessage(ErrorMaxImageSize)
                            else {
                                val (imgWidth, imgHeight) = extractImageParams(attachmentBundle.rawContent)
                                val result = sendImageMessage(
                                    conversationId = conversationId,
                                    imageRawData = attachmentBundle.rawContent,
                                    imageName = attachmentBundle.fileName,
                                    imgWidth = imgWidth,
                                    imgHeight = imgHeight
                                )
                                if (result is SendImageMessageResult.Failure) {
                                    onSnackbarMessage(ConversationSnackbarMessages.ErrorSendingImage)
                                }
                            }
                        }
                        AttachmentType.GENERIC_FILE -> {
                            // The max limit for sending assets changes between user types. Currently, is25MB for free users, and 100MB for
                            // users that belong to a team
                            val assetLimitInBytes = getAssetLimitInBytes()
                            val sizeOf1MB = 1024 * 1024
                            if (rawContent.size > assetLimitInBytes) {
                                onSnackbarMessage(ErrorMaxAssetSize(assetLimitInBytes.div(sizeOf1MB)))
                            } else {
                                try {
                                    val result = sendAssetMessage(
                                        conversationId = conversationId,
                                        assetRawData = attachmentBundle.rawContent,
                                        assetName = attachmentBundle.fileName,
                                        assetMimeType = attachmentBundle.mimeType
                                    )
                                    if (result is SendAssetMessageResult.Failure) {
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

    // This will download the asset remotely to an internal temporary storage or fetch it from the local database if it had been previously
    // downloaded. After doing so, a dialog is shown to ask the user whether he wants to open the file or download it to external storage
    fun downloadOrFetchAssetToInternalStorage(messageId: String) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                try {
                    val assetMessage = conversationViewState.messages.firstOrNull {
                        it.messageHeader.messageId == messageId && it.messageContent is AssetMessage
                    }

                    val (isAssetDownloadedInternally, assetName) = (assetMessage?.messageContent as AssetMessage).run {
                        (downloadStatus == SAVED_INTERNALLY || downloadStatus == IN_PROGRESS) to assetName
                    }

                    if (!isAssetDownloadedInternally)
                        updateAssetMessageDownloadStatus(IN_PROGRESS, conversationId, messageId)

                    val resultData = getRawAssetData(conversationId, messageId)
                    updateAssetMessageDownloadStatus(if (resultData != null) SAVED_INTERNALLY else FAILED, conversationId, messageId)

                    if (resultData != null) {
                        showOnAssetDownloadedDialog(assetName, resultData, messageId)
                    }
                } catch (e: OutOfMemoryError) {
                    appLogger.e("There was an OutOfMemory error while downloading the asset")
                    onSnackbarMessage(ConversationSnackbarMessages.ErrorSendingAsset)
                }
            }
        }
    }

    fun showOnAssetDownloadedDialog(assetName: String, assetData: ByteArray, messageId: String) {
        conversationViewState = conversationViewState.copy(downloadedAssetDialogState = Displayed(assetName, assetData, messageId))
    }

    fun hideOnAssetDownloadedDialog() {
        conversationViewState = conversationViewState.copy(downloadedAssetDialogState = Hidden)
    }

    private fun getAssetLimitInBytes(): Int {
        // Users with a team attached have larger asset sending limits than default users
        return conversationViewState.userTeam?.run {
            ASSET_SIZE_TEAM_USER_LIMIT_BYTES
        } ?: ASSET_SIZE_DEFAULT_LIMIT_BYTES
    }

    fun onSnackbarMessage(msgCode: ConversationSnackbarMessages) {
        viewModelScope.launch {
            // We need to reset the onSnackbarMessage state so that it doesn't show up again when going -> background -> resume back
            // The delay added, is to ensure the snackbar message will have enough time to be shown before it is reset to null
            conversationViewState = conversationViewState.copy(onSnackbarMessage = msgCode)
            delay(SNACKBAR_MESSAGE_DELAY)
            conversationViewState = conversationViewState.copy(onSnackbarMessage = null)
        }
    }

    fun showDeleteMessageDialog(messageId: String, isMyMessage: Boolean) =
        if (isMyMessage) {
            updateDeleteDialogState {
                it.copy(forEveryone = DeleteMessageDialogActiveState.Visible(messageId = messageId, conversationId = conversationId))
            }
        } else {
            updateDeleteDialogState {
                it.copy(forYourself = DeleteMessageDialogActiveState.Visible(messageId = messageId, conversationId = conversationId))
            }
        }

    fun showDeleteMessageForYourselfDialog(messageId: String) {
        updateDeleteDialogState { it.copy(forEveryone = DeleteMessageDialogActiveState.Hidden) }
        updateDeleteDialogState {
            it.copy(
                forYourself = DeleteMessageDialogActiveState.Visible(
                    messageId = messageId,
                    conversationId = conversationId
                )
            )
        }
    }

    fun onDeleteDialogDismissed() {
        updateDeleteDialogState {
            it.copy(
                forEveryone = DeleteMessageDialogActiveState.Hidden,
                forYourself = DeleteMessageDialogActiveState.Hidden
            )
        }
    }

    fun clearDeleteMessageError() {
        updateStateIfDialogVisible { it.copy(error = DeleteMessageError.None) }
    }

    private fun updateDeleteDialogState(newValue: (DeleteMessageDialogsState.States) -> DeleteMessageDialogsState) =
        (deleteMessageDialogsState as? DeleteMessageDialogsState.States)?.let { deleteMessageDialogsState = newValue(it) }

    private fun updateStateIfDialogVisible(newValue: (DeleteMessageDialogActiveState.Visible) -> DeleteMessageDialogActiveState) =
        updateDeleteDialogState {
            when {
                it.forEveryone is DeleteMessageDialogActiveState.Visible -> it.copy(forEveryone = newValue(it.forEveryone))
                it.forYourself is DeleteMessageDialogActiveState.Visible -> it.copy(
                    forYourself = newValue(
                        it.forYourself
                    )
                )
                else -> it
            }
        }

    fun deleteMessage(messageId: String, deleteForEveryone: Boolean) = viewModelScope.launch {
        //update dialogs state to loading
        if (deleteForEveryone) {
            updateDeleteDialogState {
                it.copy(
                    forEveryone = DeleteMessageDialogActiveState.Visible(
                        messageId = messageId,
                        conversationId = conversationId,
                        loading = true
                    )
                )
            }
        } else {
            updateDeleteDialogState {
                it.copy(
                    forYourself = DeleteMessageDialogActiveState.Visible(
                        messageId = messageId,
                        conversationId = conversationId,
                        loading = true
                    )
                )
            }
        }
        deleteMessage(conversationId = conversationId, messageId = messageId, deleteForEveryone = deleteForEveryone)
        onDeleteDialogDismissed()
    }

    private suspend fun getRawAssetData(conversationId: ConversationId, messageId: String): ByteArray? {
        getMessageAsset(
            conversationId = conversationId,
            messageId = messageId
        ).run {
            return when (this) {
                is MessageAssetResult.Success -> decodedAsset
                else -> null
            }
        }
    }

    fun onOpenFileWithExternalApp(assetName: String, assetData: ByteArray) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                fileManager.openWithExternalApp(assetName, assetData) { onOpenFileError() }
                hideOnAssetDownloadedDialog()
            }
        }
    }

    fun onSaveFile(assetName: String, assetData: ByteArray, messageId: String) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                fileManager.saveToExternalStorage(assetName, assetData) {
                    updateAssetMessageDownloadStatus(SAVED_EXTERNALLY, conversationId, messageId)
                    onFileSavedToExternalStorage(assetName)
                    hideOnAssetDownloadedDialog()
                }
            }
        }
    }

    private fun onOpenFileError() {
        conversationViewState = conversationViewState.copy(onSnackbarMessage = ErrorOpeningAssetFile)
    }

    private fun onFileSavedToExternalStorage(assetName: String?) {
        onSnackbarMessage(OnFileDownloaded(assetName))
    }
    // endregion

    // region ------------------------------ Navigation ------------------------------
    fun navigateBack() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

    fun navigateToInitiatingCallScreen() {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.InitiatingCall.getRouteWithArgs(listOf(conversationId))
                )
            )
        }
    }

    fun navigateToGallery(messageId: String) {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.Gallery.getRouteWithArgs(listOf(PrivateAsset(conversationId, messageId)))
                )
            )
        }
    }
    // endregion

    // region ------------------------------ Mapper Helpers ------------------------------
    private suspend fun List<Message>.toUIMessages(members: List<MemberDetails>): List<MessageViewWrapper> {
        return map { message ->
            val sender = members.findSender(message.senderUserId)

            MessageViewWrapper(
                messageContent = fromMessageModelToMessageContent(message),
                messageSource = if (sender is MemberDetails.Self) MessageSource.Self else MessageSource.OtherUser,
                messageHeader = MessageHeader(
                    // TODO: Designs for deleted users?
                    username = sender.name?.let { UIText.DynamicString(it) } ?: UIText.StringResource(R.string.member_name_deleted_label),
                    membership = if (sender is MemberDetails.Other) userTypeMapper.toMembership(sender.userType) else Membership.None,
                    isLegalHold = false,
                    time = message.date,
                    messageStatus = if (message.status == Message.Status.FAILED) MessageStatus.SendFailure else MessageStatus.Untouched,
                    messageId = message.id
                ),
                user = User(
                    avatarAsset = sender.previewAsset, availabilityStatus = UserStatus.NONE
                )
            )
        }
    }

    private suspend fun fromMessageModelToMessageContent(message: Message): MessageContent? =
        when (message.visibility) {
            Message.Visibility.VISIBLE -> when (val content = message.content) {
                is Asset -> mapToMessageUI(content.value, message.conversationId, message.id)
                is Text -> TextMessage(messageBody = MessageBody(UIText.DynamicString(content.value)))
                else -> TextMessage(messageBody = MessageBody((content as? Text)?.let { UIText.DynamicString(it.value) }
                    ?: UIText.StringResource(R.string.content_is_not_available)))
            }
            Message.Visibility.DELETED -> DeletedMessage
            Message.Visibility.HIDDEN -> DeletedMessage
        }

    private suspend fun mapToMessageUI(assetContent: AssetContent, conversationId: ConversationId, messageId: String): MessageContent? {
        with(assetContent) {
            val (imgWidth, imgHeight) = when (val md = metadata) {
                is Image -> md.width to md.height
                else -> 0 to 0
            }

            return if (remoteData.assetId.isNotEmpty()) {
                when {
                    // If it's an image, we download it right away
                    mimeType.contains("image") -> MessageContent.ImageMessage(
                        assetId = remoteData.assetId,
                        rawImgData = getRawAssetData(conversationId, messageId),
                        width = imgWidth,
                        height = imgHeight
                    )

                    // It's a generic Asset Message so let's not download it yet
                    else -> {
                        AssetMessage(
                            assetName = name ?: "",
                            assetExtension = name?.split(".")?.last() ?: "",
                            assetId = remoteData.assetId,
                            assetSizeInBytes = sizeInBytes,
                            downloadStatus = downloadStatus
                        )
                        // On the first asset message received, the asset ID is null, so we filter it out until the second updates it
                    }
                }
            } else null
        }
    }
    // endregion

    companion object {
        const val IMAGE_SIZE_LIMIT_BYTES = 15 * 1024 * 1024 // 15 MB limit for images
        const val ASSET_SIZE_DEFAULT_LIMIT_BYTES = 25 * 1024 * 1024 // 25 MB asset default user limit size
        const val ASSET_SIZE_TEAM_USER_LIMIT_BYTES = 100 * 1024 * 1024 // 100 MB asset team user limit size
        const val SNACKBAR_MESSAGE_DELAY = 3000L
    }
}
