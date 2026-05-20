/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.home.conversations.details.participants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import kotlinx.coroutines.launch

open class GroupConversationParticipantsViewModel(
    private val conversationId: QualifiedID,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase,
) : ViewModel() {

    open val maxNumberOfItems get() = -1 // -1 means return whole list

    var groupParticipantsState: GroupConversationParticipantsState by mutableStateOf(GroupConversationParticipantsState())

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
}
