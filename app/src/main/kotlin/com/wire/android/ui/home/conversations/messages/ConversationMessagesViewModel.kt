package com.wire.android.ui.home.conversations.messages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.home.conversations.ConversationAvatar
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.DownloadedAssetDialogVisibilityState
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageDownloadStatusUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class ConversationMessagesViewModel @Inject constructor(
    qualifiedIdMapper: QualifiedIdMapper,
    override val savedStateHandle: SavedStateHandle,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val getMessageAsset: GetMessageAssetUseCase,
    private val getMessageByIdUseCase: GetMessageByIdUseCase,
    private val updateAssetMessageDownloadStatus: UpdateAssetMessageDownloadStatusUseCase,
    private val fileManager: FileManager,
    private val dispatchers: DispatcherProvider,
    private val getMessageForConversation: GetMessagesForConversationUseCase,
) : SavedStateViewModel(savedStateHandle) {

    var conversationViewState by mutableStateOf(ConversationMessagesViewState())

    val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    init {
        observeConversationDetailsAndMessages()
    }

    private fun observeConversationDetailsAndMessages() {
        viewModelScope.launch {
            observeConversationDetails(conversationId)
                .combine(
                    getMessageForConversation(conversationId).onEach(::updateMessagesList)
                ) { conversationDetailsResult, uiMessages ->
                    Pair(conversationDetailsResult, uiMessages)
                }.collect { (conversationDetailsResult, uiMessages) ->
                    if (conversationDetailsResult is ObserveConversationDetailsUseCase.Result.Success) {
                        when (val details = conversationDetailsResult.conversationDetails) {
                            // TODO: think about lastUnreadMessage being a "common" field of ConversationDetails
                            is ConversationDetails.OneOne -> {
                                extractLastUnreadMessage(details.lastUnreadMessage, uiMessages)
                            }

                            is ConversationDetails.Group -> {
                                extractLastUnreadMessage(details.lastUnreadMessage, uiMessages)
                            }

                            else -> ConversationAvatar.None
                        }
                    }
                }
        }
    }

    private fun extractLastUnreadMessage(lastUnreadMessage: Message?, uiMessages: List<UIMessage>) {
        if (lastUnreadMessage != null) {
            uiMessages.firstOrNull { it.messageHeader.messageId == lastUnreadMessage.id }?.let {
                conversationViewState = conversationViewState.copy(lastUnreadMessage = it)
            }
        } else {
            conversationViewState = conversationViewState.copy(lastUnreadMessage = null)
        }
    }

    private fun updateMessagesList(messages: List<UIMessage>) {
        conversationViewState = conversationViewState.copy(messages = messages)
    }

    // This will download the asset remotely to an internal temporary storage or fetch it from the local database if it had been previously
    // downloaded. After doing so, a dialog is shown to ask the user whether he wants to open the file or download it to external storage
    fun downloadOrFetchAssetToInternalStorage(messageId: String) = viewModelScope.launch {
        withContext(dispatchers.io()) {
            try {
                attemptDownloadOfAsset(messageId)
            } catch (e: OutOfMemoryError) {
                appLogger.e("There was an OutOfMemory error while downloading the asset")
                onSnackbarMessage(ConversationSnackbarMessages.ErrorDownloadingAsset)
                updateAssetMessageDownloadStatus(Message.DownloadStatus.FAILED, conversationId, messageId)
            }
        }
    }

    private suspend fun attemptDownloadOfAsset(messageId: String) {
        val messageDataResult = getMessageByIdUseCase(conversationId, messageId)
        if (messageDataResult !is GetMessageByIdUseCase.Result.Success) {
            appLogger.w("Failed when fetching details of message to download asset: $messageDataResult")
            return
        }
        val message = messageDataResult.message
        val messageContent = message.content

        if (messageContent !is MessageContent.Asset) {
            // This _should_ not even happen, tho. Unless UI is buggy. So... do we crash?! Better not.
            appLogger.w("Attempting to download assets of a non-asset message. Ignoring user input.")
            return
        }
        val assetContent = messageContent.value
        val downloadStatus = assetContent.downloadStatus
        val isAssetDownloadedInternally = downloadStatus == Message.DownloadStatus.SAVED_INTERNALLY ||
                downloadStatus == Message.DownloadStatus.IN_PROGRESS

        if (!isAssetDownloadedInternally)
            // TODO: Refactor. UseCase responsible for downloading should update to IN_PROGRESS status.
            updateAssetMessageDownloadStatus(Message.DownloadStatus.IN_PROGRESS, conversationId, messageId)

        val resultData = assetDataPath(conversationId, messageId)
        updateAssetMessageDownloadStatus(
            if (resultData != null) Message.DownloadStatus.SAVED_INTERNALLY else Message.DownloadStatus.FAILED,
            conversationId,
            messageId
        )

        if (resultData != null) {
            showOnAssetDownloadedDialog(assetContent.name ?: "", resultData, assetContent.sizeInBytes, messageId)
        }
    }

    fun onOpenFileWithExternalApp(assetDataPath: Path, assetExtension: String?) {
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
                    updateAssetMessageDownloadStatus(Message.DownloadStatus.SAVED_EXTERNALLY, conversationId, messageId)
                    onFileSavedToExternalStorage(assetName)
                    hideOnAssetDownloadedDialog()
                }
            }
        }
    }

    fun showOnAssetDownloadedDialog(assetName: String, assetDataPath: Path, assetSize: Long, messageId: String) {
        conversationViewState =
            conversationViewState.copy(
                downloadedAssetDialogState = DownloadedAssetDialogVisibilityState.Displayed(
                    assetName,
                    assetDataPath,
                    assetSize,
                    messageId
                )
            )
    }

    fun onSnackbarMessage(msgCode: ConversationSnackbarMessages) {
        viewModelScope.launch(dispatchers.main()) {
            conversationViewState = conversationViewState.copy(snackbarMessage = msgCode)
        }
    }

    fun hideOnAssetDownloadedDialog() {
        conversationViewState = conversationViewState.copy(downloadedAssetDialogState = DownloadedAssetDialogVisibilityState.Hidden)
    }

    private suspend fun assetDataPath(conversationId: QualifiedID, messageId: String): Path? {
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

    private fun onOpenFileError() {
        conversationViewState = conversationViewState.copy(snackbarMessage = ConversationSnackbarMessages.ErrorOpeningAssetFile)
    }

    private fun onFileSavedToExternalStorage(assetName: String?) {
        onSnackbarMessage(ConversationSnackbarMessages.OnFileDownloaded(assetName))
    }

}
