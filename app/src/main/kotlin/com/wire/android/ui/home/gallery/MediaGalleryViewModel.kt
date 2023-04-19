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

package com.wire.android.ui.home.gallery

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.model.ImageAsset
import com.wire.android.model.parseIntoPrivateImageAsset
import com.wire.android.navigation.EXTRA_IMAGE_DATA
import com.wire.android.navigation.EXTRA_ON_MESSAGE_DETAILS_CLICKED
import com.wire.android.navigation.EXTRA_ON_MESSAGE_REACTED
import com.wire.android.navigation.EXTRA_ON_MESSAGE_REPLIED
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.MediaGallerySnackbarMessages
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogHelper
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsStates
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.startFileShareIntent
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
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
import okio.Path
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class MediaGalleryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    wireSessionImageLoader: WireSessionImageLoader,
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

    fun shareAsset(context: Context) {
        viewModelScope.launch {
            assetDataPath(imageAssetId.conversationId, imageAssetId.messageId)?.run {
                context.startFileShareIntent(first, second)
            }
        }
    }

    private suspend fun assetDataPath(conversationId: QualifiedID, messageId: String): Pair<Path, String>? =
        getImageData(conversationId, messageId).await().run {
            return when (this) {
                is Success -> decodedAssetPath to assetName
                else -> null
            }
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

    fun onMessageReacted(emoji: String) = viewModelScope.launch {
        navigationManager.navigateBack(mapOf(EXTRA_ON_MESSAGE_REACTED to Pair(imageAssetId.messageId, emoji)))
    }

    fun onMessageReplied() = viewModelScope.launch {
        navigationManager.navigateBack(mapOf(EXTRA_ON_MESSAGE_REPLIED to imageAssetId.messageId))
    }

    fun onMessageDetailsClicked() = viewModelScope.launch {
        navigationManager.navigateBack(mapOf(EXTRA_ON_MESSAGE_DETAILS_CLICKED to Pair(imageAssetId.messageId, imageAssetId.isSelfAsset)))
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
                    fileManager.saveToExternalStorage(imageData.assetName, imageData.decodedAssetPath, imageData.assetSize) {
                        onImageSavedToExternalStorage(it)
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

    private fun onImageSavedToExternalStorage(fileName: String?) {
        onSnackbarMessage(MediaGallerySnackbarMessages.OnImageDownloaded(fileName))
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

    private fun updateDeleteDialogState(newValue: (DeleteMessageDialogsStates) -> DeleteMessageDialogsStates) {
        (mediaGalleryViewState.deleteMessageDialogsState as? DeleteMessageDialogsStates)?.let {
            mediaGalleryViewState = mediaGalleryViewState.copy(deleteMessageDialogsState = newValue(it))
        }
    }
}
