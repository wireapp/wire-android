package com.wire.android.ui.home.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.model.ImageAsset
import com.wire.android.model.parseIntoPrivateImageAsset
import com.wire.android.navigation.EXTRA_IMAGE_DATA
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.MediaGallerySnackbarMessages
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogHelper
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsState
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getCurrentParsedDateTime
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult.Success
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.functional.onFailure
import com.wire.kalium.logic.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class MediaGalleryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wireSessionImageLoader: WireSessionImageLoader,
    qualifiedIdMapper: QualifiedIdMapper,
    private val navigationManager: NavigationManager,
    private val getConversationDetails: ObserveConversationDetailsUseCase,
    private val dispatchers: DispatcherProvider,
    private val getImageData: GetMessageAssetUseCase,
    private val fileManager: FileManager,
    private val deleteMessage: DeleteMessageUseCase,
) : ViewModel() {

    var mediaGalleryViewState by mutableStateOf(MediaGalleryViewState())
        private set

    private val _snackbarMessage = MutableSharedFlow<MediaGallerySnackbarMessages>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    val imageAssetId: ImageAsset.PrivateAsset =
        savedStateHandle.get<String>(EXTRA_IMAGE_DATA)!!.parseIntoPrivateImageAsset(
            wireSessionImageLoader,
            qualifiedIdMapper
        )

    val deleteMessageHelper = DeleteMessageDialogHelper(
        viewModelScope,
        imageAssetId.conversationId,
        ::updateDeleteDialogState
    ) { messageId, deleteForEveryone ->
        deleteMessage(conversationId = imageAssetId.conversationId, messageId = messageId, deleteForEveryone = deleteForEveryone)
            .onFailure { onSnackbarMessage(MediaGallerySnackbarMessages.DeletingMessageError) }
            .onSuccess { navigateBack() }
    }

    init {
        observeConversationDetails()
    }

    private fun observeConversationDetails() {
        viewModelScope.launch {
            getConversationDetails(imageAssetId.conversationId)
                .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>() // TODO handle StorageFailure
                .map { it.conversationDetails }
                .collect {
                    updateMediaGalleryTitle(getScreenTitle(it))
                }
        }
    }

    private fun getScreenTitle(conversationDetails: ConversationDetails): String? =
        when (conversationDetails) {
            is ConversationDetails.OneOne -> conversationDetails.otherUser.name
            is ConversationDetails.Group -> conversationDetails.conversation.name
            else -> null
        }

    private fun updateMediaGalleryTitle(conversationName: String?) {
        mediaGalleryViewState = mediaGalleryViewState.copy(screenTitle = conversationName)
    }

    fun saveImageToExternalStorage() {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                val imageData = getImageData(imageAssetId.conversationId, imageAssetId.messageId).await()
                if (imageData is Success) {
                    val defaultImageName = "Wire downloaded image ${getCurrentParsedDateTime()}.jpeg"
                    fileManager.saveToExternalStorage(defaultImageName, imageData.decodedAssetPath, imageData.assetSize) {
                        onImageSavedToExternalStorage()
                    }
                } else {
                    onSaveError()
                }
            }
        }
    }

    private fun onSnackbarMessage(messageCode: MediaGallerySnackbarMessages) {
        viewModelScope.launch {
            _snackbarMessage.emit(messageCode)
        }
    }

    private fun onImageSavedToExternalStorage() {
        onSnackbarMessage(MediaGallerySnackbarMessages.OnImageDownloaded())
    }

    internal fun onSaveError() {
        onSnackbarMessage(MediaGallerySnackbarMessages.OnImageDownloadError)
    }

    fun navigateBack() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

    fun deleteCurrentImage() {
        if (imageAssetId.isSelfAsset) {
            updateDeleteDialogState {
                it.copy(
                    forEveryone = DeleteMessageDialogActiveState.Visible(
                        messageId = imageAssetId.messageId,
                        conversationId = imageAssetId.conversationId
                    )
                )
            }
        } else {
            updateDeleteDialogState {
                it.copy(
                    forYourself = DeleteMessageDialogActiveState.Visible(
                        messageId = imageAssetId.messageId,
                        conversationId = imageAssetId.conversationId
                    )
                )
            }
        }
    }

    private fun updateDeleteDialogState(newValue: (DeleteMessageDialogsState.States) -> DeleteMessageDialogsState) {
        (mediaGalleryViewState.deleteMessageDialogsState as? DeleteMessageDialogsState.States)?.let {
            mediaGalleryViewState = mediaGalleryViewState.copy(deleteMessageDialogsState = newValue(it))
        }
    }
}
