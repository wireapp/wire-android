package com.wire.android.ui.home.conversations.details.participants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.AssistedViewModel
import com.wire.android.navigation.NavQualifiedId
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.VoyagerNavigationItem
import com.wire.android.navigation.nav
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class GroupConversationParticipantsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase
) : BaseGroupConversationParticipantsViewModel(savedStateHandle, navigationManager, observeConversationMembers)

open class BaseGroupConversationParticipantsViewModel(
    override val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase
) : ViewModel(), AssistedViewModel<NavQualifiedId> {

    open val conversationId: ConversationId get() = param.qualifiedId

    open val maxNumberOfItems get() = -1 // -1 means return whole list

    var groupParticipantsState: GroupConversationParticipantsState by mutableStateOf(GroupConversationParticipantsState())

    init {
        observeConversationMembers()
    }

    private fun observeConversationMembers() {
        viewModelScope.launch {
            observeConversationMembers(conversationId, maxNumberOfItems)
                .collect {
                    groupParticipantsState = groupParticipantsState.copy(data = it)
                }
        }
    }

    fun navigateBack() = viewModelScope.launch {
        navigationManager.navigateBack()
    }

    fun openProfile(participant: UIParticipant) = viewModelScope.launch {
        if (participant.isSelf) navigateToSelfProfile()
        else navigateToOtherProfile(participant.id)
    }

    private suspend fun navigateToSelfProfile() =
        navigationManager.navigate(NavigationCommand(VoyagerNavigationItem.SelfUserProfile))

    private suspend fun navigateToOtherProfile(id: UserId) =
        navigationManager.navigate(NavigationCommand(VoyagerNavigationItem.OtherUserProfile(id.nav(), conversationId.nav())))

}
