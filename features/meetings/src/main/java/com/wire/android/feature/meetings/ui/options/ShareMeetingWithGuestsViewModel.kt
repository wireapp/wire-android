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
package com.wire.android.feature.meetings.ui.options

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.metro.WireAssistedViewModelBinding
import com.wire.android.feature.meetings.ui.MeetingsManualViewModelFactoryGroup
import com.wire.kalium.logic.data.meeting.MeetingOccurrence
import com.wire.kalium.logic.feature.meeting.ObserveMeetingOccurrenceUseCase
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch

interface ShareMeetingWithGuestsViewModel {
    val state: ShareMeetingWithGuestsState
}

@WireAssistedViewModelBinding(MeetingsManualViewModelFactoryGroup::class)
class ShareMeetingWithGuestsViewModelImpl @AssistedInject constructor(
    @Assisted occurrenceId: String,
    observeMeetingOccurrence: ObserveMeetingOccurrenceUseCase,
) : ViewModel(), ShareMeetingWithGuestsViewModel {
    @AssistedFactory
    interface Factory {
        fun create(occurrenceId: String): ShareMeetingWithGuestsViewModelImpl
    }

    override var state: ShareMeetingWithGuestsState by mutableStateOf(ShareMeetingWithGuestsState.Loading)
        private set

    init {
        viewModelScope.launch {
            observeMeetingOccurrence.invoke(occurrenceId).collect { occurrence ->
                state = if (occurrence == null || occurrence.selfRole != MeetingOccurrence.SelfRole.Creator) {
                    ShareMeetingWithGuestsState.NotAvailable
                } else {
                    ShareMeetingWithGuestsState.Ready(occurrence.meeting.link, occurrence.meeting.hasPassword)
                }
            }
        }
    }
}

sealed interface ShareMeetingWithGuestsState {
    data object Loading : ShareMeetingWithGuestsState
    data object NotAvailable : ShareMeetingWithGuestsState
    data class Ready(val link: String?, val hasPassword: Boolean) : ShareMeetingWithGuestsState
}
