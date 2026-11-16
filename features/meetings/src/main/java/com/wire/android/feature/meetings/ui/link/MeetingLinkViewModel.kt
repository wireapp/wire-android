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
package com.wire.android.feature.meetings.ui.link

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.metro.WireAssistedViewModelBinding
import com.wire.android.feature.meetings.model.MeetingLink
import com.wire.android.di.CurrentAccount
import com.wire.kalium.logic.data.id.MeetingId
import com.wire.kalium.logic.data.user.UserId
import com.wire.android.feature.meetings.ui.MeetingsManualViewModelFactoryGroup
import com.wire.kalium.logic.feature.meeting.ObserveMeetingUseCase
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch

interface MeetingLinkViewModel {
    val state: MeetingLinkState
}

@WireAssistedViewModelBinding(MeetingsManualViewModelFactoryGroup::class)
class MeetingLinkViewModelImpl @AssistedInject constructor(
    @Assisted meetingId: MeetingId,
    observeMeeting: ObserveMeetingUseCase,
    @CurrentAccount selfUserId: UserId,
) : ViewModel(), MeetingLinkViewModel {
    @AssistedFactory
    interface Factory {
        fun create(meetingId: MeetingId): MeetingLinkViewModelImpl
    }

    override var state: MeetingLinkState by mutableStateOf(MeetingLinkState.Loading)
        private set

    init {
        viewModelScope.launch {
            observeMeeting(meetingId).collect { meeting ->
                state = if (meeting == null || meeting.creatorId != selfUserId) {
                    MeetingLinkState.NotAvailable
                } else {
                    MeetingLinkState.Ready(MeetingLink(meeting.link, meeting.hasPassword))
                }
            }
        }
    }
}

sealed interface MeetingLinkState {
    data object Loading : MeetingLinkState
    data object NotAvailable : MeetingLinkState
    data class Ready(val meetingLink: MeetingLink) : MeetingLinkState
}
