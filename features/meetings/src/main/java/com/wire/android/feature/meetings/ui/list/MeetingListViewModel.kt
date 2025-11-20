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
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import com.wire.android.feature.meetings.model.MeetingHeader
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.model.MeetingListItem
import com.wire.android.feature.meetings.ui.MeetingsTabItem
import com.wire.android.feature.meetings.ui.mock.MeetingMocksProvider
import com.wire.android.feature.meetings.ui.util.CurrentTimeProvider
import com.wire.android.util.dispatchers.DispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

interface MeetingListViewModel {
    val meetings: Flow<PagingData<MeetingListItem>> get() = flowOf()
    val isShowingAll: StateFlow<Boolean> get() = MutableStateFlow(false)
    fun showAll() {}
}

class MeetingListViewModelPreview(
    currentTimeProvider: CurrentTimeProvider,
    type: MeetingsTabItem,
    showingAll: Boolean = type == MeetingsTabItem.PAST,
) : MeetingListViewModel {
    private val meetingMocksProvider = MeetingMocksProvider(currentTimeProvider)
    override val isShowingAll: StateFlow<Boolean> = MutableStateFlow(showingAll)
    override val meetings: Flow<PagingData<MeetingListItem>> =
        MutableStateFlow(PagingData.from(meetingMocksProvider.getItems(showingAll, type).insertHeaders(type)))
}

@HiltViewModel(assistedFactory = MeetingListViewModelImpl.Factory::class)
class MeetingListViewModelImpl @AssistedInject constructor(
    @Assisted val type: MeetingsTabItem,
    dispatcher: DispatcherProvider,
) : ViewModel(), MeetingListViewModel {
    @AssistedFactory
    interface Factory {
        fun create(type: MeetingsTabItem): MeetingListViewModelImpl
    }

    override val isShowingAll = MutableStateFlow(type == MeetingsTabItem.PAST) // for PAST always show all, for NEXT start with false
    private val meetingMocksProvider = MeetingMocksProvider(CurrentTimeProvider.Default) // TODO replace with real data source
    override val meetings: Flow<PagingData<MeetingListItem>> = isShowingAll
        .flatMapLatest { showingAll ->
            @Suppress("MagicNumber")
            flow {
                if (!showingAll) {
                    delay(1000) // Simulate loading delay
                    emit(
                        meetingMocksProvider.getItems(showingAll = false, type = type)
                            .toPagingDataWithLoadState(type = type, appendLoading = false)
                    )
                } else {
                    if (type == MeetingsTabItem.NEXT) { // For NEXT, first emit a loading state with partial data to simulate loading more
                        emit(
                            meetingMocksProvider.getItems(showingAll = false, type = type)
                                .toPagingDataWithLoadState(type = type, appendLoading = true)
                        )
                    }
                    delay(1000) // Simulate loading delay
                    emit(
                        meetingMocksProvider.getItems(showingAll = true, type = type)
                            .toPagingDataWithLoadState(type = type, appendLoading = false)
                    )
                }
            }
        }
        .flowOn(dispatcher.io())
        .cachedIn(viewModelScope)

    override fun showAll() {
        isShowingAll.value = true
    }
}

// Generates a header between two MeetingItems if needed. The list is assumed to be sorted by start time.
private fun generateHeader(type: MeetingsTabItem, before: MeetingItem?, after: MeetingItem?): MeetingHeader? {
    val beforeLocalTime = before?.status?.startTime?.toLocalDateTime(TimeZone.currentSystemDefault())
    val afterLocalTime = after?.status?.startTime?.toLocalDateTime(TimeZone.currentSystemDefault())
    return when {
        type == MeetingsTabItem.PAST -> null // No headers for past meetings

        // If the next meeting is ongoing and the previous one is not, add an "Ongoing" header.
        before?.status !is MeetingItem.Status.Ongoing && after?.status is MeetingItem.Status.Ongoing -> MeetingHeader.Ongoing

        // If the previous meeting is ongoing and the next one is not, add a "Day and Hour" header for the next meeting.
        afterLocalTime != null && before?.status is MeetingItem.Status.Ongoing && after.status !is MeetingItem.Status.Ongoing ->
            MeetingHeader.DayAndHour(time = afterLocalTime.headerDayHourTime())

        // If the next meeting is on a different day than the previous one, add a "Day and Hour" header.
        afterLocalTime != null && beforeLocalTime?.date != afterLocalTime.date ->
            MeetingHeader.DayAndHour(time = afterLocalTime.headerDayHourTime())

        // If the next meeting is on the same day but a different hour than the previous one, add an "Hour" header.
        afterLocalTime != null && beforeLocalTime?.hour != afterLocalTime.hour ->
            MeetingHeader.Hour(time = afterLocalTime.headerDayHourTime())

        // Otherwise, no header is added.
        else -> null
    }
}

// Create a header time at the start of the hour of the meeting's start time.
private fun LocalDateTime.headerDayHourTime() = date.atTime(hour, 0, 0).toInstant(TimeZone.currentSystemDefault())

// Extension function to insert headers into a list of MeetingItems.
private fun List<MeetingItem>.insertHeaders(type: MeetingsTabItem) = buildList<MeetingListItem> {
    for (i in 0..this@insertHeaders.size) {
        val (previous, current) = this@insertHeaders.getOrNull(i - 1) to this@insertHeaders.getOrNull(i)
        val header = generateHeader(type = type, before = previous, after = current)
        if (header != null) add(header)
        if (current != null) add(current)
    }
}

// Extension function to convert a list of MeetingItems to PagingData of MeetingListItems with headers and load states.
private fun List<MeetingItem>.toPagingDataWithLoadState(type: MeetingsTabItem, appendLoading: Boolean): PagingData<MeetingListItem> =
    PagingData.from(
        data = this,
        sourceLoadStates = LoadStates(
            refresh = LoadState.NotLoading(true),
            prepend = LoadState.NotLoading(true),
            append = if (appendLoading) LoadState.Loading else LoadState.NotLoading(true),
        )
    ).insertSeparators { before, after ->
        generateHeader(type = type, before = before, after = after)
    }
