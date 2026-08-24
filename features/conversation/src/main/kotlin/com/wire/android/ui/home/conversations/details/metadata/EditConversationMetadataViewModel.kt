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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.metro.WireAssistedViewModelBinding
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.home.conversations.EditConversationMetadataManualViewModelFactoryGroup
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RenameConversationUseCase
import com.wire.kalium.logic.feature.conversation.RenamingResult
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@WireAssistedViewModelBinding(
    group = EditConversationMetadataManualViewModelFactoryGroup::class,
    factoryMethod = "editConversationMetadataViewModel",
)
class EditConversationMetadataViewModel @AssistedInject constructor(
    private val dispatcher: DispatcherProvider,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val renameConversation: RenameConversationUseCase,
    @Assisted navigationArgs: EditConversationNameNavArgs,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(navigationArgs: EditConversationNameNavArgs): EditConversationMetadataViewModel
    }

    private val conversationId: QualifiedID = navigationArgs.conversationId

    val editConversationNameTextState: TextFieldState = TextFieldState()
    var editConversationState by mutableStateOf(EditConversationMetadataState())
        private set

    init {
        getConversationDetails()
        observeConversationNameChanges()
    }

    private fun getConversationDetails() {
        viewModelScope.launch {
            val conversationDetails = observeConversationDetails(conversationId)
                .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>()
                .map { it.conversationDetails }
                .first()

            editConversationNameTextState.setTextAndPlaceCursorAtEnd(conversationDetails.conversation.name.orEmpty())
            editConversationState = editConversationState.copy(
                originalGroupName = conversationDetails.conversation.name.orEmpty(),
                isChannel = conversationDetails.conversation.type == Conversation.Type.Group.Channel,
            )
        }
    }

    private fun observeConversationNameChanges() {
        viewModelScope.launch {
            editConversationNameTextState.textAsFlow()
                .dropWhile { it.isEmpty() } // ignore first empty value to not show the error before the user typed anything
                .collectLatest {
                    editConversationState = EditGroupNameValidator.onGroupNameChange(it.toString(), editConversationState)
                }
        }
    }

    fun onGroupNameErrorAnimated() {
        editConversationState = EditGroupNameValidator.onGroupNameErrorAnimated(editConversationState)
    }

    fun saveNewGroupName() {
        viewModelScope.launch {
            withContext(dispatcher.io()) {
                renameConversation(conversationId, editConversationNameTextState.text.toString().trim())
            }.let { renamingResult ->
                editConversationState = editConversationState.copy(
                    completed = when (renamingResult) {
                        is RenamingResult.Failure -> EditConversationMetadataState.Completed.Failure
                        is RenamingResult.Success -> EditConversationMetadataState.Completed.Success
                    }
                )
            }
        }
    }
}
