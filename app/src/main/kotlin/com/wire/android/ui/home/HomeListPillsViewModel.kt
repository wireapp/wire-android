/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.ConversationFilter
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsWithEventsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Exposes the count of conversations that have "new activity" (unread events) for the main conversation list.
 * Used to render the count badge on the "Conversations" home list filter pill.
 */
class HomeListPillsViewModel(
    private val observeConversationListDetailsWithEvents: ObserveConversationListDetailsWithEventsUseCase,
    private val dispatcher: DispatcherProvider,
) : ViewModel() {

    val newActivityCount: StateFlow<Int> = flow {
        emitAll(
            observeConversationListDetailsWithEvents(
                fromArchive = false,
                conversationFilter = ConversationFilter.All,
            )
        )
    }
        .map { conversations -> conversations.count { it.hasNewActivitiesToShow } }
        .catch { emit(0) }
        .flowOn(dispatcher.io())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0,
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
