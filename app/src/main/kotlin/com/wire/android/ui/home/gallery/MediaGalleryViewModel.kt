package com.wire.android.ui.home.gallery

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.AssistedViewModel
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.NavQualifiedId
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.ConversationViewModel
import com.wire.android.ui.home.conversations.MediaGallerySnackbarMessages
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getCurrentParsedDateTime
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult.Success
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class MediaGalleryViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val navigationManager: NavigationManager,
    private val getConversationDetails: ObserveConversationDetailsUseCase,
    private val dispatchers: DispatcherProvider,
    private val getImageData: GetMessageAssetUseCase,
    private val fileManager: FileManager,
) : ViewModel(), AssistedViewModel<MediaGalleryViewModel.Params> {

    var mediaGalleryViewState by mutableStateOf(MediaGalleryViewState())
        private set

    val conversationId: ConversationId = param.conversationId.qualifiedId
    val imageAssetId: ImageAsset.PrivateAsset
        get() = ImageAsset.PrivateAsset(wireSessionImageLoader, conversationId, param.messageId, param.isSelfAsset)

    init {
        observeConversationDetails()
    }

    private fun observeConversationDetails() {
        viewModelScope.launch {
            getConversationDetails(conversationId).collect {
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
                val imageData = getImageData(conversationId, param.messageId)
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
            // We need to reset the onSnackbarMessage state so that it doesn't show up again when going -> background -> resume back
            // The delay added, is to ensure the snackbar message will have enough time to be shown before it is reset to null
            mediaGalleryViewState = mediaGalleryViewState.copy(onSnackbarMessage = messageCode)
            delay(ConversationViewModel.SNACKBAR_MESSAGE_DELAY)
            mediaGalleryViewState = mediaGalleryViewState.copy(onSnackbarMessage = null)
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

    fun deleteCurrentImageMessage() {
        viewModelScope.launch {
            param.onResult(param.messageId, param.isSelfAsset)
            navigationManager.navigateBack()
        }
    }

    @Parcelize
    data class Params(
        val conversationId: NavQualifiedId,
        val messageId: String,
        val isSelfAsset: Boolean,
        val onResult: (String, Boolean) -> Unit
    ): Parcelable
}
