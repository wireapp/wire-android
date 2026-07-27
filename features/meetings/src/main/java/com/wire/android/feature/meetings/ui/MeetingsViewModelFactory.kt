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
package com.wire.android.feature.meetings.ui

import androidx.lifecycle.SavedStateHandle
import com.wire.android.feature.meetings.ui.create.NewMeetingViewModelImpl
import com.wire.android.feature.meetings.ui.list.MeetingListViewModelImpl
import com.wire.android.feature.meetings.ui.options.MeetingOptionsMenuViewModelImpl
import com.wire.android.feature.meetings.ui.usecase.GetPaginatedFlowOfMeetingsUseCase
import com.wire.android.util.CurrentTimeProvider
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.call.usecase.ObserveActiveCallsUseCase
import com.wire.kalium.logic.feature.meeting.DeleteMeetingUseCase
import com.wire.kalium.logic.feature.meeting.ObserveMeetingOccurrenceUseCase
import dev.zacsweers.metro.Inject

class MeetingsViewModelFactory @Inject constructor(
    private val currentTimeProvider: CurrentTimeProvider,
    private val dispatcher: DispatcherProvider,
    private val getMeetingsPaginated: GetPaginatedFlowOfMeetingsUseCase,
    private val observeMeetingOccurrenceUseCase: ObserveMeetingOccurrenceUseCase,
    private val observeActiveCalls: ObserveActiveCallsUseCase,
    private val deleteMeetingUseCase: DeleteMeetingUseCase,
) {
    internal fun meetingListViewModel(type: MeetingsTabItem) = MeetingListViewModelImpl(
        type = type,
        currentTimeProvider = currentTimeProvider,
        getMeetingsPaginated = getMeetingsPaginated,
        observeActiveCalls = observeActiveCalls,
        dispatcher = dispatcher,
    )

    internal fun meetingOptionsMenuViewModel() = MeetingOptionsMenuViewModelImpl(
        observeMeetingOccurrenceUseCase = observeMeetingOccurrenceUseCase,
        deleteMeetingUseCase = deleteMeetingUseCase,
    )

    internal fun newMeetingViewModel(savedStateHandle: SavedStateHandle) = NewMeetingViewModelImpl(
        savedStateHandle = savedStateHandle,
        currentTimeProvider = currentTimeProvider
    )
}
