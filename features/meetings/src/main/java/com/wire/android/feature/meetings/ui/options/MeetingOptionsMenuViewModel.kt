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
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.ui.mock.MeetingMocksProvider
import com.wire.android.feature.meetings.ui.mock.scheduledRepeatingGroupMeeting
import com.wire.android.feature.meetings.ui.util.CurrentTimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

interface MeetingOptionsMenuViewModel {
    fun observeMeetingStateFlow(meetingId: String): StateFlow<MeetingOptionsMenuState> = MutableStateFlow(
        MeetingOptionsMenuState.Meeting(CurrentTimeProvider.Preview.scheduledRepeatingGroupMeeting.copy(meetingId = meetingId))
    )
}

class MeetingOptionsMenuViewModelPreview(currentTimeProvider: CurrentTimeProvider) : MeetingOptionsMenuViewModel {
    private val meetingMocksProvider = MeetingMocksProvider(currentTimeProvider)
    override fun observeMeetingStateFlow(meetingId: String): StateFlow<MeetingOptionsMenuState> =
        MutableStateFlow(meetingMocksProvider.getItem(meetingId)?.let { MeetingOptionsMenuState.Meeting(it) }
            ?: MeetingOptionsMenuState.NotAvailable)
}

@HiltViewModel
class MeetingOptionsMenuViewModelImpl @Inject constructor() : MeetingOptionsMenuViewModel, ViewModel() {
    private val meetingMocksProvider = MeetingMocksProvider(CurrentTimeProvider.Default) // TODO replace with real data source
    override fun observeMeetingStateFlow(meetingId: String): StateFlow<MeetingOptionsMenuState> =
        MutableStateFlow(meetingMocksProvider.getItem(meetingId)?.let { MeetingOptionsMenuState.Meeting(it) }
            ?: MeetingOptionsMenuState.NotAvailable)
}

sealed interface MeetingOptionsMenuState {
    data object Loading : MeetingOptionsMenuState
    data object NotAvailable : MeetingOptionsMenuState
    data class Meeting(val meeting: MeetingItem) : MeetingOptionsMenuState
}
