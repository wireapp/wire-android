/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.home.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.model.ImageAsset
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.home.conversations.MediaGallerySnackbarMessages
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogState
import com.wire.android.ui.navArgs
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.cells.domain.usecase.GetCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetMessageAttachmentUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.CellAssetContent
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult.Success
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
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
    private val getConversationDetails: ObserveConversationDetailsUseCase,
    private val dispatchers: DispatcherProvider,
    private val getImageData: GetMessageAssetUseCase,
    private val fileManager: FileManager,
    private val deleteMessage: DeleteMessageUseCase,
    private val getAttachment: GetMessageAttachmentUseCase,
    private val getCellNode: GetCellFileUseCase,
) : ActionsViewModel<MediaGalleryAction>() {

    private val mediaGalleryNavArgs: MediaGalleryNavArgs = savedStateHandle.navArgs()

    private val messageId = mediaGalleryNavArgs.messageId
    private val conversationId = mediaGalleryNavArgs.conversationId
    private val cellAssetId = mediaGalleryNavArgs.cellAssetId

    var mediaGalleryViewState by mutableStateOf(MediaGalleryViewState())
        private set

    var deleteMessageDialogState: VisibilityState<DeleteMessageDialogState> by mutableStateOf(VisibilityState())
        private set

    private val _snackbarMessage = MutableSharedFlow<MediaGallerySnackbarMessages>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    init {
        getConversationTitle()
        setupImageAsset()
    }

    private fun setupImageAsset() = viewModelScope.launch {
        if (cellAssetId == null) {
            mediaGalleryViewState = mediaGalleryViewState.copy(
                imageAsset = MediaGalleryImage.PrivateAsset(
                    asset = ImageAsset.PrivateAsset(
                        mediaGalleryNavArgs.conversationId,
                        mediaGalleryNavArgs.messageId,
                        mediaGalleryNavArgs.isSelfAsset,
                        mediaGalleryNavArgs.isEphemeral
                    )
                )
            )
        } else {
            getAttachment(cellAssetId).onSuccess { attachment ->
                (attachment as? CellAssetContent)?.let { cellAsset ->
                    val localPath = cellAsset.localPath
                    val url = cellAsset.contentUrl ?: cellAsset.previewUrl

                    if (localPath != null) {
                        mediaGalleryViewState = mediaGalleryViewState.copy(
                            imageAsset = MediaGalleryImage.LocalAsset(localPath)
                        )
                    } else if (url != null) {
                        mediaGalleryViewState = mediaGalleryViewState.copy(
                            imageAsset = MediaGalleryImage.UrlAsset(
                                url = url,
                                placeholder = cellAsset.previewUrl,
                                contentHash = cellAsset.contentHash,
                            )
                        )
                    }
                }
            }
        }
    }

    private fun shareAsset() = viewModelScope.launch {
        if (cellAssetId == null) {
            assetDataPath(conversationId, messageId)?.run {
                sendAction(MediaGalleryAction.Share(first, second))
            }
        } else {
            getCellNode(cellAssetId)
                .onSuccess { node ->
                    sendAction(
                        MediaGalleryAction.SharePublicLink(
                            assetId = node.uuid,
                            assetName = node.name ?: cellAssetId,
                            publicLinkId = node.publicLinkId
                        )
                    )
                }
                .onFailure {
                    sendAction(MediaGalleryAction.ShowError)
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

    private fun getConversationTitle() {
        viewModelScope.launch {
            getConversationDetails(conversationId)
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
                val imageData = getImageData(conversationId, messageId).await()
                if (imageData is Success) {
                    fileManager.saveToExternalStorage(
                        imageData.assetName,
                        imageData.decodedAssetPath,
                        imageData.assetSize
                    ) {
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

    private fun onSaveError() {
        onSnackbarMessage(MediaGallerySnackbarMessages.OnImageDownloadError)
    }

    fun onMenuIntent(menuIntent: MenuIntent) {
        when (menuIntent) {
            is MenuIntent.React -> sendAction(
                MediaGalleryAction.React(
                    messageId = messageId,
                    emoji = menuIntent.emoji
                )
            )

            MenuIntent.ShowDetails -> sendAction(
                MediaGalleryAction.ShowDetails(
                    messageId = messageId,
                    isSelfAsset = mediaGalleryNavArgs.isSelfAsset
                )
            )

            MenuIntent.Reply -> sendAction(
                MediaGalleryAction.Reply(
                    messageId = messageId
                )
            )

            MenuIntent.Download -> sendAction(MediaGalleryAction.Download)

            MenuIntent.Share -> shareAsset()

            MenuIntent.Delete -> {
                deleteMessageDialogState.show(
                    DeleteMessageDialogState(mediaGalleryNavArgs.isSelfAsset, messageId, conversationId)
                )
            }
        }
    }

    fun deleteMessage(messageId: String, deleteForEveryone: Boolean) {
        viewModelScope.launch {
            deleteMessageDialogState.update { it.copy(loading = true) }
            deleteMessage(conversationId = conversationId, messageId = messageId, deleteForEveryone = deleteForEveryone)
                .onFailure {
                    onSnackbarMessage(MediaGallerySnackbarMessages.DeletingMessageError)
                }.onSuccess {
                    sendAction(MediaGalleryAction.Close)
                }
            deleteMessageDialogState.dismiss()
        }
    }

    private fun buildMenuOptions() = buildList {
        if (mediaGalleryNavArgs.messageOptionsEnabled) {
            when {
                cellAssetId != null -> {
                    add(MediaGalleryMenuItem.REACT)
                    add(MediaGalleryMenuItem.SHOW_DETAILS)
                    add(MediaGalleryMenuItem.REPLY)
                    add(MediaGalleryMenuItem.SHARE_PUBLIC_LINK)
                }

                mediaGalleryNavArgs.isEphemeral -> {
                    add(MediaGalleryMenuItem.SHOW_DETAILS)
                    add(MediaGalleryMenuItem.DOWNLOAD)
                    add(MediaGalleryMenuItem.DELETE)
                }

                else -> {
                    add(MediaGalleryMenuItem.REACT)
                    add(MediaGalleryMenuItem.SHOW_DETAILS)
                    add(MediaGalleryMenuItem.REPLY)
                    add(MediaGalleryMenuItem.DOWNLOAD)
                    add(MediaGalleryMenuItem.SHARE)
                    add(MediaGalleryMenuItem.DELETE)
                }
            }
        } else if (cellAssetId == null) {
            add(MediaGalleryMenuItem.DOWNLOAD)
            if (!mediaGalleryNavArgs.isEphemeral) add(MediaGalleryMenuItem.SHARE)
            add(MediaGalleryMenuItem.DELETE)
        }
    }

    fun onOptionsClick() {
        mediaGalleryViewState = mediaGalleryViewState.copy(
            menuItems = buildMenuOptions()
        )
    }

    fun onOptionsDismissed() {
        mediaGalleryViewState = mediaGalleryViewState.copy(
            menuItems = emptyList()
        )
    }
}

sealed interface MediaGalleryAction {
    data class ShowDetails(val messageId: String, val isSelfAsset: Boolean) : MediaGalleryAction
    data class Share(val path: Path, val assetName: String) : MediaGalleryAction
    data class React(val messageId: String, val emoji: String) : MediaGalleryAction
    data class Reply(val messageId: String) : MediaGalleryAction
    data object Download : MediaGalleryAction
    data class SharePublicLink(val assetId: String, val assetName: String, val publicLinkId: String?) : MediaGalleryAction
    data object ShowError : MediaGalleryAction
    data object Close : MediaGalleryAction
}

sealed interface MenuIntent {
    data class React(val emoji: String) : MenuIntent
    data object ShowDetails : MenuIntent
    data object Reply : MenuIntent
    data object Download : MenuIntent
    data object Share : MenuIntent
    data object Delete : MenuIntent
}

enum class MediaGalleryMenuItem {
    REACT,
    SHOW_DETAILS,
    REPLY,
    DOWNLOAD,
    SHARE,
    SHARE_PUBLIC_LINK,
    DELETE
}
