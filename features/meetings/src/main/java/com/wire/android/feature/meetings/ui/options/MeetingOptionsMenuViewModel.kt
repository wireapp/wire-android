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
package com.wire.android.feature.meetings.ui.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.meetings.mapper.toItemSelfRole
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.ui.mock.MeetingMocksProvider
import com.wire.android.util.CurrentTimeProvider
import com.wire.kalium.logic.data.meeting.MeetingOccurrence
import com.wire.kalium.logic.feature.meeting.ObserveMeetingOccurrenceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

interface MeetingOptionsMenuViewModel {
    fun observeMeetingStateFlow(occurrenceId: String): StateFlow<MeetingOptionsMenuState>
}

class MeetingOptionsMenuViewModelPreview(currentTimeProvider: CurrentTimeProvider) : MeetingOptionsMenuViewModel {
    private val meetingMocksProvider = MeetingMocksProvider(currentTimeProvider)
    override fun observeMeetingStateFlow(occurrenceId: String): StateFlow<MeetingOptionsMenuState> = MutableStateFlow(
        meetingMocksProvider.getItem(occurrenceId)?.let {
            MeetingOptionsMenuState.Meeting(title = it.title, selfRole = it.selfRole)
        } ?: MeetingOptionsMenuState.NotAvailable
    )
}

class MeetingOptionsMenuViewModelImpl(
    private val observeMeetingOccurrenceUseCase: ObserveMeetingOccurrenceUseCase,
) : MeetingOptionsMenuViewModel, ViewModel() {
    private val currentIdFlow = MutableStateFlow<String?>(null)
    private val stateFlow: StateFlow<MeetingOptionsMenuState> = currentIdFlow
        .flatMapLatest { occurrenceId ->
            occurrenceId?.let {
                observeMeetingOccurrenceUseCase.invoke(occurrenceId)
                    .map {
                        when {
                            it != null -> MeetingOptionsMenuState.Meeting(
                                title = it.title,
                                selfRole = it.selfRole.toItemSelfRole(),
                                deleteOption = when (it.selfRole) {
                                    MeetingOccurrence.SelfRole.Creator -> MeetingOptionsMenuState.Meeting.DeleteOption.ForEveryone
                                    MeetingOccurrence.SelfRole.Member -> MeetingOptionsMenuState.Meeting.DeleteOption.ForMe
                                },
                            )

                            else -> MeetingOptionsMenuState.NotAvailable
                        }
                    }
                    .onStart { emit(MeetingOptionsMenuState.Loading) }
            } ?: flowOf(MeetingOptionsMenuState.Loading)
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 500L, replayExpirationMillis = 0L),
            initialValue = MeetingOptionsMenuState.Loading,
        )

    override fun observeMeetingStateFlow(occurrenceId: String): StateFlow<MeetingOptionsMenuState> {
        currentIdFlow.value = occurrenceId
        return stateFlow
    }
}

sealed interface MeetingOptionsMenuState {
    data object Loading : MeetingOptionsMenuState
    data object NotAvailable : MeetingOptionsMenuState
    data class Meeting(
        val title: String,
        val selfRole: MeetingItem.SelfRole = MeetingItem.SelfRole.Member,
        val deleteOption: DeleteOption = DeleteOption.ForMe,
        val createConversationEnabled: Boolean = false,
        val copyLinkEnabled: Boolean = false,
        val editMeetingEnabled: Boolean = false,
    ) : MeetingOptionsMenuState {
        enum class DeleteOption {
            ForMe, ForEveryone
        }
    }
}
