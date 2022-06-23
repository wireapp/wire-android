package com.wire.android.ui.home.conversations.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupConversationDetailsViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeConversationDetails: ObserveConversationDetailsUseCase
) : ViewModel() {

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    var groupOptionsState: GroupConversationOptionsState by mutableStateOf(GroupConversationOptionsState())

    init {
        viewModelScope.launch {
            observeConversationDetails(conversationId).collect { conversationDetails ->
                if (conversationDetails is ConversationDetails.Group) {
                    groupOptionsState = groupOptionsState.copy(groupName = conversationDetails.conversation.name.orEmpty())
                }
            }
        }
    }

    fun navigateBack() = viewModelScope.launch {
        navigationManager.navigateBack()
    }
}
