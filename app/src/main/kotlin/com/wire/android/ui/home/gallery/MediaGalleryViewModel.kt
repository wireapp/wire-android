package com.wire.android.ui.home.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.EXTRA_IMAGE_DATA
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.parseIntoPrivateImageAsset
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaGalleryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val navigationManager: NavigationManager,
    val wireSessionImageLoader: WireSessionImageLoader,
    val getConversationDetails: ObserveConversationDetailsUseCase
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
                updateMediaGalleryTitle(it.conversation.name)
            }
        }
    }

    private fun updateMediaGalleryTitle(conversationName: String?) {
        mediaGalleryViewState = mediaGalleryViewState.copy(screenTitle = conversationName)
    }

    fun navigateBack() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }
}
