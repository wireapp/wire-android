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
import com.wire.android.appLogger
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.messagecomposer.state.SelfDeletionDuration
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.conversation.messagetimer.UpdateMessageTimerUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
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

    private val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        checkNotNull(savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)) {
            "No conversationId was provided via savedStateHandle to EditSelfDeletingMessagesViewModel"
        }
    )

    var state by mutableStateOf(
        EditSelfDeletingMessagesState()
    )

    init {
        observeSelfDeletionTimerSettingsForConversation()
    }

    private fun observeSelfDeletionTimerSettingsForConversation() {
        viewModelScope.launch {
            combine(
                observeSelfDeletionTimerSettingsForConversation(conversationId, considerSelfUserSettings = false),
                observeConversationMembers(conversationId).map { it.isSelfAnAdmin },
                ::Pair
            )
                .distinctUntilChanged()
                .flowOn(dispatcher.io())
                .collect { (selfDeletingMessages, isSelfAnAdmin) ->
                    state = state.copy(
                        isLoading = selfDeletingMessages.isEnforcedByTeam || !isSelfAnAdmin,
                        isEnabled = selfDeletingMessages.isEnforcedByGroup,
                        remotelySelected = selfDeletingMessages.toDuration().toSelfDeletionDuration(),
                        locallySelected = selfDeletingMessages.toDuration().toSelfDeletionDuration()
                    )
                }
        }
    }

    fun updateSelfDeletingMessageOption(shouldBeEnabled: Boolean) {
        viewModelScope.launch {
            state = if (shouldBeEnabled) {
                state.copy(isEnabled = true)
            } else {
                state.copy(isEnabled = false, locallySelected = null)
            }
        }
    }

    fun onSelectDuration(duration: SelfDeletionDuration) {
        state = state.copy(locallySelected = duration)
    }

    fun applyNewDuration() {
        viewModelScope.launch {
            val currentSelectedDuration = state.locallySelected
            state = when (updateMessageTimer(conversationId, currentSelectedDuration?.value?.inWholeMilliseconds)) {
                is UpdateMessageTimerUseCase.Result.Failure -> {
                    appLogger.e("Failed to update self deleting enforced duration for conversation=${conversationId.toLogString()} " +
                            "with new duration=${currentSelectedDuration?.name}")
                    state.copy(isLoading = true)
                }

                UpdateMessageTimerUseCase.Result.Success -> {
                    appLogger.d("Success updating self deleting enforced duration for conversation=${conversationId.toLogString()} " +
                            "with new duration=${currentSelectedDuration?.name}")
                    state.copy(
                        isLoading = false,
                        remotelySelected = currentSelectedDuration
                    )
                }
            }
            navigateBack()
        }
    }

    fun navigateBack(args: Map<String, Boolean> = mapOf()) {
        viewModelScope.launch { navigationManager.navigateBack(args) }
    }
}
