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

package com.wire.android.ui.home.conversationslist

import androidx.lifecycle.viewModelScope
import com.wire.android.ui.common.ActionsManager
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

interface ConversationListCallViewModel : ActionsManager<ConversationListCallViewActions> {
    val joinCallDialogState: VisibilityState<ConversationId> get() = VisibilityState()
    fun joinOngoingCall(conversationId: ConversationId) {}
    fun joinAnyway(conversationId: ConversationId) {}
}

object ConversationListCallViewModelPreview : ConversationListCallViewModel

@Suppress("MagicNumber", "TooManyFunctions", "LongParameterList")
@HiltViewModel
class ConversationListCallViewModelImpl @Inject constructor(
    private val answerCall: AnswerCallUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val endCall: EndCallUseCase
) : ConversationListCallViewModel, ActionsViewModel<ConversationListCallViewActions>() {

    override val joinCallDialogState: VisibilityState<ConversationId> = VisibilityState()

    private var establishedCallConversationId: QualifiedID? = null
    private var conversationId: QualifiedID? = null

    private suspend fun observeEstablishedCall() {
        observeEstablishedCalls()
            .distinctUntilChanged()
            .collectLatest {
                establishedCallConversationId = it.firstOrNull()?.conversationId
            }
    }

    init {
        viewModelScope.launch {
            observeEstablishedCall()
        }
    }

    override fun joinAnyway(conversationId: ConversationId) {
        viewModelScope.launch {
            establishedCallConversationId?.let {
                endCall(it)
                delay(DELAY_END_CALL)
            }
            joinOngoingCall(conversationId)
        }
    }

    override fun joinOngoingCall(conversationId: ConversationId) {
        this.conversationId = conversationId
        if (establishedCallConversationId != null) {
            joinCallDialogState.show(conversationId)
        } else {
            joinCallDialogState.dismiss()
            viewModelScope.launch {
                answerCall(conversationId = conversationId)
            }
            sendAction(ConversationListCallViewActions.JoinedCall(conversationId))
        }
    }

    companion object {
        const val DELAY_END_CALL = 200L
    }
}

sealed interface ConversationListCallViewActions {
    data class JoinedCall(val conversationId: ConversationId) : ConversationListCallViewActions
}
