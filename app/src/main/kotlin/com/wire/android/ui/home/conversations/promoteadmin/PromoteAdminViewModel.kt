/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
 */
package com.wire.android.ui.home.conversations.promoteadmin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.avatar
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveEligibleMembersForConversationAdminRoleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PromoteAdminViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeEligibleMembers: ObserveEligibleMembersForConversationAdminRoleUseCase,
) : ViewModel() {

    private val navArgs: PromoteAdminNavArgs = savedStateHandle.navArgs()

    private val allMembers = MutableStateFlow<List<PromoteAdminMemberItem>>(emptyList())
    private val searchQuery = MutableStateFlow("")
    private val selectedUserId = MutableStateFlow<UserId?>(null)

    private val _state: MutableStateFlow<PromoteAdminState> = MutableStateFlow(PromoteAdminState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(allMembers, searchQuery, selectedUserId) { members, query, selected ->
                PromoteAdminState(
                    searchQuery = query,
                    filteredMembers = filter(members, query),
                    selectedUserId = selected,
                    isButtonEnabled = selected != null,
                )
            }.collect { newState ->
                _state.update { newState }
            }
        }

        viewModelScope.launch {
            observeEligibleMembers(navArgs.conversationId).collect { members ->
                allMembers.value = members.map { it.toMemberItem() }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onUserSelected(userId: UserId) {
        selectedUserId.value = if (selectedUserId.value == userId) null else userId
    }

    fun onPromoteAdminAndLeave() {
        TODO("implement with use cases")
    }

    private fun filter(members: List<PromoteAdminMemberItem>, query: String): List<PromoteAdminMemberItem> =
        if (query.isBlank()) {
            members
        } else {
            val normalized = query.removePrefix("@")
            members.filter {
                it.name.contains(normalized, ignoreCase = true) ||
                        it.handle.contains(normalized, ignoreCase = true)
            }
        }

    private fun MemberDetails.toMemberItem() = PromoteAdminMemberItem(
        userId = user.id,
        name = user.name.orEmpty(),
        handle = user.handle.orEmpty(),
        avatarData = user.avatar(connectionState = (user as? OtherUser)?.connectionStatus),
    )
}

data class PromoteAdminState(
    val searchQuery: String = "",
    val filteredMembers: List<PromoteAdminMemberItem> = emptyList(),
    val selectedUserId: UserId? = null,
    val isButtonEnabled: Boolean = false,
)

data class PromoteAdminMemberItem(
    val userId: UserId,
    val name: String,
    val handle: String,
    val avatarData: UserAvatarData = UserAvatarData(),
)
