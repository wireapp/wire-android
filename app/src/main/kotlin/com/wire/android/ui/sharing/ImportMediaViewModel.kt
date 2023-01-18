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
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getMetaDataFromUri
import com.wire.android.util.getMimeType
import com.wire.android.util.isImageFile
import com.wire.android.util.parcelableArrayList
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class ImportMediaViewModel @Inject constructor(
    private val getSelf: GetSelfUserUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val observeConversationListDetails: ObserveConversationListDetailsUseCase,
    private val navigationManager: NavigationManager,
    val wireSessionImageLoader: WireSessionImageLoader,
    val dispatchers: DispatcherProvider,
) : ViewModel() {
    var importMediaState by mutableStateOf(
        ImportMediaState()
    )
        private set

    var shareableConversationListState by mutableStateOf(ShareableConversationListState())
        private set

    val selectedConversationsFlow = MutableStateFlow(emptyList<ConversationItem>())

    private val mutableSearchQueryFlow = MutableStateFlow("")

    private val searchQueryFlow = mutableSearchQueryFlow
        .asStateFlow()
        .debounce(SearchPeopleViewModel.DEFAULT_SEARCH_QUERY_DEBOUNCE)

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
                }, searchQueryFlow, selectedConversationsFlow
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
        selectedConversationsFlow.emit(listOf(conversation))
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

    fun onImportedMediaSent(conversationsList: List<Conversation>) = viewModelScope.launch(dispatchers.main()) {
        navigationManager.navigate(
            command = NavigationCommand(
                destination = if (conversationsList.size == 1) {
                    val conversation = conversationsList.first()
                    NavigationItem.Conversation.getRouteWithArgs(listOf(conversation.id))
                } else {
                    NavigationItem.Home.getRouteWithArgs()
                },
                backStackMode = BackStackMode.REMOVE_CURRENT
            )
        )
    }


    private fun handleMimeType(context: Context, type: String, uri: Uri) {
        when {
            isImageFile(type) -> {
                uri.getMetaDataFromUri(context).let {
                    appLogger.d("image type $it")

                    importMediaState.importedAssets.add(
                        ImportedMediaAsset.Image(
                            name = it.name,
                            size = it.size,
                            mimeType = type,
                            dataUri = uri
                        )
                    )
                }

            }

            else -> {
                uri.getMetaDataFromUri(context).let {
                    importMediaState.importedAssets.add(
                        ImportedMediaAsset.GenericAsset(
                            name = it.name,
                            size = it.size,
                            mimeType = type,
                            dataUri = uri
                        )
                    )
                    appLogger.d("other types $it")
                }
            }
        }
    }
}

data class ImportMediaState(
    val avatarAsset: ImageAsset.UserAvatarAsset? = null,
    val importedAssets: ArrayList<ImportedMediaAsset> = arrayListOf()
)
