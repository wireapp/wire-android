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
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.home.conversations.avatar
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.conversation.ObserveEligibleMembersForConversationAdminRoleUseCase
import com.wire.kalium.logic.feature.conversation.PromoteAdminAndLeaveConversationUseCase
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PromoteAdminViewModel(
    private val promoteAdminAndLeave: PromoteAdminAndLeaveConversationUseCase,
    private val observeEligibleMembers: ObserveEligibleMembersForConversationAdminRoleUseCase,
    private val observeConversationMembers: ObserveConversationMembersUseCase,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ActionsViewModel<PromoteAdminAction>() {

    private val navArgs: PromoteAdminNavArgs = savedStateHandle.navArgs()

    private val clientEligibleMembers = MutableStateFlow<List<PromoteAdminMemberItem>>(emptyList())
    private val allConversationMembers = MutableStateFlow<List<PromoteAdminMemberItem>>(emptyList())
    private val eligibleMemberIds = MutableStateFlow(navArgs.eligibleMembers.map { it.toUserId() }.toSet())
    private val searchQuery = MutableStateFlow("")
    private val selectedUserId = MutableStateFlow<UserId?>(null)

    private val _state: MutableStateFlow<PromoteAdminState> = MutableStateFlow(PromoteAdminState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                clientEligibleMembers,
                allConversationMembers,
                eligibleMemberIds,
                searchQuery,
                selectedUserId
            ) { clientEligibleMembers, allConversationMembers, eligibleIds, query, selected ->
                val members = if (eligibleIds.isEmpty()) {
                    clientEligibleMembers
                } else {
                    allConversationMembers.filterByEligibleIds(eligibleIds)
                }
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
                clientEligibleMembers.value = members.map { it.toMemberItem() }
            }
        }

        viewModelScope.launch {
            observeConversationMembers(navArgs.conversationId).collect { members ->
                allConversationMembers.value = members.map { it.toMemberItem() }
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
        val userId = state.value.selectedUserId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val result = withContext(NonCancellable + dispatchers.io()) {
                promoteAdminAndLeave(navArgs.conversationId, userId)
            }

            val action = when (result) {
                is PromoteAdminAndLeaveConversationUseCase.Result.Success -> PromoteAdminAction.Success
                is PromoteAdminAndLeaveConversationUseCase.Result.FailedToPromoteUser -> PromoteAdminAction.FailedToPromoteUser
                is PromoteAdminAndLeaveConversationUseCase.Result.FailedToLeaveConversation -> {
                    if (result.eligibleMembers.isEmpty()) {
                        PromoteAdminAction.FailedToLeaveConversation
                    } else {
                        selectedUserId.value = null
                        eligibleMemberIds.value = result.eligibleMembers.toSet()
                        PromoteAdminAction.FailedToLeaveConversation
                    }
                }
            }

            sendAction(action)

            _state.update { it.copy(isLoading = false) }
        }
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

    private fun List<PromoteAdminMemberItem>.filterByEligibleIds(eligibleIds: Set<UserId>): List<PromoteAdminMemberItem> =
        if (eligibleIds.isEmpty()) this else filter { it.userId in eligibleIds }

    private fun String.toUserId() = UserId(
        value = substringBeforeLast(USER_ID_DOMAIN_SEPARATOR),
        domain = substringAfterLast(USER_ID_DOMAIN_SEPARATOR),
    )

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
    val isLoading: Boolean = false,
)

sealed interface PromoteAdminAction {
    data object Success : PromoteAdminAction
    data object FailedToPromoteUser : PromoteAdminAction
    data object FailedToLeaveConversation : PromoteAdminAction
}

data class PromoteAdminMemberItem(
    val userId: UserId,
    val name: String,
    val handle: String,
    val avatarData: UserAvatarData = UserAvatarData(),
)
