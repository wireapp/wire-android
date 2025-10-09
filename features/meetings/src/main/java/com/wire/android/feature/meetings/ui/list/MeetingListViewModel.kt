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
package com.wire.android.feature.meetings.ui.list

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.model.MeetingListItem
import com.wire.android.feature.meetings.model.MeetingSeparator
import com.wire.android.feature.meetings.ui.MeetingsTabItem
import com.wire.android.feature.meetings.ui.mock.meetingMocks
import com.wire.android.feature.meetings.ui.util.CurrentTimeScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

interface MeetingListViewModel {
    val meetings: Flow<PagingData<MeetingListItem>> get() = flowOf()
    fun showAll() {}
}

class MeetingListViewModelPreview(
    currentTimeScope: CurrentTimeScope,
    type: MeetingsTabItem,
    showingAll: Boolean = type == MeetingsTabItem.PAST,
) : MeetingListViewModel {
    override val meetings: Flow<PagingData<MeetingListItem>> =
        MutableStateFlow(PagingData.from(data = currentTimeScope.meetingMocks(showingAll, type).insertHeaders(showingAll)))
}

@HiltViewModel(assistedFactory = MeetingListViewModelImpl.Factory::class)
class MeetingListViewModelImpl @AssistedInject constructor(@Assisted val type: MeetingsTabItem) : ViewModel(), MeetingListViewModel {
    @AssistedFactory
    interface Factory {
        fun create(type: MeetingsTabItem): MeetingListViewModelImpl
    }

    private val showingAll = MutableStateFlow(type == MeetingsTabItem.PAST) // for PAST always show all, for NEXT start with false
    override val meetings: Flow<PagingData<MeetingListItem>> = showingAll.mapLatest { showingAll ->
        PagingData.from(CurrentTimeScope().meetingMocks(showingAll, type)) // TODO replace with real data source
            .insertSeparators { before, after -> generateSeparator(before, after, showingAll) }
    }

    override fun showAll() { showingAll.value = true }
}

// Generates a header between two MeetingItems if needed. The list is assumed to be sorted by start time ascending.
private fun generateSeparator(before: MeetingItem?, after: MeetingItem?, showingAll: Boolean): MeetingSeparator? {
    val beforeLocalTime = before?.status?.startTime?.toLocalDateTime(TimeZone.currentSystemDefault())
    val afterLocalTime = after?.status?.startTime?.toLocalDateTime(TimeZone.currentSystemDefault())
    return when {
        // If the next meeting is ongoing and the previous one is not, add an "Ongoing" header.
        before?.status !is MeetingItem.Status.Ongoing && after?.status is MeetingItem.Status.Ongoing -> MeetingSeparator.Ongoing

        // If the previous meeting is ongoing and the next one is not, add a "Day and Hour" header for the next meeting.
        afterLocalTime != null && before?.status is MeetingItem.Status.Ongoing && after.status !is MeetingItem.Status.Ongoing ->
            MeetingSeparator.DayAndHour(time = afterLocalTime.headerDayHourTime())

        // If the next meeting is on a different day than the previous one, add a "Day and Hour" header.
        afterLocalTime != null && beforeLocalTime?.date != afterLocalTime.date ->
            MeetingSeparator.DayAndHour(time = afterLocalTime.headerDayHourTime())

        // If the next meeting is on the same day but a different hour than the previous one, add an "Hour" header.
        afterLocalTime != null && beforeLocalTime?.hour != afterLocalTime.hour ->
            MeetingSeparator.Hour(time = afterLocalTime.headerDayHourTime())

        // If there is no next meeting (so after a last meeting) and we are not showing all meetings, add a "Show All" footer.
        before != null && after == null && !showingAll -> MeetingSeparator.ShowAll

        // Otherwise, no header is added.
        else -> null
    }
}

// Create a header time at the start of the hour of the meeting's start time.
private fun LocalDateTime.headerDayHourTime() = date.atTime(hour, 0, 0).toInstant(TimeZone.currentSystemDefault())

// Extension function to insert headers into a list of MeetingItems.
private fun List<MeetingItem>.insertHeaders(showingAll: Boolean) = buildList<MeetingListItem> {
    for (i in 0..this@insertHeaders.size) {
        val (previous, current) = this@insertHeaders.getOrNull(i - 1) to this@insertHeaders.getOrNull(i)
        val header = generateSeparator(previous, current, showingAll)
        if (header != null) add(header)
        if (current != null) add(current)
    }
}
