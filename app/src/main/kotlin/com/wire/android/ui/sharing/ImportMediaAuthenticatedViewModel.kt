package com.wire.android.ui.sharing

import android.content.Context
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
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.search.SearchPeopleViewModel
import com.wire.android.ui.home.conversationslist.model.BlockState
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.parseConversationEventType
import com.wire.android.ui.home.conversationslist.parsePrivateConversationEventType
import com.wire.android.ui.home.conversationslist.showLegalHoldIndicator
import com.wire.android.ui.home.messagecomposer.state.SelfDeletionDuration
import com.wire.android.util.FileManager
import com.wire.android.util.ImageUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getMetadataFromUri
import com.wire.android.util.getMimeType
import com.wire.android.util.isImageFile
import com.wire.android.util.parcelableArrayList
import com.wire.android.util.resampleImageAndCopyToTempPath
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
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
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.ZERO

@HiltViewModel
@OptIn(FlowPreview::class)
@Suppress("LongParameterList", "TooManyFunctions")
class ImportMediaAuthenticatedViewModel @Inject constructor(
    private val getSelf: GetSelfUserUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val observeConversationListDetails: ObserveConversationListDetailsUseCase,
    private val fileManager: FileManager,
    private val navigationManager: NavigationManager,
    private val sendAssetMessage: ScheduleNewAssetMessageUseCase,
    private val kaliumFileSystem: KaliumFileSystem,
    private val getAssetSizeLimit: GetAssetSizeLimitUseCase,
    private val persistNewSelfDeletionTimerUseCase: PersistNewSelfDeletionTimerUseCase,
    private val observeSelfDeletionSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    val wireSessionImageLoader: WireSessionImageLoader,
    val dispatchers: DispatcherProvider,
) : ViewModel() {
    var importMediaState by mutableStateOf(ImportMediaState())
        private set

    private val mutableSearchQueryFlow = MutableStateFlow("")

    private val searchQueryFlow = mutableSearchQueryFlow
        .asStateFlow()
        .debounce(SearchPeopleViewModel.DEFAULT_SEARCH_QUERY_DEBOUNCE)

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
            val conversations = observeConversationListDetails().first()
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
                isLegalHold = legalHoldStatus.showLegalHoldIndicator(),
                lastMessageContent = lastMessage.toUIPreview(unreadEventCount),
                badgeEventType = parseConversationEventType(
                    conversation.mutedStatus,
                    unreadEventCount
                ),
                hasOnGoingCall = hasOngoingCall,
                isSelfUserCreator = isSelfUserCreator,
                isSelfUserMember = isSelfUserMember,
                teamId = conversation.teamId,
                selfMemberRole = selfRole
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
                isLegalHold = legalHoldStatus.showLegalHoldIndicator(),
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
                teamId = otherUser.teamId
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

    fun navigateBack() = viewModelScope.launch(dispatchers.main()) {
        navigationManager.navigate(
            NavigationCommand(
                NavigationItem.Home.getRouteWithArgs(),
                BackStackMode.REMOVE_CURRENT
            )
        )
    }

    suspend fun handleReceivedDataFromSharingIntent(activity: AppCompatActivity) {
        val incomingIntent = ShareCompat.IntentReader(activity)
        appLogger.e("Received data from sharing intent ${incomingIntent.streamCount}")
        importMediaState = importMediaState.copy(isImporting = true)
        if (incomingIntent.streamCount == 0) {
            // if stream count is 0 the type will be text, we check the type to double check if it is text
            // todo : handle the text , we can get the text from incomingIntent.text
        } else {
            if (incomingIntent.isSingleShare) {
                // ACTION_SEND
                handleSingleIntent(incomingIntent, activity)
            } else {
                // ACTION_SEND_MULTIPLE
                handleMultipleActionIntent(activity)
            }
        }
        importMediaState = importMediaState.copy(isImporting = false)
    }

    private suspend fun handleSingleIntent(
        incomingIntent: ShareCompat.IntentReader,
        activity: AppCompatActivity
    ) {
        incomingIntent.stream?.let { uri ->
            uri.getMimeType(activity)?.let { mimeType ->
                handleImportedAsset(activity, mimeType, uri)?.let { importedAsset ->
                    importMediaState =
                        importMediaState.copy(importedAssets = mutableListOf(importedAsset))
                }
            }
        }
    }

    private suspend fun handleMultipleActionIntent(activity: AppCompatActivity) {
        val importedMediaAssets = mutableListOf<ImportedMediaAsset>()
        activity.intent.parcelableArrayList<Parcelable>(Intent.EXTRA_STREAM)?.forEach {
            val fileUri = it.toString().toUri()
            handleImportedAsset(
                activity,
                fileUri.getMimeType(activity).toString(),
                fileUri
            )?.let { importedAsset ->
                importedMediaAssets.add(importedAsset)
            }
        }
        importMediaState = importMediaState.copy(importedAssets = importedMediaAssets)
    }

    fun checkRestrictionsAndSendImportedMedia() = viewModelScope.launch(dispatchers.default()) {
        val conversation = importMediaState.selectedConversationItem.firstOrNull() ?: return@launch
        val assetsToSend = importMediaState.importedAssets

        if (assetsToSend.size > MAX_LIMIT_MEDIA_IMPORT) {
            onSnackbarMessage(ImportMediaSnackbarMessages.MaxAmountOfAssetsReached)
        } else {
            val jobs: MutableCollection<Job> = mutableListOf()
            assetsToSend.forEach { importedAsset ->
                val isImage = importedAsset is ImportedMediaAsset.Image
                val job = viewModelScope.launch {
                    sendAssetMessage(
                        conversationId = conversation.conversationId,
                        assetDataPath = importedAsset.dataPath,
                        assetName = importedAsset.name,
                        assetDataSize = importedAsset.size,
                        assetMimeType = importedAsset.mimeType,
                        assetWidth = if (isImage) (importedAsset as ImportedMediaAsset.Image).width else 0,
                        assetHeight = if (isImage) (importedAsset as ImportedMediaAsset.Image).height else 0
                    )
                }
                jobs.add(job)
            }
            jobs.joinAll()
            navigateToConversation(conversation.conversationId)
        }
    }

    fun onNewConversationPicked(conversationId: ConversationId) = viewModelScope.launch {
        importMediaState = importMediaState.copy(
            selfDeletingTimer = observeSelfDeletionSettingsForConversation(
                conversationId = conversationId,
                considerSelfUserSettings = true
            ).first()
        )
    }

    fun onNewSelfDeletionTimerPicked(selfDeletionDuration: SelfDeletionDuration) =
        viewModelScope.launch {
            importMediaState = importMediaState.copy(
                selfDeletingTimer = SelfDeletionTimer.Enabled(selfDeletionDuration.value)
            )
            persistNewSelfDeletionTimerUseCase(
                conversationId = importMediaState.selectedConversationItem.first().conversationId,
                newSelfDeletionTimer = importMediaState.selfDeletingTimer
            )
        }

    private suspend fun navigateToConversation(conversationId: ConversationId) {
        navigationManager.navigate(
            NavigationCommand(
                NavigationItem.Conversation.getRouteWithArgs(listOf(conversationId)),
                backStackMode = BackStackMode.CLEAR_TILL_START
            )
        )
    }

    fun currentSelectedConversationsCount() = if (importMediaState.importedAssets.isNotEmpty()) {
        importMediaState.selectedConversationItem.size
    } else {
        0
    }

    private suspend fun handleImportedAsset(
        context: Context,
        importedAssetMimeType: String,
        uri: Uri
    ): ImportedMediaAsset? = withContext(dispatchers.io()) {
        val assetKey = UUID.randomUUID().toString()
        val fileMetadata = uri.getMetadataFromUri(context)
        val tempAssetPath = kaliumFileSystem.tempFilePath(assetKey)
        val mimeType = fileMetadata.mimeType.ifEmpty { importedAssetMimeType }
        when {
            isAboveLimit(isImageFile(mimeType), fileMetadata.size) -> null
            isImageFile(mimeType) -> {
                // Only resample the image if it is too large
                val resampleSize = uri.resampleImageAndCopyToTempPath(
                    context,
                    tempAssetPath,
                    ImageUtil.ImageSizeClass.Medium
                )
                if (resampleSize <= 0) return@withContext null

                val (imgWidth, imgHeight) = ImageUtil.extractImageWidthAndHeight(
                    kaliumFileSystem,
                    tempAssetPath
                )

                return@withContext ImportedMediaAsset.Image(
                    name = fileMetadata.name,
                    size = fileMetadata.size,
                    mimeType = mimeType,
                    dataPath = tempAssetPath,
                    dataUri = uri,
                    key = assetKey,
                    width = imgWidth,
                    height = imgHeight
                )
            }

            else -> {
                fileManager.copyToPath(uri, tempAssetPath)
                return@withContext ImportedMediaAsset.GenericAsset(
                    name = fileMetadata.name,
                    size = fileMetadata.size,
                    mimeType = mimeType,
                    dataPath = tempAssetPath,
                    dataUri = uri,
                    key = assetKey
                )
            }
        }
    }

    private suspend fun isAboveLimit(isImage: Boolean, size: Long): Boolean {
        val assetLimitForCurrentUser = getAssetSizeLimit(isImage).toInt()
        val sizeOf1MB = SIZE_OF_1_MB * SIZE_OF_1_MB
        val isAboveLimit = size > assetLimitForCurrentUser
        if (isAboveLimit) {
            onSnackbarMessage(
                ImportMediaSnackbarMessages.MaxAssetSizeExceeded(
                    assetLimitForCurrentUser.div(sizeOf1MB)
                )
            )
        }
        return isAboveLimit
    }

    fun onSnackbarMessage(type: SnackBarMessage) = viewModelScope.launch {
        _infoMessage.emit(type)
    }

    private companion object {
        const val MAX_LIMIT_MEDIA_IMPORT = 20
        const val SIZE_OF_1_MB = 1024
    }
}

@Stable
data class ImportMediaState(
    val avatarAsset: ImageAsset.UserAvatarAsset? = null,
    val importedAssets: List<ImportedMediaAsset> = emptyList(),
    val isImporting: Boolean = false,
    val shareableConversationListState: ShareableConversationListState = ShareableConversationListState(),
    val selectedConversationItem: List<ConversationItem> = emptyList(),
    val selfDeletingTimer: SelfDeletionTimer = SelfDeletionTimer.Enabled(ZERO)
)
