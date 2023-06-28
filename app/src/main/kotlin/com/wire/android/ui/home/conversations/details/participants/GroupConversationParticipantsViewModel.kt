/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.home.conversations.details.participants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.GroupDetailsBaseViewModel
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.BotService
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class GroupConversationParticipantsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase,
    qualifiedIdMapper: QualifiedIdMapper
) : GroupDetailsBaseViewModel(savedStateHandle) {

    open val maxNumberOfItems get() = -1 // -1 means return whole list

    var groupParticipantsState: GroupConversationParticipantsState by mutableStateOf(GroupConversationParticipantsState())

    private val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    init {
        runRefreshUsersWithoutMetadata()
        observeConversationMembers()
    }

    private fun runRefreshUsersWithoutMetadata() {
        viewModelScope.launch {
            refreshUsersWithoutMetadata()
        }
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
        else if (participant.isService && participant.botService != null) navigateToServiceProfile(participant.botService)
        else navigateToOtherProfile(participant.id)
    }

    private suspend fun navigateToSelfProfile() =
        navigationManager.navigate(NavigationCommand(NavigationItem.SelfUserProfile.getRouteWithArgs()))

    private suspend fun navigateToOtherProfile(id: UserId) =
        navigationManager.navigate(NavigationCommand(NavigationItem.OtherUserProfile.getRouteWithArgs(listOf(id, conversationId))))

    private suspend fun navigateToServiceProfile(botServiceId: BotService) {
        navigationManager.navigate(NavigationCommand(NavigationItem.ServiceDetails.getRouteWithArgs(listOf(botServiceId, conversationId))))
    }
}
