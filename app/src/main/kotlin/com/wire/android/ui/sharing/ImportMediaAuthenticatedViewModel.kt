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
package com.wire.android.ui.sharing

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.model.ImageAsset
import com.wire.android.model.SnackBarMessage
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.common.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.android.ui.home.conversations.usecase.GetConversationsFromSearchUseCase
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.ui.home.conversationslist.model.ConversationFolderItem
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.android.util.EMPTY
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.parcelableArrayList
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.message.SelfDeletionTimer.Companion.SELF_DELETION_LOG_TAG
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
@OptIn(FlowPreview::class)
@Suppress("LongParameterList", "TooManyFunctions")
class ImportMediaAuthenticatedViewModel @Inject constructor(
    private val getSelf: ObserveSelfUserUseCase,
    private val getConversationsPaginated: GetConversationsFromSearchUseCase,
    private val handleUriAsset: HandleUriAssetUseCase,
    private val persistNewSelfDeletionTimerUseCase: PersistNewSelfDeletionTimerUseCase,
    private val observeSelfDeletionSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    val dispatchers: DispatcherProvider,
) : ViewModel() {
    val searchQueryTextState: TextFieldState = TextFieldState()
    private val conversationsFlow: Flow<PagingData<ConversationFolderItem>> = searchQueryTextState.textAsFlow()
        .distinctUntilChanged()
        .map { it.toString() }
        .debounce { if (it.isEmpty()) 0L else DEFAULT_SEARCH_QUERY_DEBOUNCE }
        .onStart { emit(String.EMPTY) }
        .flatMapLatest { searchQuery ->
            getConversationsPaginated(
                searchQuery = searchQuery,
                fromArchive = false,
                onlyInteractionEnabled = true,
                newActivitiesOnTop = false,
                useStrictMlsFilter = BuildConfig.USE_STRICT_MLS_FILTER
            ).map {
                it.map {
                    it as ConversationFolderItem
                }
            }
        }
        .flowOn(dispatchers.io())
    var importMediaState by mutableStateOf(ImportMediaAuthenticatedState(conversations = conversationsFlow))
        private set
    var avatarAsset by mutableStateOf<ImageAsset.UserAvatarAsset?>(null)
        private set

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    val infoMessage = _infoMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            loadUserAvatar()
        }
    }

    fun onRemove(index: Int) {
        importMediaState = importMediaState.copy(importedAssets = importMediaState.importedAssets.removeAt(index))
    }

    private fun loadUserAvatar() = viewModelScope.launch {
        getSelf().collect { selfUser ->
            avatarAsset = selfUser.previewPicture?.let {
                ImageAsset.UserAvatarAsset(it)
            }
        }
    }

    fun onConversationClicked(conversationItem: ConversationItem) {
        viewModelScope.launch {
            with(conversationItem) {
                importMediaState = importMediaState.copy(
                    selectedConversationItem = listOf(this.conversationId),
                    selfDeletingTimer = observeSelfDeletionSettingsForConversation(
                        conversationId = conversationId,
                        considerSelfUserSettings = true
                    ).first().also { timer ->
                        if (timer !is SelfDeletionTimer.Disabled) {
                            val logMap = timer.toLogString(
                                "User timer update for conversationId=${conversationId.toLogString()} on ImportMediaScreen"
                            )
                            appLogger.d("$SELF_DELETION_LOG_TAG: $logMap")
                        }
                    }
                )
            }
        }
    }

    suspend fun handleReceivedDataFromSharingIntent(activity: AppCompatActivity) {
        val incomingIntent = ShareCompat.IntentReader(activity)
        appLogger.i("Received data from sharing intent ${incomingIntent.streamCount}")
        importMediaState = importMediaState.copy(isImporting = true)
        if (incomingIntent.streamCount == 0) {
            handleSharedText(incomingIntent.text.toString())
        } else {
            if (incomingIntent.isSingleShare) {
                // ACTION_SEND
                handleSingleIntent(incomingIntent)
            } else {
                // ACTION_SEND_MULTIPLE
                handleMultipleActionIntent(activity)
            }
        }
        importMediaState = importMediaState.copy(isImporting = false)
    }

    private fun handleSharedText(text: String) {
        appLogger.d("$TAG: handleSharedText")
        importMediaState = importMediaState.copy(importedText = text)
    }

    private suspend fun handleSingleIntent(incomingIntent: ShareCompat.IntentReader) {
        incomingIntent.stream?.let { uri ->
            appLogger.d("$TAG: handleSingleIntent")
            handleImportedAsset(uri)?.let { importedAsset ->
                if (importedAsset.assetSizeExceeded != null) {
                    onSnackbarMessage(
                        SendMessagesSnackbarMessages.MaxAssetSizeExceeded(importedAsset.assetSizeExceeded)
                    )
                }
                importMediaState = importMediaState.copy(importedAssets = persistentListOf(importedAsset))
            }
        }
    }

    private suspend fun handleMultipleActionIntent(activity: AppCompatActivity) {
        appLogger.d("$TAG: handleMultipleActionIntent")
        val importedMediaAssets = activity.intent.parcelableArrayList<Parcelable>(Intent.EXTRA_STREAM)?.mapNotNull {
            val fileUri = it.toString().toUri()
            handleImportedAsset(fileUri)
        } ?: listOf()

        importMediaState = importMediaState.copy(importedAssets = importedMediaAssets.toPersistentList())

        importedMediaAssets.firstOrNull { it.assetSizeExceeded != null }?.let {
            onSnackbarMessage(SendMessagesSnackbarMessages.MaxAssetSizeExceeded(it.assetSizeExceeded!!))
        }
    }

    fun onNewSelfDeletionTimerPicked(selfDeletionDuration: SelfDeletionDuration) =
        viewModelScope.launch {
            importMediaState.selectedConversationItem.first().let {
                importMediaState = importMediaState.copy(
                    selfDeletingTimer = SelfDeletionTimer.Enabled(selfDeletionDuration.value)
                )
                val logMap = importMediaState.selfDeletingTimer.toLogString(
                    "user timer update for conversationId=${it.toLogString()} on ImportMediaScreen"
                )
                appLogger.d("$SELF_DELETION_LOG_TAG: $logMap")
                persistNewSelfDeletionTimerUseCase(
                    conversationId = it,
                    newSelfDeletionTimer = importMediaState.selfDeletingTimer
                )
            }
        }

    private suspend fun handleImportedAsset(uri: Uri): ImportedMediaAsset? = withContext(dispatchers.io()) {
        when (val result = handleUriAsset.invoke(uri, saveToDeviceIfInvalid = false)) {
            is HandleUriAssetUseCase.Result.Failure.AssetTooLarge -> {
                appLogger.w("$TAG: Failed to import asset message: Asset too large")
                ImportedMediaAsset(result.assetBundle, result.maxLimitInMB)
            }

            HandleUriAssetUseCase.Result.Failure.Unknown -> {
                appLogger.e("$TAG: Failed to import asset message: Unknown error")
                null
            }

            is HandleUriAssetUseCase.Result.Success -> ImportedMediaAsset(result.assetBundle, null)
        }
    }

    private fun onSnackbarMessage(type: SnackBarMessage) = viewModelScope.launch {
        _infoMessage.emit(type)
    }

    companion object {
        private const val TAG = "[ImportMediaAuthenticatedViewModel]"
    }
}
