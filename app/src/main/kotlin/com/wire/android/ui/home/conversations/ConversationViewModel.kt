package com.wire.android.ui.home.conversations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.model.ImageAsset.PrivateAsset
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_MESSAGE_TO_DELETE_ID
import com.wire.android.navigation.EXTRA_MESSAGE_TO_DELETE_IS_SELF
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.navigation.getBackNavArg
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
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
import com.wire.android.util.FileManager
import com.wire.android.util.ImageUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.message.Message.DownloadStatus.FAILED
import com.wire.kalium.logic.data.message.Message.DownloadStatus.IN_PROGRESS
import com.wire.kalium.logic.data.message.Message.DownloadStatus.SAVED_EXTERNALLY
import com.wire.kalium.logic.data.message.Message.DownloadStatus.SAVED_INTERNALLY
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.SendAssetMessageResult
import com.wire.kalium.logic.feature.asset.SendAssetMessageUseCase
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageDownloadStatusUseCase
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase.Result.Failure
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase.Result.Success
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import com.wire.kalium.logic.functional.onFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import okio.buffer
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import com.wire.kalium.logic.data.id.QualifiedID as ConversationId

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class ConversationViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    qualifiedIdMapper: QualifiedIdMapper,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val sendAssetMessage: SendAssetMessageUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val getMessageAsset: GetMessageAssetUseCase,
    private val deleteMessage: DeleteMessageUseCase,
    private val dispatchers: DispatcherProvider,
    private val updateAssetMessageDownloadStatus: UpdateAssetMessageDownloadStatusUseCase,
    private val getSelfUserTeam: GetSelfTeamUseCase,
    private val getMessageForConversation: GetMessagesForConversationUseCase,
    private val isFileSharingEnabled: IsFileSharingEnabledUseCase,
    private val observeOngoingCalls: ObserveOngoingCallsUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val answerCall: AnswerCallUseCase,
    private val endCall: EndCallUseCase,
    private val fileManager: FileManager,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val kaliumFileSystem: KaliumFileSystem
) : SavedStateViewModel(savedStateHandle) {

    var conversationViewState by mutableStateOf(ConversationViewState())
        private set

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

    var establishedCallConversationId: ConversationId? = null

    init {
        fetchMessages()
        listenConversationDetails()
        fetchSelfUserTeam()
        setFileSharingStatus()
        listenOngoingCall()
        observeEstablishedCall()
    }

    // region ------------------------------ Init Methods -------------------------------------
    private fun fetchMessages() = viewModelScope.launch(dispatchers.io()) {
        getMessageForConversation(conversationId).collect { messages ->
            updateMessagesList(messages)
        }
    }

    private suspend fun updateMessagesList(messages: List<UIMessage>) {
        withContext(dispatchers.main()) {
            conversationViewState = conversationViewState.copy(messages = messages)
        }
    }

    private fun listenConversationDetails() = viewModelScope.launch {
        observeConversationDetails(conversationId)
            .collect { result ->
                when (result) {
                    is Failure -> handleConversationDetailsFailure(result.storageFailure)
                    is Success -> handleConversationDetails(result.conversationDetails)
                }
            }
    }

    /**
     * TODO: This right now handles only the case when a conversation details doesn't exists.
     * Later we'll have to expand the error cases to different behaviors
     */
    private suspend fun handleConversationDetailsFailure(failure: StorageFailure) {
        when (failure) {
            is StorageFailure.DataNotFound -> navigateToHome()
            is StorageFailure.Generic -> appLogger.e("An error occurred when fetching details of the conversation", failure.rootCause)
        }
    }

    private fun handleConversationDetails(conversationDetails: ConversationDetails) {
        val conversationName = when (conversationDetails) {
            is ConversationDetails.OneOne -> conversationDetails.otherUser.name.orEmpty()
            else -> conversationDetails.conversation.name.orEmpty()
        }
        val conversationAvatar = when (conversationDetails) {
            is ConversationDetails.OneOne ->
                ConversationAvatar.OneOne(
                    conversationDetails.otherUser.previewPicture?.let {
                        UserAvatarAsset(wireSessionImageLoader, it)
                    },
                    conversationDetails.otherUser.availabilityStatus
                )
            is ConversationDetails.Group -> ConversationAvatar.Group(conversationDetails.conversation.id)
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

    private fun fetchSelfUserTeam() = viewModelScope.launch {
        getSelfUserTeam().collect {
            conversationViewState = conversationViewState.copy(userTeam = it)
        }
    }

    private fun listenOngoingCall() = viewModelScope.launch {
        observeOngoingCalls()
            .collect {
                val hasOngoingCall = it.any { call -> call.conversationId == conversationId }

                conversationViewState = conversationViewState.copy(hasOngoingCall = hasOngoingCall)
            }
    }

    private fun observeEstablishedCall() = viewModelScope.launch {
        observeEstablishedCalls().collect {
            val hasEstablishedCall = it.isNotEmpty()
            establishedCallConversationId = if (it.isNotEmpty()) {
                it.first().conversationId
            } else null
            conversationViewState = conversationViewState.copy(hasEstablishedCall = hasEstablishedCall)
        }
    }

    internal fun checkPendingActions() {
        // Check if there are messages to delete
        val messageToDeleteId = savedStateHandle
            .getBackNavArg<String>(EXTRA_MESSAGE_TO_DELETE_ID)
        val messageToDeleteIsSelf = savedStateHandle
            .getBackNavArg<Boolean>(EXTRA_MESSAGE_TO_DELETE_IS_SELF)

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
                            if (dataSize > IMAGE_SIZE_LIMIT_BYTES) onSnackbarMessage(ErrorMaxImageSize)
                            else {
                                val (imgWidth, imgHeight) = ImageUtil.extractImageWidthAndHeight(
                                    kaliumFileSystem.source(attachmentBundle.dataPath).buffer().inputStream()
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
                                if (result is SendAssetMessageResult.Failure) {
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

                    val (isAssetDownloadedInternally, assetName, assetSize) = (assetMessage?.messageContent as AssetMessage).run {
                        Triple((downloadStatus == SAVED_INTERNALLY || downloadStatus == IN_PROGRESS), assetName, assetSizeInBytes)
                    }

                    if (!isAssetDownloadedInternally)
                        updateAssetMessageDownloadStatus(IN_PROGRESS, conversationId, messageId)

                    val resultData = assetDataPath(conversationId, messageId)
                    updateAssetMessageDownloadStatus(if (resultData != null) SAVED_INTERNALLY else FAILED, conversationId, messageId)

                    if (resultData != null) {
                        showOnAssetDownloadedDialog(assetName, resultData, assetSize, messageId)
                    }
                } catch (e: OutOfMemoryError) {
                    appLogger.e("There was an OutOfMemory error while downloading the asset")
                    onSnackbarMessage(ConversationSnackbarMessages.ErrorDownloadingAsset)
                    updateAssetMessageDownloadStatus(FAILED, conversationId, messageId)
                }
            }
        }
    }

    fun showOnAssetDownloadedDialog(assetName: String, assetDataPath: Path, assetSize: Long, messageId: String) {
        conversationViewState =
            conversationViewState.copy(downloadedAssetDialogState = Displayed(assetName, assetDataPath, assetSize, messageId))
    }

    fun hideOnAssetDownloadedDialog() {
        conversationViewState = conversationViewState.copy(downloadedAssetDialogState = Hidden)
    }

    private fun setFileSharingStatus() {
        viewModelScope.launch {
            if (isFileSharingEnabled().isFileSharingEnabled != null) {
                conversationViewState = conversationViewState.copy(isFileSharingEnabled = isFileSharingEnabled().isFileSharingEnabled!!)
            }
        }
    }

    private fun getAssetLimitInBytes(): Int {
        // Users with a team attached have larger asset sending limits than default users
        return conversationViewState.userTeam?.run {
            ASSET_SIZE_TEAM_USER_LIMIT_BYTES
        } ?: ASSET_SIZE_DEFAULT_LIMIT_BYTES
    }

    fun onSnackbarMessage(msgCode: ConversationSnackbarMessages) {
        viewModelScope.launch(dispatchers.main()) {
            conversationViewState = conversationViewState.copy(onSnackbarMessage = msgCode)
        }
    }

    fun clearSnackbarMessage() {
        viewModelScope.launch(dispatchers.main()) {
            conversationViewState = conversationViewState.copy(onSnackbarMessage = null)
        }
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

    private suspend fun assetDataPath(conversationId: ConversationId, messageId: String): Path? {
        getMessageAsset(
            conversationId = conversationId,
            messageId = messageId
        ).run {
            return when (this) {
                is MessageAssetResult.Success -> decodedAssetPath
                else -> null
            }
        }
    }

    fun onOpenFileWithExternalApp(assetDataPath: Path, assetExtension: String) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                fileManager.openWithExternalApp(assetDataPath, assetExtension) { onOpenFileError() }
                hideOnAssetDownloadedDialog()
            }
        }
    }

    fun onSaveFile(assetName: String, assetDataPath: Path, assetSize: Long, messageId: String) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                fileManager.saveToExternalStorage(assetName, assetDataPath, assetSize) {
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
            establishedCallConversationId?.let {
                endCall(it)
            }
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
                    destination = NavigationItem.Gallery.getRouteWithArgs(
                        listOf(
                            PrivateAsset(wireSessionImageLoader, conversationId, messageId, isSelfMessage)
                        )
                    )
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
                            listOf(data.otherUserId)
                        )
                    )
                )
                is ConversationDetailsData.Group -> navigationManager.navigate(
                    command = NavigationCommand(
                        destination = NavigationItem.GroupConversationDetails.getRouteWithArgs(listOf(data.conversationId))
                    )
                )
                ConversationDetailsData.None -> { /* do nothing */
                }
            }
        }
    }

    fun navigateToProfile(messageSource: MessageSource, userId: UserId) {
        viewModelScope.launch {
            when (messageSource) {
                MessageSource.Self -> navigateToSelfProfile()
                MessageSource.OtherUser -> when (conversationViewState.conversationDetailsData) {
                    is ConversationDetailsData.Group -> navigateToOtherProfile(userId, conversationId)
                    else -> navigateToOtherProfile(userId)
                }
            }
        }
    }

    private suspend fun navigateToHome() =
        navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.UPDATE_EXISTED))

    private suspend fun navigateToSelfProfile() =
        navigationManager.navigate(NavigationCommand(NavigationItem.SelfUserProfile.getRouteWithArgs()))

    private suspend fun navigateToOtherProfile(id: UserId, conversationId: ConversationId? = null) =
        navigationManager.navigate(NavigationCommand(NavigationItem.OtherUserProfile.getRouteWithArgs(listOfNotNull(id, conversationId))))

    fun provideTempCachePath(): Path = kaliumFileSystem.rootCachePath

    companion object {
        const val IMAGE_SIZE_LIMIT_BYTES = 15 * 1024 * 1024 // 15 MB limit for images
        const val ASSET_SIZE_DEFAULT_LIMIT_BYTES = 25 * 1024 * 1024 // 25 MB asset default user limit size
        const val ASSET_SIZE_TEAM_USER_LIMIT_BYTES = 100 * 1024 * 1024 // 100 MB asset team user limit size
        val SNACKBAR_MESSAGE_DELAY = 3.seconds
    }
}
