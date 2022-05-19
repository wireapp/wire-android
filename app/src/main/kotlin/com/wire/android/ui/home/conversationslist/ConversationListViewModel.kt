package com.wire.android.ui.home.conversationslist

import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserStatus
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversationslist.mock.mockAllMentionList
import com.wire.android.ui.home.conversationslist.mock.mockCallHistory
import com.wire.android.ui.home.conversationslist.mock.mockMissedCalls
import com.wire.android.ui.home.conversationslist.mock.mockUnreadMentionList
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.NewActivity
import com.wire.android.ui.home.conversationslist.model.UserInfo
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getConversationColor
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationDetails.Group
import com.wire.kalium.logic.data.conversation.ConversationDetails.OneOne
import com.wire.kalium.logic.data.conversation.ConversationDetails.Self
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.conversation.UserType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.message.MarkMessagesAsNotifiedUseCase
import com.wire.kalium.logic.util.toStringDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@ExperimentalMaterial3Api
@Suppress("MagicNumber")
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val observeConversationDetailsList: ObserveConversationListDetailsUseCase,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val markMessagesAsNotified: MarkMessagesAsNotifiedUseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    var state by mutableStateOf(ConversationListState())
        private set

    var errorState by mutableStateOf<ConversationOperationErrorState?>(null)

    init {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                observeConversationDetailsList().map {
                    it.toConversationsFoldersMap()
                }.collect { detailedList ->
                    val newActivities = listOf<NewActivity>() // TODO: needs to be implemented
                    val missedCalls = mockMissedCalls // TODO: needs to be implemented
                    val unreadMentions = mockUnreadMentionList // TODO: needs to be implemented

                    state = ConversationListState(
                        newActivities = newActivities,
                        conversations = detailedList,
                        missedCalls = missedCalls,
                        callHistory = mockCallHistory, // TODO: needs to be implemented
                        unreadMentions = unreadMentions,
                        allMentions = mockAllMentionList, // TODO: needs to be implemented
                        unreadMentionsCount = unreadMentions.size,
                        missedCallsCount = missedCalls.size,
                        newActivityCount = newActivities.size
                    )
                }
            }
        }

        viewModelScope.launch {
            markMessagesAsNotified(null, System.currentTimeMillis().toStringDate()) // TODO Failure is ignored
        }
    }

    private fun List<ConversationDetails>.toConversationsFoldersMap(): Map<ConversationFolder, List<GeneralConversation>> =
        mapOf(ConversationFolder.Predefined.Conversations to this.toGeneralConversationList())

    fun openConversation(conversationItem: ConversationItem) {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.Conversation.getRouteWithArgs(listOf(conversationItem.conversationType.conversationId))
                )
            )
        }
    }

    fun openNewConversation() {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.NewConversation.getRouteWithArgs()
                )
            )
        }
    }

    fun muteConversation(conversationId: ConversationId?, mutedConversationStatus: MutedConversationStatus) {
        conversationId?.let {
            viewModelScope.launch {
                when (updateConversationMutedStatus(conversationId, mutedConversationStatus, Date().time)) {
                    ConversationUpdateStatusResult.Failure -> errorState = ConversationOperationErrorState.MutingOperationErrorState()
                    ConversationUpdateStatusResult.Success -> appLogger.d("MutedStatus changed for conversation: $conversationId")
                }
            }
        }
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun addConversationToFavourites(id: String) {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun moveConversationToFolder(id: String) {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun moveConversationToArchive(id: String) {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun clearConversationContent(id: String) {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun blockUser(id: String) {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun leaveGroup(id: String) {
    }

    private fun List<ConversationDetails>.toGeneralConversationList(): List<GeneralConversation> = filter {
        it is Group || it is OneOne
    }.map { details ->
        val conversation = details.conversation

        when (details) {
            is Group -> {
                GeneralConversation(
                    ConversationType.GroupConversation(
                        groupColorValue = getConversationColor(conversation.id),
                        groupName = conversation.name.orEmpty(),
                        conversationId = conversation.id,
                        mutedStatus = conversation.mutedStatus,
                        isLegalHold = details.legalHoldStatus.showLegalHoldIndicator()
                    )
                )
            }
            is OneOne -> {
                val otherUser = details.otherUser

                GeneralConversation(
                    ConversationType.PrivateConversation(
                        userInfo = UserInfo(
                            otherUser.previewPicture?.let { UserAvatarAsset(it) },
                            UserStatus.NONE // TODO Get actual status
                        ),
                        conversationInfo = ConversationInfo(
                            name = otherUser.name.orEmpty(),
                            membership = mapUserType(details.userType)
                        ),
                        conversationId = conversation.id,
                        mutedStatus = conversation.mutedStatus,
                        isLegalHold = details.legalHoldStatus.showLegalHoldIndicator()
                    )
                )
            }
            is Self -> {
                throw IllegalArgumentException("Self conversations should not be visible to the user.")
            }
        }
    }

    private fun mapUserType(userType: UserType): Membership {
        return when (userType) {
            UserType.GUEST -> Membership.Guest
            UserType.FEDERATED -> Membership.Federated
            UserType.EXTERNAL -> Membership.External
            UserType.INTERNAL -> Membership.None
            else -> {
                throw IllegalStateException("Unknown UserType")
            }
        }
    }
}

private fun LegalHoldStatus.showLegalHoldIndicator() = this == LegalHoldStatus.ENABLED
