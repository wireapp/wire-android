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
import com.wire.android.ui.home.conversations.ConversationViewModel
import com.wire.android.ui.home.conversations.MediaGallerySnackbarMessages
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getCurrentParsedDateTime
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult.Success
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MediaGalleryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val navigationManager: NavigationManager,
    val wireSessionImageLoader: WireSessionImageLoader,
    val getConversationDetails: ObserveConversationDetailsUseCase,
    val dispatchers: DispatcherProvider,
    val getImageData: GetMessageAssetUseCase,
    val fileManager: FileManager
) : ViewModel() {

    var mediaGalleryViewState by mutableStateOf(MediaGalleryViewState())
        private set

    val imageAssetId: ImageAsset.PrivateAsset = savedStateHandle.get<String>(EXTRA_IMAGE_DATA)!!.parseIntoPrivateImageAsset()

    init {
        observeConversationDetails()
    }

    private fun observeConversationDetails() {
        viewModelScope.launch {
            getConversationDetails(imageAssetId.conversationId).collect {
                updateMediaGalleryTitle(getScreenTitle(it))
            }
        }
    }

    private fun getScreenTitle(conversationDetails: ConversationDetails): String? {
        return when (conversationDetails) {
            is ConversationDetails.OneOne -> conversationDetails.otherUser.name
            is ConversationDetails.Group -> conversationDetails.conversation.name
            else -> null
        }
    }

    private fun updateMediaGalleryTitle(conversationName: String?) {
        mediaGalleryViewState = mediaGalleryViewState.copy(screenTitle = conversationName)
    }

    fun saveImageToExternalStorage() {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                val imageData = getImageData(imageAssetId.conversationId, imageAssetId.messageId)
                if (imageData is Success) {
                    val defaultImageName = "Wire downloaded image ${getCurrentParsedDateTime()}.jpeg"
                    fileManager.saveToExternalStorage(defaultImageName, imageData.decodedAsset) {
                        onFileSavedToExternalStorage()
                    }
                }
            }
        }
    }

    private fun onSnackbarMessage(messageCode: MediaGallerySnackbarMessages) {
        viewModelScope.launch {
            // We need to reset the onSnackbarMessage state so that it doesn't show up again when going -> background -> resume back
            // The delay added, is to ensure the snackbar message will have enough time to be shown before it is reset to null
            mediaGalleryViewState = mediaGalleryViewState.copy(onSnackbarMessage = messageCode)
            delay(ConversationViewModel.SNACKBAR_MESSAGE_DELAY)
            mediaGalleryViewState = mediaGalleryViewState.copy(onSnackbarMessage = null)
        }
    }

    private fun onFileSavedToExternalStorage() {
        onSnackbarMessage(MediaGallerySnackbarMessages.OnImageDownloaded())
    }

    fun navigateBack() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

    fun deleteCurrentImageMessage() {

    }
}
