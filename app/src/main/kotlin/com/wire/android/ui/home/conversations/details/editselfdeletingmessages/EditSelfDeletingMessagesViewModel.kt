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

package com.wire.android.ui.home.conversations.details.editselfdeletingmessages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.android.ui.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.isTeamAdmin
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.messagetimer.UpdateMessageTimerUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
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
    private val dispatcher: DispatcherProvider,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val observeSelfDeletionTimerSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    private val updateMessageTimer: UpdateMessageTimerUseCase,
    private val selfUser: ObserveSelfUserUseCase,
    private val conversationDetails: ObserveConversationDetailsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editSelfDeletingMessagesNavArgs: EditSelfDeletingMessagesNavArgs = savedStateHandle.navArgs()
    private val conversationId: QualifiedID = editSelfDeletingMessagesNavArgs.conversationId

    var state by mutableStateOf(
        EditSelfDeletingMessagesState()
    )

    init {
        observeSelfDeletionTimerSettingsForConversation()
    }

    private fun observeSelfDeletionTimerSettingsForConversation() {
        viewModelScope.launch {
            // TODO(refactor): Move all this logic to a UseCase
            val canPerformAdminActionsFlow = combine(
                observeConversationMembers(conversationId).map { it.isSelfAnAdmin },
                selfUser(),
                conversationDetails(conversationId),
                ::Triple
            ).map { (isSelfAnAdmin, selfUser, conversationDetailsResult) ->
                if (conversationDetailsResult !is ObserveConversationDetailsUseCase.Result.Success) {
                    return@map false
                }
                val conversationDetails = conversationDetailsResult.conversationDetails
                val isChannel = conversationDetails is ConversationDetails.Group.Channel
                val isTeamAdmin = selfUser.userType.isTeamAdmin()
                val isSelfUserInConversationTeam = selfUser.teamId == conversationDetails.conversation.teamId
                isSelfAnAdmin || (isChannel && isTeamAdmin && isSelfUserInConversationTeam)
            }
            combine(
                observeSelfDeletionTimerSettingsForConversation(conversationId, considerSelfUserSettings = false),
                canPerformAdminActionsFlow,
                ::Pair
            )
                .distinctUntilChanged()
                .flowOn(dispatcher.io())
                .collect { (selfDeletingMessages, isSelfAnAdmin) ->
                    state = state.copy(
                        isLoading = selfDeletingMessages.isEnforcedByTeam || !isSelfAnAdmin,
                        isEnabled = selfDeletingMessages.isEnforcedByGroup,
                        remotelySelected = selfDeletingMessages.duration?.toSelfDeletionDuration(),
                        locallySelected = selfDeletingMessages.duration?.toSelfDeletionDuration()
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
                    appLogger.e(
                        "Failed to update self deleting enforced duration for conversation=${conversationId.toLogString()} " +
                                "with new duration=${currentSelectedDuration?.name}"
                    )
                    state.copy(isLoading = true)
                }

                UpdateMessageTimerUseCase.Result.Success -> {
                    appLogger.d(
                        "Success updating self deleting enforced duration for conversation=${conversationId.toLogString()} " +
                                "with new duration=${currentSelectedDuration?.name}"
                    )
                    state.copy(
                        isLoading = false,
                        remotelySelected = currentSelectedDuration
                    )
                }
            }
            state = state.copy(isCompleted = true)
        }
    }
}
