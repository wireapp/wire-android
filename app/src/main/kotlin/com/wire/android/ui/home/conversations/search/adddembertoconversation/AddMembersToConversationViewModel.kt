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
 */

package com.wire.android.ui.home.conversations.search.adddembertoconversation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.conversations.search.AddMembersSearchNavArgs
import com.wire.android.ui.home.newconversation.model.Contact
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AddMembersToConversationViewModel @Inject constructor(
    private val addMemberToConversation: AddMemberToConversationUseCase,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val addMembersSearchNavArgs: AddMembersSearchNavArgs = savedStateHandle.navArgs()

    var newGroupState: AddMembersToConversationState by mutableStateOf(AddMembersToConversationState())
        private set

    fun addMembersToConversation() {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                // TODO: addMembersToConversationUseCase does not handle failure
                addMemberToConversation(
                    conversationId = addMembersSearchNavArgs.conversationId,
                    userIdList = newGroupState.selectedContacts.map { UserId(it.id, it.domain) }
                )
            }
            newGroupState = newGroupState.copy(isCompleted = true)
        }
    }

    fun updateSelectedContacts(selected: Boolean, contact: Contact) {
        newGroupState = if (selected) {
            newGroupState.copy(selectedContacts = (newGroupState.selectedContacts + contact).toImmutableSet())
        } else {
            newGroupState.copy(
                selectedContacts = newGroupState.selectedContacts.filterNot {
                    it.id == contact.id &&
                            it.domain == contact.domain
                }.toImmutableSet()
            )
        }
    }
}

data class AddMembersToConversationState(
    val selectedContacts: ImmutableSet<Contact> = persistentSetOf(),
    val isCompleted: Boolean = false,
)
