package com.wire.android.ui.home.conversations.details.participants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
open class GroupConversationParticipantsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase
) : ViewModel() {

    open val maxNumberOfItems get() = -1 // -1 means return whole list

    var groupParticipantsState: GroupConversationParticipantsState by mutableStateOf(GroupConversationParticipantsState())

    private val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    open fun navigateBack() = viewModelScope.launch {
        navigationManager.navigateBack()
    }

    private fun observeConversationMembers() {
        viewModelScope.launch {
            observeConversationMembers(conversationId, maxNumberOfItems)
                .collect {
                    groupParticipantsState = groupParticipantsState.copy(data = it)
                }
        }
    }

    init {
        observeConversationMembers()
    }
}
