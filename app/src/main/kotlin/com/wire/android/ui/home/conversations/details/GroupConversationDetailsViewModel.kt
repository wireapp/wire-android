package com.wire.android.ui.home.conversations.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModel
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupConversationDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase
) : GroupConversationParticipantsViewModel(savedStateHandle, navigationManager, observeConversationMembers) {

    override val maxNumberOfItems: Int get() = MAX_NUMBER_OF_PARTICIPANTS

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    var groupOptionsState: GroupConversationOptionsState by mutableStateOf(GroupConversationOptionsState())

    init {
        observeConversationDetails()
    }

    fun navigateToFullParticipantsList() = viewModelScope.launch {
        navigationManager.navigate(
            command = NavigationCommand(
                destination = NavigationItem.GroupConversationAllParticipants.getRouteWithArgs(listOf(conversationId))
            )
        )
    }

    private fun observeConversationDetails() {
        viewModelScope.launch {
            observeConversationDetails(conversationId).collect { conversationDetails ->
                if (conversationDetails is ConversationDetails.Group) {
                    groupOptionsState = groupOptionsState.copy(groupName = conversationDetails.conversation.name.orEmpty())
                }
            }
        }
    }

    companion object {
        const val MAX_NUMBER_OF_PARTICIPANTS = 4
    }
}

@HiltViewModel
class GroupConversationOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase
) : ViewModel() {

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)?.parseIntoQualifiedID() ?: throw IllegalStateException()

    var groupOptionsState: GroupConversationOptionsState by mutableStateOf(GroupConversationOptionsState())

    fun navigateBack() = viewModelScope.launch {
        navigationManager.navigateBack()
    }

    init {
        observeConversationDetails()
    }

    private fun observeConversationDetails() {
        viewModelScope.launch {
            observeConversationDetails(conversationId).collect { conversationDetails ->
                with(conversationDetails) {
                    if (this is ConversationDetails.Group) {
                        groupOptionsState = groupOptionsState.copy(
                            groupName = conversation.name.orEmpty(),
                            isTeamGroup = conversation.isTeamGroup(),
                            isGuestAllowed = conversation.isGuestAllowed(),
                            isServicesAllowed = conversation.isServicesAllowed()
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            observeConversationMembers(conversationId).map { it.isSelfAnAdmin }.distinctUntilChanged().collect { isSelfAdmin ->
                groupOptionsState = groupOptionsState.copy(isChangingAllowed = isSelfAdmin)
            }
        }
    }

    private fun Conversation.isTeamGroup(): Boolean = (this.teamId != null)
    private fun Conversation.isGuestAllowed(): Boolean = this.accessRole?.let {
        (it.containsAll(listOf(Conversation.AccessRole.GUEST, Conversation.AccessRole.NON_TEAM_MEMBER)))
    } ?: TODO(
        "swagger: This field is optional. If it is not present, " +
                "the default will be [team_member, non_team_member, service]"
    )

    private fun Conversation.isServicesAllowed(): Boolean = this.accessRole?.let {
        (it.contains(Conversation.AccessRole.SERVICE))
    } ?: TODO(
        "swagger: This field is optional. If it is not present, " +
                "the default will be [team_member, non_team_member, service]"
    )
}

