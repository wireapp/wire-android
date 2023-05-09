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

package com.wire.android.ui.home.conversations.details.editselfdeletingmessages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.messagecomposer.state.SelfDeletionDuration
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.conversation.messagetimer.UpdateMessageTimerUseCase
import com.wire.kalium.logic.feature.selfdeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class EditSelfDeletingMessagesViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dispatcher: DispatcherProvider,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val observeSelfDeletionTimerSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    private val updateMessageTimer: UpdateMessageTimerUseCase,
    savedStateHandle: SavedStateHandle,
    qualifiedIdMapper: QualifiedIdMapper,
) : ViewModel() {

    val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        checkNotNull(savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)) {
            "No conversationId was provided via savedStateHandle to EditSelfDeletingMessagesViewModel"
        }
    )

    var editSelfDeletingMessagesState by mutableStateOf(
        EditSelfDeletingMessagesState()
    )

    init {
        observeSelfDeletingMessages()
    }

    private fun observeSelfDeletingMessages() {
        viewModelScope.launch {
            combine(
                observeSelfDeletionTimerSettingsForConversation(conversationId, includeSelfSettings = false),
                observeConversationMembers(conversationId)
                    .map { it.isSelfAnAdmin }
                    .distinctUntilChanged()
            ) { selfDeletingMessages, isSelfAnAdmin ->
                editSelfDeletingMessagesState = editSelfDeletingMessagesState.copy(
                    isLoading = selfDeletingMessages.isEnforcedByTeam || !isSelfAnAdmin,
                    isEnabled = selfDeletingMessages.isEnforcedByGroup,
                    selfDeletingDuration = selfDeletingMessages.toDuration(),
                    currentlySelected = selfDeletingMessages.toDuration().toSelfDeletionDuration()
                )
            }.collect()
        }
    }

    fun updateSelfDeletingMessageOption(shouldBeEnabled: Boolean) {
        viewModelScope.launch {
            if (shouldBeEnabled) {
                editSelfDeletingMessagesState = editSelfDeletingMessagesState.copy(
                    isEnabled = true
                )
            } else {
                editSelfDeletingMessagesState = editSelfDeletingMessagesState.copy(
                    isLoading = true,
                    isEnabled = false
                )
                editSelfDeletingMessagesState = when (updateMessageTimer(conversationId, null)) {
                    is UpdateMessageTimerUseCase.Result.Failure -> editSelfDeletingMessagesState.copy(
                        isLoading = false,
                        isEnabled = true
                    )

                    UpdateMessageTimerUseCase.Result.Success -> editSelfDeletingMessagesState.copy(
                        isLoading = false,
                        isEnabled = false
                    )
                }
            }
        }
    }

    fun onSelectDuration(duration: SelfDeletionDuration) {
        viewModelScope.launch {
            val previousSelected = editSelfDeletingMessagesState.currentlySelected
            editSelfDeletingMessagesState = editSelfDeletingMessagesState.copy(
                isLoading = true,
                currentlySelected = duration
            )
            editSelfDeletingMessagesState = when (updateMessageTimer(conversationId, duration.value.inWholeMilliseconds)) {
                is UpdateMessageTimerUseCase.Result.Failure -> {
                    editSelfDeletingMessagesState.copy(
                        isLoading = true,
                        currentlySelected = previousSelected
                    )
                }

                UpdateMessageTimerUseCase.Result.Success -> {
                    editSelfDeletingMessagesState.copy(
                        isLoading = false
                    )
                }
            }
        }
    }

    fun navigateBack(args: Map<String, Boolean> = mapOf()) {
        viewModelScope.launch { navigationManager.navigateBack(args) }
    }
}
