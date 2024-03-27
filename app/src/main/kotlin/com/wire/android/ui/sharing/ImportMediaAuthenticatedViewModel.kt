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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
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
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.search.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.ui.home.conversationslist.model.BlockState
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.parseConversationEventType
import com.wire.android.ui.home.conversationslist.parsePrivateConversationEventType
import com.wire.android.ui.home.conversationslist.showLegalHoldIndicator
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.android.util.ImageUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getAudioLengthInMs
import com.wire.android.util.parcelableArrayList
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.message.SelfDeletionTimer.Companion.SELF_DELETION_LOG_TAG
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageResult
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.joinAll
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
    private val sendAssetMessage: ScheduleNewAssetMessageUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val kaliumFileSystem: KaliumFileSystem,
    private val handleUriAssetUseCase: HandleUriAssetUseCase,
    private val persistNewSelfDeletionTimerUseCase: PersistNewSelfDeletionTimerUseCase,
    private val observeSelfDeletionSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    val dispatchers: DispatcherProvider,
) : ViewModel() {
    var importMediaState by mutableStateOf(ImportMediaAuthenticatedState())
        private set

    private val mutableSearchQueryFlow = MutableStateFlow("")

    private val searchQueryFlow = mutableSearchQueryFlow
        .asStateFlow()
        .debounce(DEFAULT_SEARCH_QUERY_DEBOUNCE)

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    val infoMessage = _infoMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            loadUserAvatar()
            observeConversationWithSearch()
        }
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
        searchQueryFlow.mapLatest { searchQuery ->
            val conversations = observeConversationListDetails(fromArchive = false).first()
                .mapNotNull { conversationDetails ->
                    conversationDetails.toConversationItem(
                        wireSessionImageLoader,
                        userTypeMapper
                    )
                }
            val searchResult =
                if (searchQuery.isEmpty()) conversations else searchShareableConversation(
                    conversations,
                    searchQuery
                )
            ShareableConversationListState(
                initialConversations = conversations,
                searchQuery = searchQuery,
                hasNoConversations = conversations.isEmpty(),
                searchResult = searchResult
            )
        }
            .flowOn(dispatchers.io())
            .collect { updatedState ->
                importMediaState =
                    importMediaState.copy(shareableConversationListState = updatedState)
            }
    }

    fun onSearchQueryChanged(searchQuery: TextFieldValue) {
        val textQueryChanged = mutableSearchQueryFlow.value != searchQuery.text
        // we set the state with a searchQuery, immediately to update the UI first
        viewModelScope.launch {
            if (textQueryChanged) {
                mutableSearchQueryFlow.emit(searchQuery.text)
            }
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
                }
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
                        ImportMediaSnackbarMessages.MaxAssetSizeExceeded(importedAsset.assetSizeExceeded!!)
                    )
                }
                importMediaState = importMediaState.copy(importedAssets = mutableListOf(importedAsset))
            }
        }
    }

    private suspend fun handleMultipleActionIntent(activity: AppCompatActivity) {
        val importedMediaAssets = activity.intent.parcelableArrayList<Parcelable>(Intent.EXTRA_STREAM)?.mapNotNull {
            val fileUri = it.toString().toUri()
            handleImportedAsset(fileUri)
        } ?: listOf()

        importMediaState = importMediaState.copy(importedAssets = importedMediaAssets)

        importedMediaAssets.firstOrNull { it.assetSizeExceeded != null }?.let {
            onSnackbarMessage(ImportMediaSnackbarMessages.MaxAssetSizeExceeded(it.assetSizeExceeded!!))
        }
    }

    fun checkRestrictionsAndSendImportedMedia(onSent: (ConversationId) -> Unit) =
        viewModelScope.launch(dispatchers.default()) {
            val conversation =
                importMediaState.selectedConversationItem.firstOrNull() ?: return@launch
            val assetsToSend = importMediaState.importedAssets
            val textToSend = importMediaState.importedText

            if (assetsToSend.size > MAX_LIMIT_MEDIA_IMPORT) {
                onSnackbarMessage(ImportMediaSnackbarMessages.MaxAmountOfAssetsReached)
            } else {
                val jobs: MutableCollection<Job> = mutableListOf()

                textToSend?.let {
                    sendTextMessage(
                        conversationId = conversation.conversationId,
                        text = it
                    )
                } ?: assetsToSend.forEach { importedAsset ->
                    val isImage = importedAsset is ImportedMediaAsset.Image
                    val job = viewModelScope.launch {
                        sendAssetMessage(
                            conversationId = conversation.conversationId,
                            assetDataPath = importedAsset.assetBundle.dataPath,
                            assetName = importedAsset.assetBundle.fileName,
                            assetDataSize = importedAsset.assetBundle.dataSize,
                            assetMimeType = importedAsset.assetBundle.mimeType,
                            assetWidth = if (isImage) (importedAsset as ImportedMediaAsset.Image).width else 0,
                            assetHeight = if (isImage) (importedAsset as ImportedMediaAsset.Image).height else 0,
                            audioLengthInMs = getAudioLengthInMs(
                                dataPath = importedAsset.assetBundle.dataPath,
                                mimeType = importedAsset.assetBundle.mimeType,
                            )
                        ).also {
                            val logConversationId = conversation.conversationId.toLogString()
                            if (it is ScheduleNewAssetMessageResult.Failure) {
                                appLogger.e(
                                    "Failed to import asset message to " +
                                            "conversationId=$logConversationId"
                                )
                            } else {
                                appLogger.d(
                                    "Success importing asset message to " +
                                            "conversationId=$logConversationId"
                                )
                            }
                        }
                    }
                    jobs.add(job)
                }

                jobs.joinAll()
                withContext(dispatchers.main()) {
                    onSent(conversation.conversationId)
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
        when (val result = handleUriAssetUseCase.invoke(uri, saveToDeviceIfInvalid = false, audioPath = null)) {
            is HandleUriAssetUseCase.Result.Failure.AssetTooLarge -> mapToImportedAsset(result.assetBundle, result.maxLimitInMB)

            HandleUriAssetUseCase.Result.Failure.Unknown -> null
            is HandleUriAssetUseCase.Result.Success -> mapToImportedAsset(result.assetBundle, null)
        }
    }

    private fun mapToImportedAsset(assetBundle: AssetBundle, assetSizeExceeded: Int?): ImportedMediaAsset {
        return when (assetBundle.assetType) {
            AttachmentType.IMAGE -> {
                val (imgWidth, imgHeight) = ImageUtil.extractImageWidthAndHeight(
                    kaliumFileSystem,
                    assetBundle.dataPath
                )
                ImportedMediaAsset.Image(
                    assetBundle = assetBundle,
                    width = imgWidth,
                    height = imgHeight,
                    assetSizeExceeded = assetSizeExceeded,
                    wireSessionImageLoader = wireSessionImageLoader
                )
            }

            AttachmentType.GENERIC_FILE,
            AttachmentType.AUDIO,
            AttachmentType.VIDEO -> {
                ImportedMediaAsset.GenericAsset(
                    assetBundle = assetBundle,
                    assetSizeExceeded = assetSizeExceeded
                )
            }
        }
    }

    fun onSnackbarMessage(type: SnackBarMessage) = viewModelScope.launch {
        _infoMessage.emit(type)
    }

    private companion object {
        const val MAX_LIMIT_MEDIA_IMPORT = 20
    }
}

@Stable
data class ImportMediaAuthenticatedState(
    val avatarAsset: ImageAsset.UserAvatarAsset? = null,
    val importedAssets: List<ImportedMediaAsset> = emptyList(),
    val importedText: String? = null,
    val isImporting: Boolean = false,
    val shareableConversationListState: ShareableConversationListState = ShareableConversationListState(),
    val selectedConversationItem: List<ConversationItem> = emptyList(),
    val selfDeletingTimer: SelfDeletionTimer = SelfDeletionTimer.Enabled(null)
)
