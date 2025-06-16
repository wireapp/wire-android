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

package com.wire.android.ui.home.conversations.details.metadata

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.common.groupname.GroupMetadataState
import com.wire.android.ui.common.groupname.GroupNameMode
import com.wire.android.ui.common.groupname.GroupNameValidator
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RenameConversationUseCase
import com.wire.kalium.logic.feature.conversation.RenamingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class EditConversationMetadataViewModel @Inject constructor(
    private val dispatcher: DispatcherProvider,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val renameConversation: RenameConversationUseCase,
    val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editConversationNameNavArgs: EditConversationNameNavArgs = savedStateHandle.navArgs()
    private val conversationId: QualifiedID = editConversationNameNavArgs.conversationId

    val editConversationNameTextState: TextFieldState = TextFieldState()
    var editConversationState by mutableStateOf(GroupMetadataState(mode = GroupNameMode.EDITION))
        private set

    init {
        observeConversationDetails()
        observeConversationNameChanges()
    }

    private fun observeConversationDetails() {
        viewModelScope.launch {
            observeConversationDetails(conversationId)
                .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>()
                .map { it.conversationDetails }
                .distinctUntilChanged()
                .flowOn(dispatcher.io())
                .shareIn(this, SharingStarted.WhileSubscribed(), 1)
                .collectLatest {
                    editConversationNameTextState.setTextAndPlaceCursorAtEnd(it.conversation.name.orEmpty())
                    editConversationState = editConversationState.copy(
                        originalGroupName = it.conversation.name.orEmpty(),
                        isChannel = it.conversation.type == Conversation.Type.Group.Channel,
                    )
                }
        }
    }

    private fun observeConversationNameChanges() {
        viewModelScope.launch {
            editConversationNameTextState.textAsFlow()
                .dropWhile { it.isEmpty() } // ignore first empty value to not show the error before the user typed anything
                .collectLatest {
                editConversationState = GroupNameValidator.onGroupNameChange(it.toString(), editConversationState)
            }
        }
    }

    fun onGroupNameErrorAnimated() {
        editConversationState = GroupNameValidator.onGroupNameErrorAnimated(editConversationState)
    }

    fun saveNewGroupName() {
        viewModelScope.launch {
            withContext(dispatcher.io()) {
                renameConversation(conversationId, editConversationNameTextState.text.toString())
            }.let { renamingResult ->
                editConversationState = editConversationState.copy(
                    completed = when (renamingResult) {
                        is RenamingResult.Failure -> GroupMetadataState.Completed.Failure
                        is RenamingResult.Success -> GroupMetadataState.Completed.Success
                    }
                )
            }
        }
    }
}
