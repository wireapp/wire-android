package com.wire.android.ui.sharing

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.ContactMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.mapper.toUIPreview
import com.wire.android.model.ImageAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.search.SearchAllPeopleViewModel
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
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchPublicUsersUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.functional.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class ImportMediaViewModel @Inject constructor(
    private val getSelf: GetSelfUserUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val observeConversationListDetails: ObserveConversationListDetailsUseCase,
    val wireSessionImageLoader: WireSessionImageLoader,
    val dispatchers: DispatcherProvider,
    getAllKnownUsers: GetAllContactsUseCase,
    searchKnownUsers: SearchKnownUsersUseCase,
    searchPublicUsers: SearchPublicUsersUseCase,
    contactMapper: ContactMapper,
    sendConnectionRequest: SendConnectionRequestUseCase,
    navigationManager: NavigationManager
) : SearchAllPeopleViewModel(
    getAllKnownUsers = getAllKnownUsers,
    sendConnectionRequest = sendConnectionRequest,
    searchKnownUsers = searchKnownUsers,
    searchPublicUsers = searchPublicUsers,
    contactMapper = contactMapper,
    dispatcher = dispatchers,
    navigationManager = navigationManager
) {
    var importMediaState by mutableStateOf(
        ImportMediaState()
    )
        private set

    var shareableConversationListState by mutableStateOf(ShareableConversationListState())
        private set

    init {
        loadUserAvatar()
        observeConversationWithSearch()
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

    private fun observeConversationWithSearch() = viewModelScope.launch {
        searchQueryFlow.combine(observeConversationListDetails()
            .map {
                it.map { conversationDetails ->
                    conversationDetails.toConversationItem(
                        wireSessionImageLoader,
                        userTypeMapper
                    )
                }.filterNotNull()
            })
            .flowOn(dispatchers.io())
            .collect { (searchQuery, conversationItems) ->
                shareableConversationListState = shareableConversationListState.copy(
                    searchResult = if (searchQuery.isEmpty()) conversationItems else searchShareableConversation(
                        conversationItems,
                        searchQuery
                    ),
                    hasNoConversations = conversationItems.isEmpty(),
                    searchQuery = searchQuery
                )
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
