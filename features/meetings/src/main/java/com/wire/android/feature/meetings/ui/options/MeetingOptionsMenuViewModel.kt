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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.mapper.toItemSelfRole
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.ui.mock.MeetingMocksProvider
import com.wire.android.model.SnackBarMessage
import com.wire.android.model.asSnackBarMessage
import com.wire.android.ui.common.ActionsManager
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.util.CurrentTimeProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.MeetingId
import com.wire.kalium.logic.data.meeting.MeetingOccurrence
import com.wire.kalium.logic.feature.meeting.DeleteMeetingUseCase
import com.wire.kalium.logic.feature.meeting.ObserveMeetingOccurrenceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.concurrent.ConcurrentHashMap

interface MeetingOptionsMenuViewModel : ActionsManager<MeetingOptionsMenuViewAction> {
    val deleteMeetingForEveryoneDialogState: VisibilityState<DeleteMeetingDialogState> get() = VisibilityState()
    fun observeMeetingStateFlow(occurrenceId: String): StateFlow<MeetingOptionsMenuState>
    fun deleteMeeting(meetingId: MeetingId, meetingTitle: String)
}

class MeetingOptionsMenuViewModelPreview(currentTimeProvider: CurrentTimeProvider) : MeetingOptionsMenuViewModel {
    private val meetingMocksProvider = MeetingMocksProvider(currentTimeProvider)
    override fun observeMeetingStateFlow(occurrenceId: String): StateFlow<MeetingOptionsMenuState> = MutableStateFlow(
        meetingMocksProvider.getItem(occurrenceId)?.let {
            MeetingOptionsMenuState.Meeting(meetingId = it.meetingId, title = it.title, selfRole = it.selfRole)
        } ?: MeetingOptionsMenuState.NotAvailable
    )

    override fun deleteMeeting(meetingId: MeetingId, meetingTitle: String) = Unit
}

class MeetingOptionsMenuViewModelImpl(
    private val observeMeetingOccurrenceUseCase: ObserveMeetingOccurrenceUseCase,
    private val deleteMeetingUseCase: DeleteMeetingUseCase,
) : MeetingOptionsMenuViewModel, ActionsViewModel<MeetingOptionsMenuViewAction>() {
    private val stateFlow: ConcurrentHashMap<String, StateFlow<MeetingOptionsMenuState>> = ConcurrentHashMap()
    override val deleteMeetingForEveryoneDialogState: VisibilityState<DeleteMeetingDialogState> by mutableStateOf(VisibilityState())

    override fun observeMeetingStateFlow(occurrenceId: String): StateFlow<MeetingOptionsMenuState> = stateFlow.getOrPut(occurrenceId) {
        flowOf(occurrenceId)
            .flatMapConcat { occurrenceId ->
                observeMeetingOccurrenceUseCase.invoke(occurrenceId).map {
                    when {
                        it != null -> MeetingOptionsMenuState.Meeting(
                            meetingId = it.meetingId,
                            title = it.title,
                            selfRole = it.selfRole.toItemSelfRole(),
                            deleteOption = when {
                                it.occurrenceStartTime < Clock.System.now() -> MeetingOptionsMenuState.Meeting.DeleteOption.None
                                else -> when (it.selfRole) {
                                    MeetingOccurrence.SelfRole.Creator -> MeetingOptionsMenuState.Meeting.DeleteOption.ForEveryone
                                    MeetingOccurrence.SelfRole.Member -> MeetingOptionsMenuState.Meeting.DeleteOption.ForMe
                                }
                            },
                        )

                        else -> MeetingOptionsMenuState.NotAvailable
                    }
                }
            }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 500L),
                initialValue = MeetingOptionsMenuState.Loading,
            )
    }

    override fun deleteMeeting(meetingId: MeetingId, meetingTitle: String) {
        viewModelScope.launch {
            deleteMeetingForEveryoneDialogState.update { it.copy(loading = true) }
            when (deleteMeetingUseCase.invoke(meetingId = meetingId)) {
                is DeleteMeetingUseCase.Result.Success -> {
                    UIText.StringResource(R.string.meeting_deleted_success, meetingTitle).asSnackBarMessage()
                }

                is DeleteMeetingUseCase.Result.Failure -> {
                    UIText.StringResource(R.string.meeting_deleted_failure, meetingTitle).asSnackBarMessage()
                }
            }.let {
                sendAction(MeetingOptionsMenuViewAction.Message(it))
            }
            deleteMeetingForEveryoneDialogState.dismiss()
        }
    }
}

sealed interface MeetingOptionsMenuState {
    data object Loading : MeetingOptionsMenuState
    data object NotAvailable : MeetingOptionsMenuState
    data class Meeting(
        val meetingId: MeetingId,
        val title: String,
        val selfRole: MeetingItem.SelfRole = MeetingItem.SelfRole.Member,
        val deleteOption: DeleteOption = DeleteOption.None,
        val createConversationEnabled: Boolean = false,
        val copyLinkEnabled: Boolean = false,
        val editMeetingEnabled: Boolean = false,
    ) : MeetingOptionsMenuState {
        enum class DeleteOption {
            ForMe, ForEveryone, None
        }
    }
}

sealed interface MeetingOptionsMenuViewAction {
    data class Message(val message: SnackBarMessage) : MeetingOptionsMenuViewAction
}
