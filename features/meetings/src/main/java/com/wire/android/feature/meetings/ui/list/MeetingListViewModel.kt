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
import androidx.paging.map
import com.wire.android.feature.meetings.mapper.toMeetingItem
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

interface MeetingListViewModel {
    val currentTimeProvider: CurrentTimeProvider get() = CurrentTimeProvider.Default
    val meetings: Flow<PagingData<MeetingListItem>> get() = flowOf()
    val isShowingAll: StateFlow<Boolean> get() = MutableStateFlow(false)
    fun showAll() {}
}

class MeetingListViewModelPreview(
    override val currentTimeProvider: CurrentTimeProvider,
    type: MeetingsTabItem,
    showingAll: Boolean = type == MeetingsTabItem.PAST,
) : MeetingListViewModel {
    private val meetingMocksProvider = MeetingMocksProvider(currentTimeProvider)
    override val isShowingAll: StateFlow<Boolean> = MutableStateFlow(showingAll)
    override val meetings: Flow<PagingData<MeetingListItem>> = MutableStateFlow(
        meetingMocksProvider.getItems(showingAll, type).toPagingDataWithLoadState(isShowingAll.value).insertHeaders(type)
    )
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
    private val alignedTickerFlow = flow {
        while (currentCoroutineContext().isActive) {
            val currentTime = currentTimeProvider()
            emit(currentTime)
            // Calculate ms remaining until the next exact minute to align emissions with full minutes
            delay(60_000 - (currentTime.toEpochMilliseconds() % 60_000))
        }
    }

    private val pagingDataFlow = isShowingAll
        .flatMapLatest { isShowingAll ->
            MeetingMocksProvider.Default.getPagingDataFlow(type = type, showingAll = isShowingAll) // TODO replace with real data source
        }
        .flowOn(dispatcher.io())
        .cachedIn(viewModelScope)

    override val meetings: Flow<PagingData<MeetingListItem>> = combine(pagingDataFlow, alignedTickerFlow) { pagingData, currentTime ->
        pagingData
            .map { rawMeeting ->
                rawMeeting.toMeetingItem(time = currentTime)
            }
            .insertHeaders(type = type)
    }.flowOn(dispatcher.io())

    override fun showAll() {
        isShowingAll.value = true
    }
}

/** Generates a header between two MeetingItems if needed. The list is assumed to be sorted by start time. */
@Suppress("CyclomaticComplexMethod")
private fun generateHeader(
    before: MeetingItem?,
    after: MeetingItem?,
    ongoingEnabled: Boolean = false,
    hoursEnabled: Boolean = false,
): MeetingHeader? {
    val beforeLocalTime = before?.status?.startTime?.toLocalDateTime(TimeZone.currentSystemDefault())
    val afterLocalTime = after?.status?.startTime?.toLocalDateTime(TimeZone.currentSystemDefault())
    val beforeStatusOngoing = before?.status is MeetingItem.Status.Ongoing
    val afterStatusOngoing = after?.status is MeetingItem.Status.Ongoing
    return when {
        // If the next meeting is ongoing and the previous one is not, start new "Ongoing" section if enabled.
        ongoingEnabled && !beforeStatusOngoing && afterStatusOngoing -> MeetingHeader.Ongoing

        // If the previous meeting is ongoing in "Ongoing" section and the next one is not, start new "Day" section, add "Hour" if enabled.
        ongoingEnabled && afterLocalTime != null && beforeStatusOngoing && !afterStatusOngoing -> when {
            hoursEnabled -> MeetingHeader.DayAndHour(time = afterLocalTime.headerDayHourTime())
            else -> MeetingHeader.Day(time = afterLocalTime.headerDayHourTime())
        }

        // If the next meeting is on a different day than the previous one, start a new "Day" section, add "Hour" if enabled.
        afterLocalTime != null && beforeLocalTime?.date != afterLocalTime.date -> when {
            hoursEnabled -> MeetingHeader.DayAndHour(time = afterLocalTime.headerDayHourTime())
            else -> MeetingHeader.Day(time = afterLocalTime.headerDayHourTime())
        }

        // If the next meeting is on the same day but a different hour than the previous one, add an "Hour" header if enabled.
        hoursEnabled && afterLocalTime != null && beforeLocalTime?.hour != afterLocalTime.hour ->
            MeetingHeader.Hour(time = afterLocalTime.headerDayHourTime())

        // Otherwise, no header is added.
        else -> null
    }
}

/** Extension function to create a header time at the start of the hour of the meeting's start time. */
private fun LocalDateTime.headerDayHourTime() = date.atTime(hour, 0, 0).toInstant(TimeZone.currentSystemDefault())

/** Extension function to convert a list of MeetingItems to PagingData of MeetingItems with load states. */
private fun List<MeetingItem>.toPagingDataWithLoadState(appendLoading: Boolean): PagingData<MeetingItem> =
    PagingData.from(
        data = this,
        sourceLoadStates = LoadStates(
            refresh = LoadState.NotLoading(true),
            prepend = LoadState.NotLoading(true),
            append = if (appendLoading) LoadState.Loading else LoadState.NotLoading(true),
        )
    )

/** Extension function to insert headers into a PagingData stream of MeetingItems based on the given type. */
private fun PagingData<MeetingItem>.insertHeaders(type: MeetingsTabItem): PagingData<MeetingListItem> =
    insertSeparators { before, after ->
        when (type) {
            MeetingsTabItem.NEXT -> generateHeader(before = before, after = after)
            MeetingsTabItem.PAST -> null // No headers for past meetings
        }
    }
