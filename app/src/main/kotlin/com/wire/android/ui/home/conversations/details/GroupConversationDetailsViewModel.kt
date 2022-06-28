package com.wire.android.ui.home.conversations.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsState
import com.wire.android.ui.home.conversations.name
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupConversationDetailsViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeConversationMembers: ObserveConversationMembersUseCase,
    private val uiParticipantMapper: UIParticipantMapper
) : ViewModel() {

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    var groupOptionsState: GroupConversationOptionsState by mutableStateOf(GroupConversationOptionsState())
    var groupParticipantsState: GroupConversationParticipantsState by mutableStateOf(GroupConversationParticipantsState())


    fun navigateBack() = viewModelScope.launch {
        navigationManager.navigateBack()
    }

    init {
        observeConversationDetails()
        observeConversationMembers()
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

    private fun observeConversationMembers() {
        viewModelScope.launch {
            observeConversationMembers(conversationId)
                .map {
                    it.sortedBy { it.name }
                        .take(MAX_NUMBER_OF_PARTICIPANTS)
                        .map { uiParticipantMapper.toUIParticipant(it) } to it.size
                }
                .collect { (uiParticipants, allParticipantsCount) ->
                    groupParticipantsState = groupParticipantsState.copy(
                        participants = uiParticipants,
                        allParticipantsCount = allParticipantsCount
                    )
                }
        }
    }

    companion object {
        const val MAX_NUMBER_OF_PARTICIPANTS = 4
    }
}
