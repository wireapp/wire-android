package com.wire.android.ui.home.conversations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.model.ImageAsset.PrivateAsset
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_MESSAGE_TO_DELETE_ID
import com.wire.android.navigation.EXTRA_MESSAGE_TO_DELETE_IS_SELF
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorDeletingMessage
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorMaxAssetSize
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorMaxImageSize
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorOpeningAssetFile
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.OnFileDownloaded
import com.wire.android.ui.home.conversations.DownloadedAssetDialogVisibilityState.Displayed
import com.wire.android.ui.home.conversations.DownloadedAssetDialogVisibilityState.Hidden
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.ui.home.conversations.model.MessageContent.AssetMessage
import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
import com.wire.android.util.FileManager
import com.wire.android.util.ImageUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.data.message.Message.DownloadStatus.FAILED
import com.wire.kalium.logic.data.message.Message.DownloadStatus.IN_PROGRESS
import com.wire.kalium.logic.data.message.Message.DownloadStatus.SAVED_EXTERNALLY
import com.wire.kalium.logic.data.message.Message.DownloadStatus.SAVED_INTERNALLY
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.SendAssetMessageResult
import com.wire.kalium.logic.feature.asset.SendAssetMessageUseCase
import com.wire.kalium.logic.feature.asset.SendImageMessageResult
import com.wire.kalium.logic.feature.asset.SendImageMessageUseCase
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageDownloadStatusUseCase
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.MarkMessagesAsNotifiedUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.functional.onFailure
import com.wire.kalium.logic.util.toStringDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import com.wire.kalium.logic.data.id.QualifiedID as ConversationId

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class ConversationViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val sendImageMessage: SendImageMessageUseCase,
    private val sendAssetMessage: SendAssetMessageUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val getMessageAsset: GetMessageAssetUseCase,
    private val deleteMessage: DeleteMessageUseCase,
    private val dispatchers: DispatcherProvider,
    private val markMessagesAsNotified: MarkMessagesAsNotifiedUseCase,
    private val updateAssetMessageDownloadStatus: UpdateAssetMessageDownloadStatusUseCase,
    private val getSelfUserTeam: GetSelfTeamUseCase,
    private val getMessageForConversation: GetMessagesForConversationUseCase,
    private val observeOngoingCalls: ObserveOngoingCallsUseCase,
    private val answerCall: AnswerCallUseCase,
    private val fileManager: FileManager
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

    init {
        fetchMessages()
        listenConversationDetails()
        fetchSelfUserTeam()
        setMessagesAsNotified()
        listenOngoingCall()
    }

    // region ------------------------------ Init Methods -------------------------------------
    private fun fetchMessages() = viewModelScope.launch {
        getMessageForConversation(conversationId).collect { messages ->
            conversationViewState = conversationViewState.copy(messages = messages)
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
                    ConversationAvatar.Group(conversationDetails.conversation.id)
                else -> ConversationAvatar.None
            }
            val conversationDetailsData = when (conversationDetails) {
                is ConversationDetails.Group -> ConversationDetailsData.Group(conversationDetails.conversation.id)
                is ConversationDetails.OneOne -> ConversationDetailsData.OneOne(conversationDetails.otherUser.id)
                else -> ConversationDetailsData.None
            }
            conversationViewState = conversationViewState.copy(
                conversationName = conversationName,
                conversationAvatar = conversationAvatar,
                conversationDetailsData = conversationDetailsData
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

    private fun listenOngoingCall() = viewModelScope.launch {
        observeOngoingCalls()
            .collect {
                val hasOngoingCall = it.any { call -> call.conversationId == conversationId }

                conversationViewState = conversationViewState.copy(hasOngoingCall = hasOngoingCall)
            }
    }

    internal fun checkPendingActions() {
        // Check if there are messages to delete
        val messageToDeleteId = savedStateHandle
            .get<String>(EXTRA_MESSAGE_TO_DELETE_ID)
        val messageToDeleteIsSelf = savedStateHandle
            .get<Boolean>(EXTRA_MESSAGE_TO_DELETE_IS_SELF)

        if (messageToDeleteId != null && messageToDeleteIsSelf != null) {
            showDeleteMessageDialog(messageToDeleteId, messageToDeleteIsSelf)
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
                                val (imgWidth, imgHeight) = ImageUtil.extractImageWidthAndHeight(attachmentBundle.rawContent)
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

    fun joinOngoingCall() {
        viewModelScope.launch {
            answerCall(conversationId = conversationId)
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(conversationId))
                )
            )
        }
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
        // update dialogs state to loading
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
            .onFailure { onDeleteMessageError() }
        onDeleteDialogDismissed()
    }

    private fun onDeleteMessageError() {
        onSnackbarMessage(ErrorDeletingMessage)
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

    fun navigateToGallery(messageId: String, isSelfMessage: Boolean) {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.Gallery.getRouteWithArgs(listOf(PrivateAsset(conversationId, messageId, isSelfMessage)))
                )
            )
        }
    }

    fun navigateToDetails() {
        viewModelScope.launch {
            when (val data = conversationViewState.conversationDetailsData) {
                is ConversationDetailsData.OneOne -> navigationManager.navigate(
                    command = NavigationCommand(
                        destination = NavigationItem.OtherUserProfile.getRouteWithArgs(
                            listOf(data.otherUserId.domain, data.otherUserId.value)
                        )
                    )
                )
                is ConversationDetailsData.Group -> navigationManager.navigate(
                    command = NavigationCommand(
                        destination = NavigationItem.GroupConversationDetails.getRouteWithArgs(listOf(data.covnersationId))
                    )
                )
                ConversationDetailsData.None -> { /* do nothing */
                }
            }
        }
    }

    companion object {
        const val IMAGE_SIZE_LIMIT_BYTES = 15 * 1024 * 1024 // 15 MB limit for images
        const val ASSET_SIZE_DEFAULT_LIMIT_BYTES = 25 * 1024 * 1024 // 25 MB asset default user limit size
        const val ASSET_SIZE_TEAM_USER_LIMIT_BYTES = 100 * 1024 * 1024 // 100 MB asset team user limit size
        val SNACKBAR_MESSAGE_DELAY = 3.seconds
    }
}
