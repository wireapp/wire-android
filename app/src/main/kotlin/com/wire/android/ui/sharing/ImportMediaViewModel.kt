package com.wire.android.ui.sharing

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
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
import com.wire.android.util.ImageUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getMetaDataFromUri
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
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class ImportMediaViewModel @Inject constructor(
    private val getSelf: GetSelfUserUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val observeConversationListDetails: ObserveConversationListDetailsUseCase,
    private val navigationManager: NavigationManager,
    private val sendAssetMessage: ScheduleNewAssetMessageUseCase,
    private val kaliumFileSystem: KaliumFileSystem,
    private val getAssetSizeLimit: GetAssetSizeLimitUseCase,
    val wireSessionImageLoader: WireSessionImageLoader,
    val dispatchers: DispatcherProvider,
) : ViewModel() {
    var importMediaState by mutableStateOf(
        ImportMediaState()
    )
        private set

    var shareableConversationListState by mutableStateOf(ShareableConversationListState())
        private set

    val selectedConversationFlow = MutableStateFlow(emptyList<ConversationItem>())

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
                importMediaState = importMediaState.copy(avatarAsset = selfUser.previewPicture?.let {
                    ImageAsset.UserAvatarAsset(wireSessionImageLoader, it)
                })
            }
        }
    }

    private suspend fun observeConversationWithSearch() = viewModelScope.launch {
        combine(
            observeConversationListDetails()
                .map {
                    it.mapNotNull { conversationDetails ->
                        conversationDetails.toConversationItem(
                            wireSessionImageLoader,
                            userTypeMapper
                        )
                    }
                }, searchQueryFlow, selectedConversationFlow
        ) { conversations, searchQuery, selectedConversations ->
            val searchResult = if (searchQuery.isEmpty()) conversations else searchShareableConversation(
                conversations,
                searchQuery
            )
            ShareableConversationListState(
                initialConversations = conversations,
                searchQuery = searchQuery,
                hasNoConversations = conversations.isEmpty(),
                searchResult = searchResult,
                conversationsAddedToGroup = selectedConversations.toImmutableList()
            )
        }
            .flowOn(dispatchers.io())
            .collect { updatedState ->
                shareableConversationListState = updatedState
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

    fun selectConversationOnRadioGroup(conversation: ConversationItem) = viewModelScope.launch {
        selectedConversationFlow.emit(listOf(conversation))
    }

    fun onConversationClicked(conversationId: ConversationId) {
        shareableConversationListState.initialConversations.find { it.conversationId == conversationId }?.let {
            selectConversationOnRadioGroup(it)
        }
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
                    otherUser.previewPicture?.let { ImageAsset.UserAvatarAsset(wireSessionImageLoader, it) },
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

    private fun searchShareableConversation(conversationDetails: List<ConversationItem>, searchQuery: String): List<ConversationItem> {
        val matchingConversations =
            conversationDetails.filter { details ->
                when (details) {
                    is ConversationItem.GroupConversation -> details.groupName.contains(searchQuery, true)
                    is ConversationItem.PrivateConversation -> details.conversationInfo.name.contains(searchQuery, true)
                    is ConversationItem.ConnectionConversation -> false
                }
            }
        return matchingConversations
    }

    fun navigateBack() = viewModelScope.launch(dispatchers.main()) { navigationManager.navigateBack() }

    fun handleReceivedDataFromSharingIntent(activity: AppCompatActivity) {
        val incomingIntent = ShareCompat.IntentReader(activity)
        appLogger.e("Received data from sharing intent ${incomingIntent.streamCount}")
        when (incomingIntent.streamCount) {
            0 -> {
                // if stream count is 0 the type will be text, we check the type to double check if it is text
                // todo : handle the text , we can get the text from incomingIntent.text
            }

            1 -> {
                // ACTION_SEND
                incomingIntent.stream?.let { incomingIntent.type?.let { it1 -> handleMimeType(activity, it1, it) } }
            }

            else -> {
                // ACTION_SEND_MULTIPLE
                activity.intent.parcelableArrayList<Parcelable>(Intent.EXTRA_STREAM)?.forEach {
                    val fileUri = it.toString().toUri()
                    handleMimeType(activity, fileUri.getMimeType(activity).toString(), fileUri)
                }
            }
        }
    }

    fun onImportedMediaSent() = viewModelScope.launch {
        val conversation = shareableConversationListState.conversationsAddedToGroup.first()
        val assetsToSend = importMediaState.importedAssets

        if (assetsToSend.size > MAX_LIMIT_MEDIA_IMPORT) {
            onSnackbarMessage(ImportMediaSnackbarMessages.MaxAmountOfAssetsReached)
        } else {
            assetsToSend.forEach { importedAsset ->
                val isImage = importedAsset is ImportedMediaAsset.Image
                val assetLimitForCurrentUser = getAssetSizeLimit(isImage).toInt()
                val sizeOf1MB = 1024 * 1024
                val isAboveLimit = importedAsset.size > assetLimitForCurrentUser
                if (isAboveLimit) {
                    onSnackbarMessage(ImportMediaSnackbarMessages.MaxAssetSizeExceeded(assetLimitForCurrentUser.div(sizeOf1MB)))
                } else {
                    sendAssetMessage(
                        conversationId = conversation.conversationId,
                        assetDataPath = importedAsset.dataPath,
                        assetName = importedAsset.name,
                        assetDataSize = importedAsset.size,
                        assetMimeType = importedAsset.mimeType,
                        assetWidth = if (isImage) (importedAsset as ImportedMediaAsset.Image).width else 0,
                        assetHeight = if (isImage) (importedAsset as ImportedMediaAsset.Image).height else 0,
                    )
                }
            }
        }
        navigationManager.navigate(
            command = NavigationCommand(NavigationItem.Conversation.getRouteWithArgs(listOf(conversation.conversationId)))
        )
    }


    private fun handleMimeType(context: Context, mimeType: String, uri: Uri) = viewModelScope.launch {
        when {
            isImageFile(mimeType) -> {
                uri.getMetaDataFromUri(context).let {
                    appLogger.d("image type $it")
                    val assetKey = UUID.randomUUID().toString()
                    val tempAssetPath = kaliumFileSystem.tempFilePath(assetKey)

                    // Only resample the image if it is too large
                    uri.resampleImageAndCopyToTempPath(context, tempAssetPath, ImageUtil.ImageSizeClass.Medium)
                    val (imgWidth, imgHeight) =
                        ImageUtil.extractImageWidthAndHeight(
                            kaliumFileSystem.source(tempAssetPath).buffer().inputStream(),
                            mimeType
                        )
                    importMediaState.importedAssets.add(
                        ImportedMediaAsset.Image(
                            name = it.name,
                            size = it.size,
                            mimeType = mimeType,
                            dataPath = tempAssetPath,
                            dataUri = uri,
                            key = assetKey,
                            width = imgWidth,
                            height = imgHeight
                        )
                    )
                }

            }

            else -> {
                uri.getMetaDataFromUri(context).let {
                    val assetKey = UUID.randomUUID().toString()
                    val tempAssetPath = kaliumFileSystem.tempFilePath(assetKey)
                    importMediaState.importedAssets.add(
                        ImportedMediaAsset.GenericAsset(
                            name = it.name,
                            size = it.size,
                            mimeType = mimeType,
                            dataPath = tempAssetPath,
                            dataUri = uri,
                            key = assetKey
                        )
                    )
                    appLogger.d("other types $it")
                }
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

data class ImportMediaState(
    val avatarAsset: ImageAsset.UserAvatarAsset? = null,
    val importedAssets: ArrayList<ImportedMediaAsset> = arrayListOf()
)
