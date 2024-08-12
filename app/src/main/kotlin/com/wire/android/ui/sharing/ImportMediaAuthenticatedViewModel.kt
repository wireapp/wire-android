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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.mapper.toUIPreview
import com.wire.android.model.ImageAsset
import com.wire.android.model.SnackBarMessage
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.search.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.ui.home.conversationslist.model.BlockState
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.parseConversationEventType
import com.wire.android.ui.home.conversationslist.parsePrivateConversationEventType
import com.wire.android.ui.home.conversationslist.showLegalHoldIndicator
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.android.util.EMPTY
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.parcelableArrayList
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.message.SelfDeletionTimer.Companion.SELF_DELETION_LOG_TAG
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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
    private val getSelf: GetSelfUserUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val observeConversationListDetails: ObserveConversationListDetailsUseCase,
    private val handleUriAsset: HandleUriAssetUseCase,
    private val persistNewSelfDeletionTimerUseCase: PersistNewSelfDeletionTimerUseCase,
    private val observeSelfDeletionSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    val dispatchers: DispatcherProvider,
) : ViewModel() {
    val searchQueryTextState: TextFieldState = TextFieldState()
    var importMediaState by mutableStateOf(ImportMediaAuthenticatedState())
        private set

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    val infoMessage = _infoMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            loadUserAvatar()
            observeConversationWithSearch()
        }
    }

    fun onRemove(index: Int) {
        importMediaState = importMediaState.copy(importedAssets = importMediaState.importedAssets.removeAt(index))
    }

    private fun loadUserAvatar() = viewModelScope.launch(dispatchers.io()) {
        getSelf().collect { selfUser ->
            withContext(dispatchers.main()) {
                importMediaState =
                    importMediaState.copy(avatarAsset = selfUser.previewPicture?.let {
                        ImageAsset.UserAvatarAsset(wireSessionImageLoader, it)
                    })
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observeConversationWithSearch() = viewModelScope.launch {
        searchQueryTextState.textAsFlow()
            .distinctUntilChanged()
            .debounce(DEFAULT_SEARCH_QUERY_DEBOUNCE)
            .onStart { emit(String.EMPTY) }
            .flatMapLatest { searchQuery ->
                observeConversationListDetails(fromArchive = false)
                    .map { conversationDetailsList ->
                        val conversations = conversationDetailsList.mapNotNull { conversationDetails ->
                            conversationDetails.toConversationItem(wireSessionImageLoader, userTypeMapper)
                        }
                        ShareableConversationListState(
                            initialConversations = conversations,
                            searchQuery = searchQuery.toString(),
                            hasNoConversations = conversations.isEmpty(),
                            searchResult = searchShareableConversation(conversations, searchQuery.toString())
                        )
                    }
            }
            .flowOn(dispatchers.io())
            .collect { updatedState ->
                importMediaState =
                    importMediaState.copy(shareableConversationListState = updatedState)
            }
    }

    private fun addConversationItemToGroupSelection(conversation: ConversationItem) =
        viewModelScope.launch {
            // TODO: change this conversation item to a list of conversation items in case we want to support
            // sharing to multiple conversations
            importMediaState =
                importMediaState.copy(selectedConversationItem = listOf(conversation))
        }

    fun onConversationClicked(conversationId: ConversationId) {
        importMediaState.shareableConversationListState.initialConversations.find { it.conversationId == conversationId }
            ?.let {
                addConversationItemToGroupSelection(it)
            }
        onNewConversationPicked(conversationId)
    }

    @Suppress("LongMethod")
    private fun ConversationDetails.toConversationItem(
        wireSessionImageLoader: WireSessionImageLoader,
        userTypeMapper: UserTypeMapper
    ): ConversationItem? = when (this) {
        is ConversationDetails.Group -> {
            ConversationItem.GroupConversation(
                groupName = conversation.name.orEmpty(),
                conversationId = conversation.id,
                mutedStatus = conversation.mutedStatus,
                isLegalHold = conversation.legalHoldStatus.showLegalHoldIndicator(),
                lastMessageContent = lastMessage.toUIPreview(unreadEventCount),
                badgeEventType = parseConversationEventType(
                    conversation.mutedStatus,
                    unreadEventCount
                ),
                hasOnGoingCall = hasOngoingCall,
                isSelfUserCreator = isSelfUserCreator,
                isSelfUserMember = isSelfUserMember,
                teamId = conversation.teamId,
                selfMemberRole = selfRole,
                isArchived = conversation.archived,
                mlsVerificationStatus = conversation.mlsVerificationStatus,
                proteusVerificationStatus = conversation.proteusVerificationStatus
            )
        }

        is ConversationDetails.OneOne -> {
            ConversationItem.PrivateConversation(
                userAvatarData = UserAvatarData(
                    otherUser.previewPicture?.let {
                        ImageAsset.UserAvatarAsset(
                            wireSessionImageLoader,
                            it
                        )
                    },
                    otherUser.availabilityStatus,
                    otherUser.connectionStatus
                ),
                conversationInfo = ConversationInfo(
                    name = otherUser.name.orEmpty(),
                    membership = userTypeMapper.toMembership(userType),
                    isSenderUnavailable = otherUser.isUnavailableUser
                ),
                conversationId = conversation.id,
                mutedStatus = conversation.mutedStatus,
                isLegalHold = conversation.legalHoldStatus.showLegalHoldIndicator(),
                lastMessageContent = lastMessage.toUIPreview(unreadEventCount),
                badgeEventType = parsePrivateConversationEventType(
                    otherUser.connectionStatus,
                    otherUser.deleted,
                    parseConversationEventType(
                        conversation.mutedStatus,
                        unreadEventCount
                    )
                ),
                userId = otherUser.id,
                blockingState = otherUser.BlockState,
                teamId = otherUser.teamId,
                isArchived = conversation.archived,
                mlsVerificationStatus = conversation.mlsVerificationStatus,
                proteusVerificationStatus = conversation.proteusVerificationStatus
            )
        }

        else -> null // We don't care about connection requests
    }

    private fun searchShareableConversation(
        conversationDetails: List<ConversationItem>,
        searchQuery: String
    ): List<ConversationItem> {
        val matchingConversations =
            conversationDetails.filter { details ->
                when (details) {
                    is ConversationItem.GroupConversation -> details.groupName.contains(
                        searchQuery,
                        true
                    )

                    is ConversationItem.PrivateConversation -> details.conversationInfo.name.contains(
                        searchQuery,
                        true
                    )

                    is ConversationItem.ConnectionConversation -> false
                } or searchQuery.isBlank()
            }
        return matchingConversations
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
        importMediaState = importMediaState.copy(importedText = text)
    }

    private suspend fun handleSingleIntent(incomingIntent: ShareCompat.IntentReader) {
        incomingIntent.stream?.let { uri ->
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
        val importedMediaAssets = activity.intent.parcelableArrayList<Parcelable>(Intent.EXTRA_STREAM)?.mapNotNull {
            val fileUri = it.toString().toUri()
            handleImportedAsset(fileUri)
        } ?: listOf()

        importMediaState = importMediaState.copy(importedAssets = importedMediaAssets.toPersistentList())

        importedMediaAssets.firstOrNull { it.assetSizeExceeded != null }?.let {
            onSnackbarMessage(SendMessagesSnackbarMessages.MaxAssetSizeExceeded(it.assetSizeExceeded!!))
        }
    }

    private fun handleError(result: ScheduleNewAssetMessageResult, conversationId: ConversationId) {
        when (result) {
            is ScheduleNewAssetMessageResult.Success -> appLogger.d(
                "Successfully imported asset message to conversationId=${conversationId.toLogString()}"
            )

            is ScheduleNewAssetMessageResult.Failure.Generic ->
                appLogger.e(
                    "Failed to import asset message to conversationId=${conversationId.toLogString()}"
                )

            ScheduleNewAssetMessageResult.Failure.RestrictedFileType,
            ScheduleNewAssetMessageResult.Failure.DisabledByTeam -> {
                onSnackbarMessage(ConversationSnackbarMessages.ErrorAssetRestriction)
                appLogger.e(
                    "Failed to import asset message to conversationId=${conversationId.toLogString()}"
                )
            }
        }
    }

    fun onNewConversationPicked(conversationId: ConversationId) = viewModelScope.launch {
        importMediaState = importMediaState.copy(
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

    fun onNewSelfDeletionTimerPicked(selfDeletionDuration: SelfDeletionDuration) =
        viewModelScope.launch {
            val conversationId = importMediaState.selectedConversationItem.first().conversationId
            importMediaState = importMediaState.copy(
                selfDeletingTimer = SelfDeletionTimer.Enabled(selfDeletionDuration.value)
            )
            val logMap = importMediaState.selfDeletingTimer.toLogString(
                "user timer update for conversationId=${conversationId.toLogString()} on ImportMediaScreen"
            )
            appLogger.d("$SELF_DELETION_LOG_TAG: $logMap")
            persistNewSelfDeletionTimerUseCase(
                conversationId = conversationId,
                newSelfDeletionTimer = importMediaState.selfDeletingTimer
            )
        }

    private suspend fun handleImportedAsset(uri: Uri): ImportedMediaAsset? = withContext(dispatchers.io()) {
        when (val result = handleUriAsset.invoke(uri, saveToDeviceIfInvalid = false)) {
            is HandleUriAssetUseCase.Result.Failure.AssetTooLarge -> ImportedMediaAsset(result.assetBundle, result.maxLimitInMB)

            HandleUriAssetUseCase.Result.Failure.Unknown -> null
            is HandleUriAssetUseCase.Result.Success -> ImportedMediaAsset(result.assetBundle, null)
        }
    }

    private fun onSnackbarMessage(type: SnackBarMessage) = viewModelScope.launch {
        _infoMessage.emit(type)
    }
}

@Stable
data class ImportMediaAuthenticatedState(
    val avatarAsset: ImageAsset.UserAvatarAsset? = null,
    val importedAssets: PersistentList<ImportedMediaAsset> = persistentListOf(),
    val importedText: String? = null,
    val isImporting: Boolean = false,
    val shareableConversationListState: ShareableConversationListState = ShareableConversationListState(),
    val selectedConversationItem: List<ConversationItem> = emptyList(),
    val selfDeletingTimer: SelfDeletionTimer = SelfDeletionTimer.Enabled(null)
) {
    @Stable
    fun isImportingData() {
        importedText?.isNotEmpty() == true || importedAssets.isNotEmpty()
    }
}
