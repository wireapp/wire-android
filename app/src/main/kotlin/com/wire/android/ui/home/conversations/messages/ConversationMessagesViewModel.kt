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

package com.wire.android.ui.home.conversations.messages

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.model.SnackBarMessage
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.OnResetSession
import com.wire.android.ui.home.conversations.DownloadedAssetDialogVisibilityState
import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.startFileShareIntent
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageDownloadStatusUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import com.wire.kalium.logic.feature.message.ToggleReactionUseCase
import com.wire.kalium.logic.feature.sessionreset.ResetSessionResult
import com.wire.kalium.logic.feature.sessionreset.ResetSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import okio.Path
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class ConversationMessagesViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    qualifiedIdMapper: QualifiedIdMapper,
    override val savedStateHandle: SavedStateHandle,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val getMessageAsset: GetMessageAssetUseCase,
    private val getMessageByIdUseCase: GetMessageByIdUseCase,
    private val updateAssetMessageDownloadStatus: UpdateAssetMessageDownloadStatusUseCase,
    private val fileManager: FileManager,
    private val dispatchers: DispatcherProvider,
    private val getMessageForConversation: GetMessagesForConversationUseCase,
    private val toggleReaction: ToggleReactionUseCase,
    private val resetSession: ResetSessionUseCase,
    private val conversationAudioMessagePlayer: ConversationAudioMessagePlayer
) : SavedStateViewModel(savedStateHandle) {

    var conversationViewState by mutableStateOf(ConversationMessagesViewState())

    private val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    val infoMessage = _infoMessage.asSharedFlow()

    init {
        loadPaginatedMessages()
        loadLastMessageInstant()
        observeAudioPlayerState()
    }

    private fun observeAudioPlayerState() {
        viewModelScope.launch {
            conversationAudioMessagePlayer.observableAudioMessagesState.collect {
                conversationViewState = conversationViewState.copy(
                    audioMessagesState = it
                )
            }
        }
    }

    private fun loadPaginatedMessages() = viewModelScope.launch {
        val paginatedMessagesFlow = getMessageForConversation(conversationId)
            .flowOn(dispatchers.io())
            .cachedIn(this)

        conversationViewState = conversationViewState.copy(messages = paginatedMessagesFlow)
    }

    private fun loadLastMessageInstant() = viewModelScope.launch {
        observeConversationDetails(conversationId)
            .flowOn(dispatchers.io())
            .collect { conversationDetailsResult ->
                if (conversationDetailsResult is ObserveConversationDetailsUseCase.Result.Success) {
                    val lastUnreadInstant = conversationDetailsResult.conversationDetails.conversation.lastReadDate.let {
                        Instant.parse(it)
                    }
                    conversationViewState = conversationViewState.copy(firstUnreadInstant = lastUnreadInstant)
                }
            }
    }

    private fun onSnackbarMessage(type: SnackBarMessage) = viewModelScope.launch {
        _infoMessage.emit(type)
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
                updateAssetMessageDownloadStatus(Message.DownloadStatus.FAILED_DOWNLOAD, conversationId, messageId)
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
        val resultData = assetDataPath(conversationId, messageId)

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
                fileManager.saveToExternalStorage(assetName, assetDataPath, assetSize) { savedFileName: String? ->
                    updateAssetMessageDownloadStatus(Message.DownloadStatus.SAVED_EXTERNALLY, conversationId, messageId)
                    onFileSavedToExternalStorage(savedFileName)
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

    fun hideOnAssetDownloadedDialog() {
        conversationViewState = conversationViewState.copy(downloadedAssetDialogState = DownloadedAssetDialogVisibilityState.Hidden)
    }

    fun toggleReaction(messageId: String, reactionEmoji: String) {
        viewModelScope.launch {
            toggleReaction(conversationId, messageId, reactionEmoji)
        }
    }

    fun openMessageDetails(messageId: String, isSelfMessage: Boolean) {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.MessageDetails.getRouteWithArgs(
                        listOf(conversationId, messageId, isSelfMessage)
                    )
                )
            )
        }
    }

    fun onResetSession(userId: UserId, clientId: String?) {
        viewModelScope.launch {
            when (resetSession(conversationId, userId, ClientId(clientId.orEmpty()))) {
                is ResetSessionResult.Failure -> {
                    onSnackbarMessage(OnResetSession(UIText.StringResource(R.string.label_general_error)))
                }

                is ResetSessionResult.Success -> {
                    onSnackbarMessage(OnResetSession(UIText.StringResource(R.string.label_reset_session_success)))
                }
            }
        }
    }

    fun shareAsset(context: Context, messageId: String) {
        viewModelScope.launch {
            context.startFileShareIntent(assetDataPath(conversationId, messageId).toString())
        }
    }

    // region Private
    private suspend fun assetDataPath(conversationId: QualifiedID, messageId: String): Path? =
        getMessageAsset(conversationId, messageId).await().run {
            return when (this) {
                is MessageAssetResult.Success -> decodedAssetPath
                else -> null
            }
        }

    private fun onOpenFileError() {
        onSnackbarMessage(ConversationSnackbarMessages.ErrorOpeningAssetFile)
    }

    private fun onFileSavedToExternalStorage(assetName: String?) {
        onSnackbarMessage(ConversationSnackbarMessages.OnFileDownloaded(assetName))
    }

    fun audioClick(messageId: String) {
        viewModelScope.launch {
            conversationAudioMessagePlayer.playAudio(conversationId, messageId)
        }
    }

    fun changeAudioPosition(messageId: String, position: Int) {
        viewModelScope.launch {
            conversationAudioMessagePlayer.setPosition(messageId, position)
        }
    }

    override fun onCleared() {
        super.onCleared()
        conversationAudioMessagePlayer.close()
    }

}
